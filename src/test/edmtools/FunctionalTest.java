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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import edmtools.JpiDecoder.JpiDecoderConfiguration;
import edmtools.Proto.DataRecord;
import edmtools.Proto.DataRecord.Mark;
import edmtools.Proto.EngineDataRecord;
import edmtools.Proto.Flight;
import edmtools.Proto.JpiFile;

public class FunctionalTest {
  @Test
  public void testEdm830() throws IOException {
    diff("testdata/edm830.jpi", 45, "testdata/edm830.45.txt");
    diff("testdata/edm830.jpi", 72, "testdata/edm830.72.txt");
  }

  private void diff(String jpiFile, int flightNumber, String goldenCsv) throws IOException {
    Flight golden = readGoldenCsvToProto(goldenCsv);
    Flight parsed = readJpiFile(jpiFile, flightNumber);
    assertEquals(golden.getDataCount(), parsed.getDataCount());
    diff_match_patch dmp = new diff_match_patch();
    for (int i = 0; i < golden.getDataCount(); ++i) {
      List<Diff> unequalDiffs = new ArrayList<>();
      for (Diff diff : dmp.diff_main(golden.getData(i).toString(), parsed.getData(i).toString())) {
        if (diff.operation != Operation.EQUAL) {
          unequalDiffs.add(diff);
        }
      }
      assertTrue("DataRecord " + i + ": " + Joiner.on('\n').join(unequalDiffs),
          unequalDiffs.isEmpty());
    }
  }

  private static final Splitter SPLITTER = Splitter.on(',').trimResults();

  // TODO: this is 830 specific.  Use header row when we have samples from other models.
  private static Flight readGoldenCsvToProto(String filename) throws IOException {
    List<String> lines = Files.readLines(new File(filename), Charsets.UTF_8);
    Flight.Builder builder = Flight.newBuilder();
    for (String line : lines) {
      List<String> parts = SPLITTER.splitToList(line);
      if (parts.size() != 29 || parts.get(0).equals("Date")) {
        continue;
      }
      DataRecord.Builder record = builder.addDataBuilder();
      EngineDataRecord.Builder engineRecord = record.addEngineBuilder();
      engineRecord.addAllExhaustGasTemperature(intSublist(parts, 2, 7));
      engineRecord.addAllCylinderHeadTemperature(intSublist(parts, 8, 13));
      int i = 14;
      engineRecord.setOilTemperature(Integer.parseInt(parts.get(i++)));
      engineRecord.setMaxExhaustGasTemperatureDifference(Integer.parseInt(parts.get(i++)));
      engineRecord.setCylinderHeadTemperatureCoolingRate(Integer.parseInt(parts.get(i++)));
      record.setOutsideAirTemperature(Integer.parseInt(parts.get(i++)));
      record.addVoltage(Float.parseFloat(parts.get(i++)));
      engineRecord.addFuelFlow(Float.parseFloat(parts.get(i++)));
      engineRecord.addFuelUsed(Float.parseFloat(parts.get(i++)));
      engineRecord.setRpm(Integer.parseInt(parts.get(i++)));
      engineRecord.setManifoldPressure(Float.parseFloat(parts.get(i++)));
      engineRecord.setHorsepower(Integer.parseInt(parts.get(i++)));
      engineRecord.setOilPressure(Integer.parseInt(parts.get(i++)));
      engineRecord.setHours(Float.parseFloat(parts.get(i++)));
      switch (parts.get(28)) {
        case "":  record.setMark(Mark.NOT_MARKED); break;
        case "[":  record.setMark(Mark.RICH_START); break;
        case "]":  record.setMark(Mark.RICH_END); break;
        default: throw new IllegalStateException("Need to handle mark [" + parts.get(28) + "]");
      }
    }
    return builder.build();
  }

  private static Iterable<Integer> intSublist(List<String> list, int start, int end) {
    ImmutableList.Builder<Integer> result = ImmutableList.builder();
    for (int i = start; i <= end; ++i) {
      result.add(Integer.parseInt(list.get(i)));
    }
    return result.build();
  }

  private static Flight readJpiFile(String filename, int flightNumber) throws IOException {
    JpiInputStream inputStream = new JpiInputStream(filename);
    JpiFile jpiFile = JpiDecoder.decode(
        inputStream,
        JpiDecoderConfiguration.newBuilder().withExactFlightNumber(flightNumber).build());
    assertEquals(1, jpiFile.getFlightCount());
    return jpiFile.getFlight(0);
  }
}
