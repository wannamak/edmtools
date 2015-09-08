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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * A Metric maps a protocol buffer field (expressed with protoPath, see {@link BuilderUtil})
 * to a particular bit in the EDM mask which indicates what values have changed in the latest
 * record cycle.
 *
 * <p>The low byte of all fields are mapped to a particular bit.  Some fields also map a high byte
 * to a different bit.
 */
class Metric {
  public static final String UNSUPPORTED_METRIC = "";
  private static int DEFAULT_VALUE = 240;

  private int versionMask;
  private int lowByteBit;
  private Optional<Integer> highByteBit;
  private String protoPath;
  enum ScaleFactor {
    TEN,
    TEN_IF_GPH
  }
  private Optional<ScaleFactor> scaleFactor;

  public Metric(int versionMask, int lowByteBit, String protoPath) {
    this.versionMask = versionMask;
    this.lowByteBit = lowByteBit;
    this.highByteBit = Optional.absent();
    this.protoPath = protoPath;
    this.scaleFactor = Optional.absent();
  }

  public Metric(int versionMask, int lowByteBit, int highByteBit, String protoPath) {
    this(versionMask, lowByteBit, protoPath);
    this.highByteBit = Optional.of(highByteBit);
  }

  public Metric(int versionMask, int lowByteBit, int highByteBit, String protoPath, ScaleFactor scaleFactor) {
    this(versionMask, lowByteBit, protoPath, scaleFactor);
    this.highByteBit = Optional.of(highByteBit);
  }

  public Metric(int versionMask, int lowByteBit, String protoPath, ScaleFactor scaleFactor) {
    this(versionMask, lowByteBit, protoPath);
    this.scaleFactor = Optional.of(scaleFactor);
  }

  public int getVersionMask() {
    return versionMask;
  }

  public int getLowByteBit() {
    return lowByteBit;
  }

  public Optional<Integer> getHighByteBit() {
    return highByteBit;
  }

  public String getProtoPath() {
    return protoPath;
  }

  public boolean isUnsupported() {
    return protoPath.equals(UNSUPPORTED_METRIC);
  }

  public float getDefaultValue(MetadataUtil metadataUtil) {
    // sic.  One exception to the rule.
    return protoPath.equals("engine[0].horsepower") ? 0 : scale(metadataUtil, DEFAULT_VALUE);
  }

  public float scale(MetadataUtil metadataUtil, float value) {
    if (!scaleFactor.isPresent()) {
      return value;
    }
    if (scaleFactor.get() == ScaleFactor.TEN_IF_GPH && !metadataUtil.isGallonsPerHour()) {
      return value;
    }
    return value /= 10.0f;
  }

  /** Returns true if this bit index corresponds to the high byte index (as opposed to the low) */
  public boolean isHighByteBit(int index) {
    return highByteBit.isPresent() && highByteBit.get() == index;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("versionMask", versionMask)
        .add("lowByteBit", lowByteBit)
        .add("highByteBit", highByteBit)
        .add("protoPath", protoPath)
        .add("scaleFactor", scaleFactor)
        .toString();
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !(object instanceof Metric)) {
      return false;
    }
    Metric that = (Metric) object;
    return Objects.equal(this.versionMask, that.versionMask)
        && Objects.equal(this.lowByteBit, that.lowByteBit)
        && Objects.equal(this.highByteBit, that.highByteBit)
        && Objects.equal(this.protoPath, that.protoPath)
        && Objects.equal(this.scaleFactor, that.scaleFactor);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
