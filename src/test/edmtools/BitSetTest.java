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

import org.junit.Test;

public class BitSetTest {
  @Test
  public void testBitSet() {
    BitSet set = new BitSet(3);
    set.setBit(11);
    set.setBit(12);
    assertEquals("00000000 00011000 00000000", set.toString());
    assertEquals(12, set.extractBits(10, 13));
    set.clearBit(10);
    assertEquals("00000000 00011000 00000000", set.toString());
    set.clearBit(11);
    assertEquals("00000000 00010000 00000000", set.toString());
  }
}
