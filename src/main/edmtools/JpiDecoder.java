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

import edmtools.Proto.FlightMetadata;
import edmtools.Proto.JpiFile;

/**
 * Public API for decoding a JPI data file. 
 */
public class JpiDecoder {
  public static class JpiDecoderConfiguration {
    private JpiDecoderConfiguration(boolean headersOnly, Integer startFlightNumber, Integer endFlightNumber) {
      this.headersOnly = headersOnly;
      this.startFlightNumber = startFlightNumber;
      this.endFlightNumber = endFlightNumber;
    }
    
    private boolean headersOnly;
    private Integer startFlightNumber;
    private Integer endFlightNumber;
    
    public static Builder newBuilder() { return new Builder(); }
    
    public static class Builder {
      private Builder() {}
      private boolean headersOnly;
      private Integer startFlightNumber;
      private Integer endFlightNumber;
   
      /**
       * If called, only the metadata for each flight will be parsed.  The actual data will be
       * skipped.  The returned {@code Flight} will not have the {@code data} field populated.
       */
      public Builder withFlightHeadersOnly() {
        headersOnly = true;
        return this;
      }
      
      /**
       * Selects a minimum flight number to parse.  Flight numbers smaller than this will be skipped.
       */
      public Builder withStartFlightNumber(int startFlightNumber) {
        this.startFlightNumber = startFlightNumber;
        return this;
      }
      
      /**
       * Selects a maximum flight number to parse.  Flight numbers larger than this will be skipped.
       */
      public Builder withEndFlightNumber(int endFlightNumber) {
        this.endFlightNumber = endFlightNumber;
        return this;
      }
      
      /**
       * Selects a single flight number to parse.
       */
      public Builder withExactFlightNumber(int flightNumber) {
        this.startFlightNumber = flightNumber;
        this.endFlightNumber = flightNumber;
        return this;
      }
      
      public JpiDecoderConfiguration build() {
        return new JpiDecoderConfiguration(headersOnly, startFlightNumber, endFlightNumber);
      }
    }
  }
  
  /**
   * Decodes a {@link JpiInputStream}, based on the settings of {@link JpiDecoderConfiguration},
   * into a {@link JpiFile} protocol buffer.
   */
  public static JpiFile decode(JpiInputStream inputStream, JpiDecoderConfiguration config)
      throws IOException {
    JpiFile.Builder jpiFile = JpiFile.newBuilder();
    jpiFile.setMetadata(new MetadataParser(inputStream).parse());
    MetadataUtil metadataUtil = new MetadataUtil(jpiFile.getMetadata());
    for (FlightMetadata flightMetadata : jpiFile.getMetadata().getFlightMetadataList()) {
      FlightParser parser = new FlightParser(inputStream, flightMetadata, metadataUtil);
      int flightNumber = flightMetadata.getFlightNumber();
      if ((config.startFlightNumber != null && flightNumber < config.startFlightNumber)
          || (config.endFlightNumber != null && flightNumber > config.endFlightNumber)) {
        parser.parseHeaderAndSkipData();
        continue;
      }
      jpiFile.addFlight(config.headersOnly ? parser.parseHeaderAndSkipData() : parser.parse());
    }
    return jpiFile.build();
  }
}
