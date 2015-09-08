/**
 *    Copyright 2015 Keith Wannamaker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edmtools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;

import edmtools.Proto.DataRecord;
import edmtools.Proto.EngineDataRecord;

class DataRecordParser {
  private static final Logger logger = Logger.getLogger(DataRecordParser.class.getName());

  /** A recorded value of 0 means the value is "not available". */
  private static final int NOT_AVAILABLE_VALUE_MARKER = 0;

  /**
   * The decode byte is a 8- or 16- bit number which maps to changes in a bitmask of 8 or 16 bytes
   * (the valueFlags and signFlags).  The bits in these 8 or 16 byte flags determine which metrics
   * are updated, and the sign of the update.
   * We allocate 16 bytes for the value mask to cover all cases.
   */
  private static final int MAX_NUM_VALUE_BYTES = 16;
  private BitSet valueFlags = new BitSet(MAX_NUM_VALUE_BYTES);
  private BitSet signFlags = new BitSet(MAX_NUM_VALUE_BYTES);

  private final MetadataUtil metadataUtil;
  private final JpiInputStream inputStream;
  private final Map<Integer, Metric> handlers;

  private int previousRecordRepeatCount;
  /**
   * The current list of last known actual values for elements which are currently marked "N/A".
   * We represent "N/A" values by clearing the proto field (or setting repeated fields to 0).
   * Preserve the last good value here for use with subsequent deltas.
   */
  private Map<Metric, Object> naValues = new HashMap<>();

  public DataRecordParser(MetadataUtil metadataUtil, JpiInputStream inputStream) {
    this.metadataUtil = metadataUtil;
    this.inputStream = inputStream;
    this.handlers = Metrics.getBitToMetricMap(metadataUtil);
  }

  public DataRecord parse(DataRecord previousDataRecord) throws IOException {
    valueFlags.clear();
    signFlags.clear();
    previousRecordRepeatCount = 0;
    inputStream.clearCurrentRecord();

    DataRecord.Builder builder = previousDataRecord == null
        ? DataRecord.newBuilder() : previousDataRecord.toBuilder();
    builder.clearParseWarning();

    BuilderUtil util = new BuilderUtil(builder);
    for (int bitIndex : getBitIndexesFromMasks(builder)) {
      updateProtoValue(util, builder, bitIndex, inputStream.read());
    }
    calculateExhaustGasTemperatureMaxDiffs(builder);

    Optional<String> checksumFailureMessage = inputStream.getChecksumFailureMessage();
    if (checksumFailureMessage.isPresent()) {
      builder.addParseWarning(checksumFailureMessage.get());
    }

    logger.finest(String.format("Parsed %d record bytes [%s]",
        inputStream.getCurrentRecordSize(), inputStream.getCurrentRecord()));
    logger.finer(String.format("DataRecord:\n%s", builder.build()));
    return builder.build();
  }

  private void updateProtoValue(BuilderUtil util, DataRecord.Builder builder, int bitIndex, int value) {
    Metric metric = handlers.get(bitIndex);
    if (metric.isUnsupported()) {
      builder.addParseWarning("Unexpected value for " + metric);
      return;
    }
    if (value == NOT_AVAILABLE_VALUE_MARKER) {
      // Transition from a valid value to a "N/A" value.
      if (!naValues.containsKey(metric)) {
        naValues.put(metric, getExistingValueOrDefault(util, metric));
        util.clearField(metric.getProtoPath());
      }
      return;
    } else if (naValues.containsKey(metric)) {
      // Transition from a "N/A" value back to a valid value.
      util.setFieldValue(metric.getProtoPath(), naValues.get(metric));
      naValues.remove(metric);
    }

    // For high bytes, use the low byte sign bit.
    if (signFlags.testBit(metric.getLowByteBit())) {
      value = -value;
    }
    if (metric.isHighByteBit(bitIndex)) {
      value <<= 8;
    }
    float newValue = metric.scale(metadataUtil, value);

    Number existingValue = getExistingValueOrDefault(util, metric);
    logger.finer(String.format("Updating %s = %s + %s",
        metric.getProtoPath(), existingValue.toString(), ((Float) newValue).toString()));

    util.setFieldValue(metric.getProtoPath(), existingValue.floatValue() + newValue);
  }

  private Number getExistingValueOrDefault(BuilderUtil util, Metric metric) {
    if (util.hasField(metric.getProtoPath())) {
      Object value = util.getFieldValue(metric.getProtoPath());
      if (value instanceof EnumValueDescriptor) {
        return ((EnumValueDescriptor) value).getIndex();
      } else {
        return (Number) value;
      }
    } else {
      return metric.getDefaultValue(metadataUtil);
    }
  }

  public int getPreviousRecordRepeatCount() {
    return previousRecordRepeatCount;
  }

  private List<Integer> getBitIndexesFromMasks(DataRecord.Builder builder) throws IOException {
    int decodeMask;
    int secondDecodeMask;
    if (metadataUtil.isDecodeMaskSingleByte()) {
      decodeMask = inputStream.read();
      secondDecodeMask = inputStream.read();
    } else {
      decodeMask = inputStream.readWord();
      secondDecodeMask = inputStream.readWord();
    }
    if (decodeMask != secondDecodeMask) {
      throw new IOException(String.format("Expected the decode byte %02X to appear twice: %s",
            decodeMask, inputStream.getCurrentRecord()));
    }
    logger.finest(String.format("Decode mask is %04X", decodeMask));

    previousRecordRepeatCount = inputStream.read();

    int numDecodeBits = metadataUtil.isDecodeMaskSingleByte() ? 8 : 16;
    Preconditions.checkState(numDecodeBits <= valueFlags.numBytes());
    for (int i = 0; i < numDecodeBits; ++i) {
      if ((decodeMask & (1 << i)) > 0) {
        int nextByte = inputStream.read();
        if (nextByte == 0) {
          builder.addParseWarning("value byte is 00.  Don't know how many bytes to read.");
        }
        valueFlags.setByte(i, nextByte);
        logger.finest(String.format("Value byte %d is %02X", i, nextByte));
      }
    }
    Preconditions.checkState(numDecodeBits <= signFlags.numBytes());
    for (int i = 0; i < numDecodeBits; ++i) {
      // Bytes 6 and 7 do not have a sign byte.
      if (i != 6 && i != 7 && (decodeMask & (1 << i)) > 0) {
        int nextByte = inputStream.read();
        signFlags.setByte(i, nextByte);
        logger.finest(String.format("Sign byte %d is %02X", i, nextByte));
      }
    }

    List<Integer> bitIndex = new ArrayList<>();
    for (int i = 0; i < valueFlags.numBits(); ++i) {
      if (valueFlags.testBit(i)) {
        bitIndex.add(i);
      }
    }
    return bitIndex;
  }

  private void calculateExhaustGasTemperatureMaxDiffs(DataRecord.Builder builder) {
    for (EngineDataRecord.Builder engine : builder.getEngineBuilderList()) {
      if (engine.getExhaustGasTemperatureCount() == 0) {
        continue;
      }
      int maximumExhaustGasTemperature = 0;
      for (int exhaustGasTemperature : engine.getExhaustGasTemperatureList()) {
        if (exhaustGasTemperature > maximumExhaustGasTemperature) {
          maximumExhaustGasTemperature = exhaustGasTemperature;
        }
      }
      int minimumExhaustGasTemperature = maximumExhaustGasTemperature;
      for (int exhaustGasTemperature : engine.getExhaustGasTemperatureList()) {
        if (exhaustGasTemperature < minimumExhaustGasTemperature) {
          minimumExhaustGasTemperature = exhaustGasTemperature;
        }
      }
      engine.setMaxExhaustGasTemperatureDifference(
          maximumExhaustGasTemperature - minimumExhaustGasTemperature);
    }
  }
}
