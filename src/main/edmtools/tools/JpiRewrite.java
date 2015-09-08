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

package edmtools.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.args4j.Option;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import edmtools.JpiDecoder;
import edmtools.JpiDecoder.JpiDecoderConfiguration;
import edmtools.JpiInputStream;
import edmtools.Proto.Flight;
import edmtools.Proto.JpiFile;

/**
 * Demo tool which extracts all or part of one JPI file into another JPI file.
 *
 * <p>Useful for extracting a flight or two for functional tests.
 */
public class JpiRewrite extends CommandLineTool {
  private static final Logger logger = Logger.getLogger(JpiRewrite.class.getName());

  @Option(name = "-reg", usage="replace registration string", aliases={"--reg", "-reg"})
  private String registration;

  @Option(name = "-flights", usage="comma-delimited flight numbers",
      aliases={"--flights", "-flights"})
  private List<Integer> flightNumbers = new ArrayList<>();

  public static void main(String args[]) throws Exception {
    CommandLineTool.initAndRun(args, new JpiRewrite());
  }

  private class Offset {
    private Offset(int start, int length) {
      this.start = start;
      this.length = length;
    }

    final int start;
    final int length;
  }

  @Override
  public void run() throws Exception {
    Preconditions.checkArgument(args.size() == 2, "Specify input and output .JPI filenames");
    byte[] b = modifyJpiFile(Files.toByteArray(new File(args.get(0))));
    Files.write(b, new File(args.get(1)));
    logger.info("Read " + args.get(0) + " and wrote " + args.get(1));
  }

  private byte[] modifyJpiFile(byte b[]) throws IOException {
    JpiInputStream inputStream = new JpiInputStream(new ByteArrayInputStream(b));
    JpiFile jpiFile = JpiDecoder.decode(
        inputStream,
        JpiDecoderConfiguration.newBuilder().withFlightHeadersOnly().build());

    // Retain selected $D (flight metadata) headers.
    byte newHeader[] = modifyFileHeader(
        new String(
            Arrays.copyOfRange(b, 0, jpiFile.getMetadata().getLength()),
            Charsets.US_ASCII),
        flightNumbers)
            .getBytes(Charsets.US_ASCII);

    // Select data records to include.
    List<Offset> offsets = new ArrayList<>();
    int currentOffset = jpiFile.getMetadata().getLength();
    int newDataLength = 0;
    for (Flight flight : jpiFile.getFlightList()) {
      int recordLength = flight.getHeaderLength() + flight.getDataLength();
      if (flightNumbers.contains(flight.getFlightNumber())) {
        offsets.add(new Offset(currentOffset, recordLength));
        newDataLength += recordLength;
        logger.fine("Adding flight " + flight.getFlightNumber() + " from " + currentOffset +
            " to " + (currentOffset + recordLength));
      }
      currentOffset += recordLength;
    }

    byte out[] = new byte[newHeader.length + newDataLength];
    System.arraycopy(newHeader, 0, out, 0, newHeader.length);
    int outOffset = newHeader.length;
    for (Offset offset : offsets) {
      System.arraycopy(b, offset.start, out, outOffset, offset.length);
      outOffset += offset.length;
    }
    return out;
  }

  private static final String CR_LF = "\r\n";
  private static final Splitter NEWLINE = Splitter.on(CR_LF).omitEmptyStrings();
  private static final Splitter COMMA = Splitter.on(",").trimResults();

  private String modifyFileHeader(String headers, List<Integer> flightNumbers) {
    List<String> lines = NEWLINE.splitToList(headers);
    return Joiner.on(CR_LF).join(modifyFileHeader(lines, flightNumbers)) + CR_LF;
  }

  private List<String> modifyFileHeader(List<String> lines, List<Integer> flightNumbers) {
    List<String> output = new ArrayList<>();
    for (String line : lines) {
      if (registration != null && line.startsWith("$U,")) {
        line = String.format("U,%s", registration);
        int checksum = 0;
        for (byte b : line.getBytes()) {
          checksum ^= b;
        }
        line = String.format("$%s*%X", line, checksum);  // $ and * are not part of checksum.
      } else if (line.startsWith("$D,")) {
        List<String> parts = COMMA.splitToList(line);
        if (!flightNumbers.contains(Integer.parseInt(parts.get(1)))) {
          continue;
        }
      }
      output.add(line);
    }
    return output;
  }
}
