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
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import edmtools.Proto.DataRecord;
import edmtools.Proto.DataRecord.Mark;

public class BuilderUtilTest {
  private static final String OAT = "outside_air_temperature";
  private static final String CHT = "engine[0].cylinder_head_temperature[0]";
  private static final String MARK = "mark";
  private static final String MAP = "engine[0].manifold_pressure";
  
  private DataRecord.Builder builder = DataRecord.newBuilder();
  private BuilderUtil util = new BuilderUtil(builder);
  
  @Test
  public void testHasValue() {
    assertFalse(util.hasField(OAT));
    assertFalse(util.hasField("engine[0].exhaust_gas_temperature[1]"));
  }
  
  @Test
  public void testSetGetSingle() {
    util.setFieldValue(OAT, 1);
    assertEquals(1, util.getFieldValue(OAT));
    util.setFieldValue(MAP, 1.0f);
    assertEquals(1.0f, util.getFieldValue(MAP));
  }
  
  @Test
  public void testCoerceNumericToInt() {
    util.setFieldValue(CHT, 1);
    assertEquals(1, util.getFieldValue(CHT));
    util.setFieldValue(CHT, 1.0f);
    assertEquals(1, util.getFieldValue(CHT));
    util.setFieldValue(CHT, 1.0);
    assertEquals(1, util.getFieldValue(CHT));
    util.setFieldValue(CHT, 1L);
    assertEquals(1, util.getFieldValue(CHT));
  }

  @Test
  public void testCoerceNumericToFloat() {
    util.setFieldValue(MAP, 1);
    assertEquals(1.0f, util.getFieldValue(MAP));
    util.setFieldValue(MAP, 1.0f);
    assertEquals(1.0f, util.getFieldValue(MAP));
    util.setFieldValue(MAP, 1.0);
    assertEquals(1.0f, util.getFieldValue(MAP));
    util.setFieldValue(MAP, 1L);
    assertEquals(1.0f, util.getFieldValue(MAP));
  }
  
  @Test
  public void testCoerceNumericToEnum() {
    util.setFieldValue(MARK, Mark.MARKED.getValueDescriptor());
    assertEquals(Mark.MARKED.getValueDescriptor(), util.getFieldValue(MARK));
    util.setFieldValue(MARK, 1);
    assertEquals(Mark.MARKED.getValueDescriptor(), util.getFieldValue(MARK));
  }

  @Test
  public void testSetGetRepeated() {
    util.setFieldValue("engine[0].cylinder_head_temperature[0]", 100);
    assertEquals(100, util.getFieldValue("engine[0].cylinder_head_temperature[0]"));
    util.setFieldValue("engine[0].cylinder_head_temperature[1]", 200);
    assertEquals(200, util.getFieldValue("engine[0].cylinder_head_temperature[1]"));
    util.setFieldValue("engine[1].cylinder_head_temperature[0]", 300);
    assertEquals(300, util.getFieldValue("engine[1].cylinder_head_temperature[0]"));
    assertEquals(
        "engine {\n" +
        "  cylinder_head_temperature: 100\n" +
        "  cylinder_head_temperature: 200\n" +
        "}\n" +
        "engine {\n" +
        "  cylinder_head_temperature: 300\n" +
        "}\n",
        builder.build().toString());
  }
}
