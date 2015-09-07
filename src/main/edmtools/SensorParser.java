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

import edmtools.Proto.Sensors;

class SensorParser {
  private BitSet mask;
  
  public SensorParser(int low, int high) {
    mask = new BitSet(4);
    mask.setWord(0, low);
    mask.setWord(2, high);
  }
  
  public Sensors parse() {
    Sensors.Builder builder = Sensors.newBuilder();
    
    // 63741 =                     1111 1000 1111 1101
    // 24561 = 0101 1111 1111 0001
    // 32273 = 0111 1110 0001 0001
    //         -m-u fpai r2to eeee eeee eccc cccc cc-b
    
    // Unknown bits are 1, 28, 29, and 31.

    if (mask.testBit(0)) {
      builder.setVoltage(true);
    }
    // TODO: is it useful to capture if, eg probe 2 is configured ?  
    int numEgt = mask.countBits(11, 19);
    if (numEgt > 0) {
      builder.setNumExhaustGasTemperature(numEgt);    
    }
    int numCht = mask.countBits(2, 10);
    if (numCht > 0) {
      builder.setNumCylinderHeadTemperature(numCht);
    }
    for (int i = 20; i < 28; ++i) {
      if (mask.testBit(i)) {
        switch (i) {
          case 20:  builder.setOilTemperature(true); break;
          case 21:  builder.setTurbineInletTemperature1(true); break;
          case 22:  builder.setTurbineInletTemperature2(true); break;
          case 23:  builder.setCompressorDischargeTemperature(true); break;
          case 24:  builder.setInductionAirTemperature(true); break;
          case 25:  builder.setOutsideAirTemperature(true); break;
          case 26:  builder.setRpm(true); break;
          case 27:  builder.setFuelFlow(true); break;
        }
      }
    }
    if (mask.testBit(30)) {
      builder.setManifoldPressure(true);
    }
    return builder.build();
  }
}
