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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

/** Easy bitmap construction and testing. */
class BitSet {
  private int[] data;  // only 8 bits of each int are used.
  private int length;  // in bytes
  
  public BitSet(int numBytes) {
    data = new int[numBytes];  // Low = 0.
    length = numBytes;
    clear();
  }
  
  public void clear() {
    for (int i = 0; i < length; ++i) {
      data[i] = 0;
    }
  }
  
  /** Sets a single byte.  Index 0 contains the lowest-order bits. */
  public void setByte(int index, int value) {
    data[index] = value;
  }

  /** Sets a short, with the lowest-order bits at index. */
  public void setWord(int index, int value) {
    data[index + 1] = (value >> 8) & 0xff;
    data[index] = value & 0xff;
  }
  
  public int numBytes() {
    return length;
  }
  
  public int numBits() {
    return length * 8;
  }
  
  public boolean testBit(int bitIndex) {
    int byteIndex = bitIndex / 8;
    bitIndex = bitIndex % 8;
    return (data[byteIndex] & (1 << bitIndex)) > 0;
  }
  
  public void setBit(int bitIndex) {
    int byteIndex = bitIndex / 8;
    bitIndex = bitIndex % 8;
    data[byteIndex] = data[byteIndex] | (1 << bitIndex);
  }

  public void clearBit(int bitIndex) {
    int byteIndex = bitIndex / 8;
    bitIndex = bitIndex % 8;
    data[byteIndex] = data[byteIndex] & ~(1 << bitIndex);
  }
  
  public int extractBits(int startIndex, int endIndex) {
    int result = 0;
    for (int i = endIndex; i >= startIndex; --i) {
      result |= testBit(i) ? 1 : 0;
      result <<= 1;
    }
    return result;
  }
  
  public int countBits(int startIndex, int endIndex) {
    int result = 0;
    for (int i = startIndex; i <= endIndex; ++i) {
      result += testBit(i) ? 1 : 0;
    }
    return result;
  }

  private static final Joiner JOINER = Joiner.on(' ');
  
  public String toString() {
    List<String> bitStrings = new ArrayList<>();
    for (int i = length - 1; i >= 0; --i) {
      bitStrings.add(String.format("%8s", Integer.toBinaryString(data[i])).replace(' ', '0'));
    }
    return JOINER.join(bitStrings);
  }
}
