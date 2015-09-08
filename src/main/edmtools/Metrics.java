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

import static edmtools.Metric.UNSUPPORTED_METRIC;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

class Metrics {
  public static Map<Integer, Metric> getBitToMetricMap(MetadataUtil metadataUtil) {
    int versionSelector = getVersionSelector(metadataUtil);
    ImmutableMap.Builder<Integer, Metric> builder = ImmutableMap.builder();
    for (Metric metric : METRICS) {
      if ((metric.getVersionMask() & versionSelector) > 0) {
        builder.put(metric.getLowByteBit(), metric);
        if (metric.getHighByteBit().isPresent()) {
          builder.put(metric.getHighByteBit().get(), metric);
        }
      }
    }
    return builder.build();
  }

  private static int getVersionSelector(MetadataUtil metadataUtil) {
    if (metadataUtil.isModelNumber(760)) return V2;
    if (metadataUtil.isModelNumber(960)) return V5;
    if (metadataUtil.isModelNumberAtLeast(900)) {
      return metadataUtil.isFirmwareVersionAtLeast(108) ? V4 : V3;
    } else {
      return metadataUtil.hasProtocolHeader() ? V4 : V1;
    }
  }

  private static int V1 = 0x1;  // EDM < 900, !hasProtocolHeader
  private static int V2 = 0x2;  // EDM 760
  private static int V3 = 0x4;  // EDM >= 900, older firmware
  private static int V4 = 0x8;  // EDM >= 900, newer firmware or EDM < 900, hasProtocolHeader
  private static int V5 = 0x10; // EDM 960

