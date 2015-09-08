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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

import edmtools.Proto.AlarmThresholds;
import edmtools.Proto.Features;
import edmtools.Proto.Features.TemperatureUnit;
import edmtools.Proto.Fuel;
import edmtools.Proto.Fuel.FuelFlowUnits;
import edmtools.Proto.Metadata;

class MetadataParser {
  private static final Logger logger = Logger.getLogger(FlightParser.class.getName());
  private static final int MAX_NUM_HEADERS = 128;

  private final HeaderInputStream headerInputStream;

  public MetadataParser(JpiInputStream inputStream) {
    this.headerInputStream = new HeaderInputStream(inputStream);
  }

  private class HeaderInputStream {
    private static final char HEADER_PREFIX = '$';
    private static final char HEADER_POSTFIX = '*';
    private static final char HEADER_ITEM_DELIMITER = ',';
    private static final int MAX_HEADER_LINE_LENGTH = 128;
    private static final String CR_LF = "\r\n";

    private final Splitter checksumSplitter = Splitter.on(HEADER_POSTFIX);
    private final Splitter itemSplitter = Splitter.on(HEADER_ITEM_DELIMITER).trimResults();


    private final JpiInputStream inputStream;
    private Optional<String> currentFailureMessage;

    public HeaderInputStream(JpiInputStream inputStream) {
      this.inputStream = inputStream;
      inputStream.resetCounter();
    }

    public List<String> nextHeader() throws IOException {
      currentFailureMessage = Optional.absent();
      String line = readLine();
      List<String> parts = checksumSplitter.splitToList(line);
      if (parts.size() != 2) {
        throw new IOException(
            "Expected a checksum denoted with " + HEADER_POSTFIX + " in line " + line);
      }
      String data = parts.get(0);
      String actualChecksumString = parts.get(1);
      if (data.charAt(0) != HEADER_PREFIX) {
        throw new IOException("Expected line " + data + " to begin with " + HEADER_PREFIX);
      }
      data = data.substring(1);
      int computedChecksum = 0;
      for (byte b : data.getBytes()) {
        computedChecksum ^= b;
      }
      try {
        int actualChecksum = Integer.parseInt(actualChecksumString, 16);
        if (computedChecksum != actualChecksum) {
          currentFailureMessage = Optional.of(
              String.format("Checksum mismatch actual %2X vs expected %2X:\n%s",
                  actualChecksum, computedChecksum, data));
        }
        return itemSplitter.splitToList(data);
      } catch (NumberFormatException nfe) {
        throw new IOException("Checksum byte malformed: " + nfe + " in line " + line);
      }
    }

    public Optional<String> getChecksumFailureMessage() throws IOException {
      return currentFailureMessage;
    }

    // InputStreamReader, without reading ahead.
    private String readLine() throws IOException {
      int i = 0;
      String result = "";
      while (i++ < MAX_HEADER_LINE_LENGTH) {
        result += Character.toString((char) inputStream.read());
        if (result.endsWith(CR_LF)) {
          return result.substring(0, result.length() - CR_LF.length());
        }
      }
      throw new IOException("Header input too large");
    }

    public int getCounter() {
      return inputStream.getCounter();
    }
  }

  /**
   * Parses JPI headers into a {@code edmtools.Proto.Metadata} proto.
   *
   * <p>If an unexpected error disrupts the stream, throw an {@link IOException}.  Otherwise,
   * add the warning to the {@code edmtools.Proto.Metadata} proto.
   */
  public Metadata parse() throws IOException {
    Metadata.Builder data = Metadata.newBuilder();
    int numHeaders = 0;
    while (numHeaders++ < MAX_NUM_HEADERS) {
      List<String> header = headerInputStream.nextHeader();
      if (headerInputStream.getChecksumFailureMessage().isPresent()) {
        data.addParseWarning(headerInputStream.getChecksumFailureMessage().get());
      }
      if (!parseHeaderToMetadata(header.iterator(), data)) {
        break;
      } else if (numHeaders == MAX_NUM_HEADERS) {
        throw new IOException("Too many headers.");
      }
    }
    data.setLength(headerInputStream.getCounter());
    logger.finer(String.format("Parsed %d headers to Metadata:\n%s", numHeaders, data.build()));
    return data.build();
  }

