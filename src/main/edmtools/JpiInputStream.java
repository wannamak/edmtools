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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

// TODO: *ducks* this isn't really an InputStream.
public class JpiInputStream {
  private final InputStream stream;

  private final List<Integer> currentRecord;

  /** Current byte counter, independent of currentRecord. */
  private int counter;

  public JpiInputStream(String filename) throws FileNotFoundException {
    this(new BufferedInputStream(new FileInputStream(new File(filename))));
  }

  public JpiInputStream(InputStream stream) {
    this.stream = stream;
    this.currentRecord = new ArrayList<>();
    this.counter = 0;
    Preconditions.checkArgument(stream.markSupported(), "stream must support mark()");
  }

  public void clearCurrentRecord() {
    currentRecord.clear();
  }

  public Optional<String> getChecksumFailureMessage() throws IOException {
    read();  // Read the checksum into currentRecord
    int computedChecksum = computeCurrentRecordChecksum();
    if (computedChecksum != 0) {
      String failureMessage = String.format("Checksum mismatch actual %2X vs expected %2X:\n%s",
          currentRecord.get(currentRecord.size() - 1),
          -computedChecksum & 0xff,
          getCurrentRecord());
      return Optional.of(failureMessage);
    } else {
      return Optional.absent();
    }
  }

  private int computeCurrentRecordChecksum() {
    // TODO: firmware < 3.00 used ^=.  Implement if we get a file to test.
    int computedChecksum = 0;
    for (int b : currentRecord) {
      computedChecksum += b;
    }
    return (-computedChecksum) & 0xff;
  }

  public void resetCounter() {
    counter = 0;
  }

  public int getCounter() {
    return counter;
  }

  /** Reads two bytes.  If the stream is at EOF, throw an {@link IOException}. */
  public int readWord() throws IOException {
    int result = read() << 8;
    return result | read();
  }

  /** Reads a byte.  If the stream is at EOF, throw an {@link IOException}. */
  public int read() throws IOException {
    int read = stream.read();
    if (read == -1) {
      throw new IOException("Unexpected EOF");
    }
    read = read & 0xff;
    counter++;
    currentRecord.add(read);
    return read;
  }

  public int getCurrentRecordSize() {
    return currentRecord.size();
  }

  /** Peeks at the next {@code numBytes} in the underlying {@link InputStream}. */
  public byte[] peek(int numBytes) throws IOException {
    mark(numBytes);
    DataInputStream dis = new DataInputStream(stream);
    byte result[] = new byte[numBytes];
    try {
      dis.readFully(result);
      return result;
    } finally {
      reset();
    }
  }

  public void mark(int readLimit) {
    stream.mark(readLimit);
  }

  public void reset() throws IOException {
    stream.reset();
    // TODO: counter and currentRecord are not updated.
  }

  public void skip(long numBytes) throws IOException {
    counter += numBytes;
    do {
      numBytes -= stream.skip(numBytes);
    } while (numBytes > 0);
    // TODO: currentRecord is not updated.
  }

  public int skipToEndOfFile() throws IOException {
    int length = 0;
    while (stream.read() != -1) {
      length++;
    }
    return length;
  }

  public String getCurrentRecord() {
    String result = "";
    for (int b : currentRecord) {
      result += String.format("%2X", b).replace(' ', '0') + " ";
    }
    return result.trim();
  }
}