  private static Metric[] METRICS = {
    // bytes 0 and 6
    new Metric(V1|V2|V3|V4|V5,   0,  48, "engine[0].exhaust_gas_temperature[0]"),
    new Metric(V1|V2|V3|V4|V5,   1,  49, "engine[0].exhaust_gas_temperature[1]"),
    new Metric(V1|V2|V3|V4|V5,   2,  50, "engine[0].exhaust_gas_temperature[2]"),
    new Metric(V1|V2|V3|V4|V5,   3,  51, "engine[0].exhaust_gas_temperature[3]"),
    new Metric(V1|V2|V3|V4|V5,   4,  52, "engine[0].exhaust_gas_temperature[4]"),
    new Metric(V1|V2|V3|V4|V5,   5,  53, "engine[0].exhaust_gas_temperature[5]"),
    new Metric(V1|V2|V3|V4|V5,   6,  54, "engine[0].turbine_inlet_temperature[0]"),
    new Metric(V1|V2|V3|V4|V5,   7,  55, "engine[0].turbine_inlet_temperature[1]"),

    // byte 1
    new Metric(V1|V2|V3|V4|V5,   8,      "engine[0].cylinder_head_temperature[0]"),
    new Metric(V1|V2|V3|V4|V5,   9,      "engine[0].cylinder_head_temperature[1]"),
    new Metric(V1|V2|V3|V4|V5,  10,      "engine[0].cylinder_head_temperature[2]"),
    new Metric(V1|V2|V3|V4|V5,  11,      "engine[0].cylinder_head_temperature[3]"),
    new Metric(V1|V2|V3|V4|V5,  12,      "engine[0].cylinder_head_temperature[4]"),
    new Metric(V1|V2|V3|V4|V5,  13,      "engine[0].cylinder_head_temperature[5]"),
    new Metric(V1|V2|V3|V4|V5,  14,      "engine[0].cylinder_head_temperature_cooling_rate"),
    new Metric(V1|V2|V3|V4|V5,  15,      "engine[0].oil_temperature"),

    // byte 2
    new Metric(V1|V2|V3|V4|V5,  16,      "mark"),
    new Metric(V1   |V3|V4|V5,  17,      "engine[0].oil_pressure"),
    new Metric(V1|V2|V3|V4|V5,  18,      "engine[0].compressor_discharge_temperature"),
    new Metric(V1   |V3|V4|V5,  19,      "engine[0].induction_air_temperature"),
    new Metric(   V2         ,  19,      "engine[1].manifold_pressure", Metric.ScaleFactor.TEN),
    new Metric(V1|V2|V3|V4|V5,  20,      "voltage[0]", Metric.ScaleFactor.TEN),
    new Metric(V1|V2|V3|V4|V5,  21,      "outside_air_temperature"),
    new Metric(V1|V2|V3|V4|V5,  22,      "engine[0].fuel_used[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(V1|V2|V3|V4|V5,  23,      "engine[0].fuel_flow[0]", Metric.ScaleFactor.TEN_IF_GPH),

    // bytes 3 and 7
    new Metric(V1   |V3|V4   ,  24,  56, "engine[0].exhaust_gas_temperature[6]"),
    new Metric(   V2      |V5,  24,  56, "engine[1].exhaust_gas_temperature[0]"),
    new Metric(V1   |V3|V4   ,  25,  57, "engine[0].exhaust_gas_temperature[7]"),
    new Metric(   V2      |V5,  25,  57, "engine[1].exhaust_gas_temperature[1]"),
    new Metric(V1   |V3|V4   ,  26,  58, "engine[0].exhaust_gas_temperature[8]"),
    new Metric(   V2      |V5,  26,  58, "engine[1].exhaust_gas_temperature[2]"),
    new Metric(V1   |V3|V4   ,  27,      "engine[0].cylinder_head_temperature[6]"),
    new Metric(   V2      |V5,  27,  59, "engine[1].exhaust_gas_temperature[3]"),
    new Metric(V1   |V3|V4   ,  28,      "engine[0].cylinder_head_temperature[7]"),
    new Metric(   V2      |V5,  28,  60, "engine[1].exhaust_gas_temperature[4]"),
    new Metric(V1   |V3|V4   ,  29,      "engine[0].cylinder_head_temperature[8]"),
    new Metric(   V2      |V5,  29,  61, "engine[1].exhaust_gas_temperature[5]"),
    new Metric(V1   |V3|V4   ,  30,      "engine[0].horsepower"),
    new Metric(   V2      |V5,  30,  62, "engine[1].turbine_inlet_temperature[0]"),
    new Metric(   V2      |V5,  31,  63, "engine[1].turbine_inlet_temperature[1]"),

    // byte 4
    new Metric(   V2      |V5,  32,      "engine[1].cylinder_head_temperature[0]"),
    new Metric(   V2      |V5,  33,      "engine[1].cylinder_head_temperature[1]"),
    new Metric(   V2      |V5,  34,      "engine[1].cylinder_head_temperature[2]"),
    new Metric(   V2      |V5,  35,      "engine[1].cylinder_head_temperature[3]"),
    new Metric(   V2      |V5,  36,      "engine[1].cylinder_head_temperature[4]"),
    new Metric(   V2      |V5,  37,      "engine[1].cylinder_head_temperature[5]"),
    new Metric(   V2      |V5,  38,      "engine[1].cylinder_head_temperature_cooling_rate"),
    new Metric(   V2      |V5,  39,      "engine[1].oil_temperature"),

    // byte 5
    new Metric(V1|V2|V3|V4|V5,  40,      "engine[0].manifold_pressure", Metric.ScaleFactor.TEN),
    new Metric(V1|V2|V3|V4|V5,  41,  42, "engine[0].rpm"),
    new Metric(   V2      |V5,  43,  44, "engine[1].rpm"),
    new Metric(         V4   ,  44,      "engine[0].hydraulic_pressure[1]"),
    new Metric(   V2      |V5,  45,      "engine[1].compressor_discharge_temperature"),
    new Metric(         V4   ,  45,      "engine[0].hydraulic_pressure[0]"),
    new Metric(   V2      |V5,  46,      "engine[1].fuel_used[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(         V4   ,  46,      "engine[0].fuel_flow[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(         V4   ,  47,      "engine[0].fuel_used[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(   V2      |V5,  47,      "engine[1].fuel_flow[0]", Metric.ScaleFactor.TEN_IF_GPH),

    // byte 8
    new Metric(      V3|V4|V5,  64,      "amperage[0]"),
    new Metric(      V3|V4|V5,  65,      "voltage[1]", Metric.ScaleFactor.TEN),
    new Metric(      V3|V4|V5,  66,      "amperage[1]"),
    new Metric(      V3|V4   ,  67,      "engine[1].fuel_level[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5,  67,      "engine[0].fuel_level[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(      V3|V4   ,  68,      "engine[0].fuel_level[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5,  68,      "engine[0].fuel_level[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(      V3|V4|V5,  69,      "engine[0].fuel_pressure", Metric.ScaleFactor.TEN),
    new Metric(            V5,  70,      "engine[0].horsepower"),
    new Metric(         V4   ,  71,      UNSUPPORTED_METRIC, Metric.ScaleFactor.TEN_IF_GPH),  // left aux level ?
    new Metric(            V5,  71,      "engine[0].fuel_level[2]", Metric.ScaleFactor.TEN_IF_GPH),

    // byte 9
    new Metric(         V4|V5,  72,  76, UNSUPPORTED_METRIC, Metric.ScaleFactor.TEN),  // left ng ?
    new Metric(         V4|V5,  73,  77, UNSUPPORTED_METRIC),  // left np ?
    new Metric(         V4|V5,  74,      "engine[0].torque"),
    new Metric(         V4|V5,  75,      UNSUPPORTED_METRIC),  // left itt, but no high byte ?
    new Metric(         V4|V5,  78,  79, "engine[0].hours", Metric.ScaleFactor.TEN),

    // byte 10
    new Metric(         V4   ,  84,      UNSUPPORTED_METRIC, Metric.ScaleFactor.TEN_IF_GPH),  // right aux level ?

    // byte 11
    new Metric(            V5,  88,      "engine[1].manifold_pressure", Metric.ScaleFactor.TEN),
    new Metric(            V5,  89,      "engine[1].horsepower"),
    new Metric(            V5,  90,      "engine[1].induction_air_temperature"),
    new Metric(            V5,  91,      "engine[1].fuel_level[0]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5,  92,      "engine[1].fuel_level[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5,  93,      "engine[1].fuel_pressure", Metric.ScaleFactor.TEN),
    new Metric(            V5,  94,      "engine[1].oil_pressure", Metric.ScaleFactor.TEN),
    new Metric(            V5,  95,      "engine[1].fuel_level[2]", Metric.ScaleFactor.TEN_IF_GPH),

    // byte 12
    new Metric(            V5,  96, 100, UNSUPPORTED_METRIC, Metric.ScaleFactor.TEN),  // right ng ?
    new Metric(            V5,  97, 101, UNSUPPORTED_METRIC),  // right np ?
    new Metric(            V5,  98,      "engine[1].torque"),
    new Metric(            V5,  99,      UNSUPPORTED_METRIC),  // right itt, but no high byte ?
    new Metric(            V5, 102, 103, "engine[1].hours", Metric.ScaleFactor.TEN),

    // byte 13
    new Metric(            V5, 104, 108, "engine[0].exhaust_gas_temperature[6]"),
    new Metric(            V5, 105, 109, "engine[0].exhaust_gas_temperature[7]"),
    new Metric(            V5, 106, 110, "engine[0].exhaust_gas_temperature[8]"),
    new Metric(            V5, 107,      "engine[1].fuel_flow[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5, 111,      "engine[0].hydraulic_pressure[0]"),

    // byte 14
    new Metric(            V5, 112, 116, "engine[1].exhaust_gas_temperature[6]"),
    new Metric(            V5, 113, 117, "engine[1].exhaust_gas_temperature[7]"),
    new Metric(            V5, 114, 118, "engine[1].exhaust_gas_temperature[6]"),
    new Metric(            V5, 115,      "engine[1].fuel_flow[1]", Metric.ScaleFactor.TEN_IF_GPH),
    new Metric(            V5, 119,      "engine[1].hydraulic_pressure[0]"),

    // byte 15
    new Metric(            V5, 120,      "engine[0].cylinder_head_temperature[6]"),
    new Metric(            V5, 121,      "engine[0].cylinder_head_temperature[7]"),
    new Metric(            V5, 122,      "engine[0].cylinder_head_temperature[8]"),
    new Metric(            V5, 123,      "engine[0].hydraulic_pressure[1]"),
    new Metric(            V5, 124,      "engine[1].cylinder_head_temperature[6]"),
    new Metric(            V5, 125,      "engine[1].cylinder_head_temperature[7]"),
    new Metric(            V5, 126,      "engine[1].cylinder_head_temperature[8]"),
    new Metric(            V5, 127,      "engine[1].hydraulic_pressure[1]"),
  };
}