  /**
   * Parses parts of a header line into appropriate {@link jpi.Jpi.Metadata} submessages.
   * Returns true to continue parsing headers.
   */
  private boolean parseHeaderToMetadata(Iterator<String> parts, Metadata.Builder data) {
    String prefix = parts.next();
    switch (prefix) {
      case "A":
        parseAlarmThresholds(parts, data.getAlarmThresholdsBuilder());
        break;
      case "C":
        parseFeatures(parts, data.getFeaturesBuilder());
        break;
      case "D":
        data.addFlightMetadataBuilder()
            .setFlightNumber(Integer.parseInt(parts.next()))
            .setFlightDataLengthWords(Integer.parseInt(parts.next()));
        break;
      case "E":
        break;
      case "F":
        parseFuelConfiguration(parts, data.getFuelBuilder());
        break;
      case "H":
        // TODO: fvl
        break;
      case "I":
        // TODO: crb
        break;
      case "L":
        return false;
      case "P":
        data.setProtocolVersion(Integer.parseInt(parts.next()));
        break;
      case "T":
        data.setDownloadTimestamp(parseUnixTimestamp(parts));
        break;
      case "U":
        data.setRegistration(parts.next().replace('_', ' ').trim());
        break;
      case "W":
        break;
    }
    return true;
  }

  private void parseAlarmThresholds(Iterator<String> parts, AlarmThresholds.Builder builder) {
    builder.setMaxVolts(Float.parseFloat(parts.next()) / 10);
    builder.setMinVolts(Float.parseFloat(parts.next()) / 10);
    builder.setMaxExhaustGasTemperatureDifference(Integer.parseInt(parts.next()));
    builder.setMaxCylinderHeadTemperature(Integer.parseInt(parts.next()));
    builder.setMaxCylinderHeadTemperatureCoolingRate(Integer.parseInt(parts.next()));
    builder.setMaxExhaustGasTemperature(Integer.parseInt(parts.next()));
    builder.setMaxOilTemperature(Integer.parseInt(parts.next()));
    builder.setMinOilTemperature(Integer.parseInt(parts.next()));
  }

  private void parseFuelConfiguration(Iterator<String> parts, Fuel.Builder builder) {
    builder.setFuelFlowUnits(FuelFlowUnits.valueOf(Integer.parseInt(parts.next()) + 1));
    builder.setFullQuantity(Integer.parseInt(parts.next()));
    builder.setWarningQuantity(Integer.parseInt(parts.next()));
    builder.setKFactor1(Integer.parseInt(parts.next()));
    builder.setKFactor2(Integer.parseInt(parts.next()));
  }

  private long parseUnixTimestamp(Iterator<String> parts) {
    int month = Integer.parseInt(parts.next());
    int day = Integer.parseInt(parts.next());
    int year = Integer.parseInt(parts.next()) + 2000;
    int hour = Integer.parseInt(parts.next());
    int minute = Integer.parseInt(parts.next());

    @SuppressWarnings("unused")
    int unknown = Integer.parseInt(parts.next());

    DateTime parsed = new DateTime(year, month, day, hour, minute, 0);
    return parsed.getMillis() / 1000;
  }

  private void parseFeatures(Iterator<String> parts, Features.Builder features) {
    features.setModelNumber(Integer.parseInt(parts.next()));

    int low = Integer.parseInt(parts.next());
    int high = Integer.parseInt(parts.next());
    features.setSensors(new SensorParser(low, high).parse());

    BitSet units = new BitSet(2);
    units.setWord(0, high);
    features.setEngineTemperatureUnit(
        units.testBit(12) ? TemperatureUnit.FAHRENHEIT : TemperatureUnit.CELSIUS);

    @SuppressWarnings("unused")
    int unknown = Integer.parseInt(parts.next());

    // The version numbers always come last.  Different models have additional data before
    // the version numbers.  Read the tail in reverse to pick off just the version numbers.
    List<String> reverseTail = new ArrayList<>();
    while (parts.hasNext()) {
      reverseTail.add(0, parts.next());
    }
    parts = reverseTail.iterator();
    if (reverseTail.size() > 3) {
      features.setBetaNumber(Integer.parseInt(parts.next()));
      features.setBuildNumber(Integer.parseInt(parts.next()));
    }
    features.setFirmwareVersion(Integer.parseInt(parts.next()));
  }
}
