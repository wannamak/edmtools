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

import org.joda.time.DateTime;
import org.kohsuke.args4j.Option;

import com.google.common.base.Preconditions;

import edmtools.JpiDecoder;
import edmtools.JpiDecoder.JpiDecoderConfiguration;
import edmtools.JpiInputStream;
import edmtools.Proto.Flight;
import edmtools.Proto.JpiFile;

/**
 * Demo tool which decodes and prints a flight from a JPI file.
 */
public class JpiDecode extends CommandLineTool {
  @Option(name = "-list", usage="list available flights", aliases="--list")
  private boolean listAllFlights;
  
  @Option(name = "-flightNumber", usage="flight number to parse (-1 to decode all)", 
      aliases={"--flightNumber", "-flight", "--flight"})
  private int flightNumber = -1;

  public static void main(String args[]) throws Exception {
    CommandLineTool.initAndRun(args, new JpiDecode());
  }

  @Override
  public void run() throws Exception {
    Preconditions.checkArgument(!args.isEmpty(), "Specify a .JPI filename");
    JpiInputStream inputStream = new JpiInputStream(args.get(0));
    if (listAllFlights) {
      JpiFile jpiFile = JpiDecoder.decode(
          inputStream,
          JpiDecoderConfiguration.newBuilder().withFlightHeadersOnly().build());
      for (Flight flight : jpiFile.getFlightList()) {
        System.out.printf("Flight number %4d at %s\n", flight.getFlightNumber(), 
            new DateTime(flight.getStartTimestamp() * 1000));
      }
      return;
    }
    
    JpiDecoderConfiguration.Builder configBuilder = JpiDecoderConfiguration.newBuilder();
    if (flightNumber != -1) {
      configBuilder.withExactFlightNumber(flightNumber);
    }
    JpiFile jpiFile = JpiDecoder.decode(inputStream, configBuilder.build());
    if (flightNumber != -1 && jpiFile.getFlightList().isEmpty()) {
      System.out.printf("Flight number %d not found.\n", flightNumber);
      return;
    }
    System.out.println(jpiFile);
  }
}
