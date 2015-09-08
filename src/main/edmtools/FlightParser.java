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
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import edmtools.Proto.DataRecord;
import edmtools.Proto.Flight;
import edmtools.Proto.FlightMetadata;

/**
 * Parses a binary flight header and zero or more binary flight data records from the specified
 * {@link JpiInputStream} into a {@link edmtools.Proto.Flight} proto.
 */
class FlightParser {
  private static final Logger logger = Logger.getLogger(FlightParser.class.getName());

  private final JpiInputStream inputStream;
  private final int flightNumber;

  /**
   * This estimate, from the file metadata, includes both the header and the data payload.
   * The actual size might be one byte less.  See {@link skipDataRecords}.
   */
  private final int estimatedFlightLengthBytes;
  private final MetadataUtil metadataUtil;

  public FlightParser(JpiInputStream stream, FlightMetadata metadata, MetadataUtil metadataUtil) {
    this.inputStream = stream;
    this.flightNumber = metadata.getFlightNumber();
    this.estimatedFlightLengthBytes = metadata.getFlightDataLengthWords() * 2;
    this.metadataUtil = metadataUtil;
  }

  /**
   * Parses a flight header and all data records associated with the flight.
   * The stream should be at the beginning of the flight.
   */
  public Flight parse() throws IOException {
    Flight.Builder builder = Flight.newBuilder();
    parseFlightHeader(builder);
    parseFlightData(builder);
    return builder.build();
  }

  /**
   * Parses a flight header and skips all data records associated with the flight.
   * The stream should be at the beginning of the flight.
   */
  public Flight parseHeaderAndSkipData() throws IOException {
    Flight.Builder builder = Flight.newBuilder();
    parseFlightHeader(builder);
    builder.setDataLength(skipDataRecords(builder.getHeaderLength(), builder.getFlightNumber()));
    return builder.build();
  }

  /**
   * We don't know the exact length of the data.
   * estimatedFlightSizeWords = (header length + data length) / 2, which is a good hint, but
   * the next record can start at either flightSizeWords * 2 or at (flightSizeWords * 2) - 1.
   * Looks for the magic next flight number (which starts the next record) at both of these offsets.
   * Returns the number of bytes skipped.
   */
  private int skipDataRecords(int headerLength, int flightNumber) throws IOException {
    if (metadataUtil.isLastFlight(flightNumber)) {
      return inputStream.skipToEndOfFile();
    }
    int numSkip = estimatedFlightLengthBytes - headerLength - 1;
    logger.finest("Skipping " + numSkip + " bytes " +
        "(" + estimatedFlightLengthBytes + " - " + headerLength + " - 1)");
    inputStream.skip(numSkip);
    // Each flight header begins with the flight number.
    byte peek[] = inputStream.peek(3);
    logger.finest(String.format("Peeked at %02X %02X %02X\n", peek[0], peek[1], peek[2]));
    int nextFlightNumber = metadataUtil.getNextFlightNumber(flightNumber);
    if (((peek[0] << 8) + peek[1]) != nextFlightNumber) {
      if (((peek[1] << 8) + peek[2]) != nextFlightNumber) {
        throw new IOException("Could not find next flight header");
      }
      numSkip++;
      inputStream.skip(1);
    }
    return numSkip;
  }

  private void parseFlightHeader(Flight.Builder builder) throws IOException {
    inputStream.resetCounter();
    inputStream.clearCurrentRecord();

    builder.setFlightNumber(inputStream.readWord());
    if (builder.getFlightNumber() != flightNumber) {
      builder.addParseWarning(String.format(
          "Unexpected flight number %d (0x%04X) instead of expected %d (0x%04X)",
          builder.getFlightNumber(), builder.getFlightNumber(), flightNumber, flightNumber));
    }
    logger.finest(String.format("Parsing flight %d header", builder.getFlightNumber()));
    int low = inputStream.readWord();
    int high = inputStream.readWord();
    builder.setSensors(new SensorParser(low, high).parse());

    if (metadataUtil.hasExtraFlightHeaderConfiguration()) {
      @SuppressWarnings("unused")
      int unusedConfigLow = inputStream.readWord();
      @SuppressWarnings("unused")
      int unusedConfigHigh = inputStream.readWord();
      if (metadataUtil.isBuildNumberAtLeast(880)) {
        @SuppressWarnings("unused")
        int unusedConfig = inputStream.readWord();
      }
    }

    @SuppressWarnings("unused")
    int unknown = inputStream.readWord();

    int recordingInterval = inputStream.readWord();

    builder.setRecordingIntervalSecs(recordingInterval);
    int packedDate = inputStream.readWord();
    int packedTime = inputStream.readWord();
    builder.setStartTimestamp(parseUnixTimestamp(packedDate, packedTime));

    Optional<String> checksumFailureMessage = inputStream.getChecksumFailureMessage();
    if (checksumFailureMessage.isPresent()) {
      builder.addParseWarning(checksumFailureMessage.get());
    }

    builder.setHeaderLength(inputStream.getCurrentRecordSize());

    logger.finest(String.format("Parsed %d header bytes [%s]",
        inputStream.getCurrentRecordSize(), inputStream.getCurrentRecord()));
    logger.finer(String.format("Flight header:\n%s", builder.build()));
  }

  private void parseFlightData(Flight.Builder builder) throws IOException {
    DataRecordParser parser = new DataRecordParser(metadataUtil, inputStream);
    DataRecord previousDataRecord = null;
    while (inputStream.getCounter() + getMinimumRecordSize() < estimatedFlightLengthBytes) {
      DataRecord dataRecord = parser.parse(previousDataRecord);

      // TODO: verify this logic.  We believe the count means "add the previous record N times".
      int repeatCount = parser.getPreviousRecordRepeatCount();
      while (repeatCount-- > 0) {
        builder.addData(Preconditions.checkNotNull(previousDataRecord));
      }
      builder.addData(dataRecord);
      previousDataRecord = dataRecord;
    }
    builder.setDataLength(inputStream.getCounter());
  }

  private int getMinimumRecordSize() {
    return metadataUtil.isDecodeMaskSingleByte() ? 3 : 5;  // Two decode masks + repeat count byte.
  }

  private long parseUnixTimestamp(int packedDate, int packedTime) {
    int year = (packedDate & 0xfe00) >> 9;
    year += (year >= 75) ? 1900 : 2000;
    DateTime dateTime = new DateTime(
        year,
        (packedDate & 0x01e0) >> 5,
        packedDate & 0x001f,
        (packedTime & 0xf800) >> 11,
        (packedTime & 0x07e0) >> 5,
        (packedTime & 0x001f) * 2);
    return dateTime.getMillis() / 1000;
  }
}
