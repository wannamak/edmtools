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
import edmtools.MetadataUtil;
import edmtools.Proto.Flight;
import edmtools.Proto.JpiFile;

/**
 * Demo tool which extracts all or part of one JPI file into another JPI file.
 * 
 * <p>Useful for extracting a flight or two for functional tests.
 */
public class JpiRewrite extends CommandLineTool {
  private static final Logger logger = Logger.getLogger(JpiRewrite.class.getName());

  @Option(name = "-start", usage="beginning flight number, -1 for first",
      aliases={"--start", "-start"})
  private int startFlight = -1;

  @Option(name = "-end", usage="ending flight number, -1 for last",
      aliases={"--end", "-end"})
  private int endFlight = -1;

  @Option(name = "-reg", usage="replace registration string", aliases={"--reg", "-reg"})
  private String registration;

  public static void main(String args[]) throws Exception {
    CommandLineTool.initAndRun(args, new JpiRewrite());
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
    
    // Determine which $D (flight metadata) headers to include.
    MetadataUtil metadataUtil = new MetadataUtil(jpiFile.getMetadata());
    int startHeader = (startFlight == -1)
        ? 0 
        : metadataUtil.findFlightMetadataIndexByFlightNumber(startFlight);
    int endHeader = (endFlight == -1) 
        ? jpiFile.getMetadata().getFlightMetadataCount()
        : metadataUtil.findFlightMetadataIndexByFlightNumber(endFlight);
    logger.fine(String.format("Retaining metadata flight headers %d through %d", startHeader, endHeader));

    // Determine data record offset to include.
    int startDataOffset = jpiFile.getMetadata().getLength();
    int endDataOffset = b.length;
    if (startFlight != -1 || endFlight != -1) {
      int offset = jpiFile.getMetadata().getLength();
      for (Flight flight : jpiFile.getFlightList()) {
        int flightNumber = flight.getFlightNumber();
        logger.fine(String.format("Flight %d's data is %d", flightNumber, flight.getHeaderLength() + flight.getDataLength()));
        if (startFlight != -1 && startFlight == flightNumber) { startDataOffset = offset; }
        offset += flight.getHeaderLength() + flight.getDataLength();
        if (endFlight != -1 && endFlight == flightNumber) { endDataOffset = offset; }
      }
    }
    
    logger.fine(String.format("Retaining data offset %d through %d", startDataOffset, endDataOffset));
    
    byte newHeader[] = modifyFileHeader(
        new String(
            Arrays.copyOfRange(b, 0, jpiFile.getMetadata().getLength()),
            Charsets.US_ASCII),
        startHeader,
        endHeader)
            .getBytes(Charsets.US_ASCII);
    int dataLength = endDataOffset - startDataOffset;
    byte out[] = new byte[newHeader.length + dataLength];
    System.arraycopy(newHeader, 0, out, 0, newHeader.length);
    System.arraycopy(b, startDataOffset, out, newHeader.length, dataLength);
    return out;
  }
  
  private static final String CR_LF = "\r\n";
  private static final Splitter LINE = Splitter.on(CR_LF).omitEmptyStrings();
  
  private String modifyFileHeader(String headers, int startHeader, int endHeader) {
    List<String> lines = LINE.splitToList(headers);
    return Joiner.on(CR_LF).join(modifyFileHeader(lines, startHeader, endHeader)) + CR_LF;
  }
  
  private List<String> modifyFileHeader(List<String> lines, int startHeader, int endHeader) {
    List<String> output = new ArrayList<>();
    int headerIndex = 0;
    for (String line : lines) {
      if (registration != null && line.startsWith("$U,")) {
        line = String.format("U,%s", registration);
        int checksum = 0;
        for (byte b : line.getBytes()) {
          checksum ^= b;
        }
        line = String.format("$%s*%X", line, checksum);  // $ and * are not part of checksum.
      } else if (line.startsWith("$D,")) {
        headerIndex++;
        if (headerIndex - 1 < startHeader || headerIndex - 1 > endHeader) {
          continue;
        }
      }
      output.add(line);
    }
    return output;
  }
}
