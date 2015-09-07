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

import com.google.common.base.Function;

import edmtools.Proto.Fuel.FuelFlowUnits;
import edmtools.Proto.Metadata;

public class MetadataUtil {
  private Metadata metadata;
  
  public MetadataUtil(Metadata metadata) {
    this.metadata = metadata;
  }
  
  public boolean hasProtocolHeader() {
    return metadata.hasProtocolVersion();
  }
  
  public boolean hasExtraFlightHeaderConfiguration() {
    return hasProtocolHeader() || isModelNumberAtLeast(900);
  }
  
  public boolean isDecodeMaskSingleByte() {
    return !hasProtocolHeader() && !isModelNumberAtLeast(900);
  }
  
  public boolean isModelNumber(int modelNumber) {
    return metadata.getFeatures().getModelNumber() == modelNumber;
  }

  public boolean isModelNumberAtLeast(int modelNumber) {
    return metadata.getFeatures().getModelNumber() >= modelNumber;
  }
  
  public boolean isBuildNumberAtLeast(int buildNumber) {
    return metadata.getFeatures().getBuildNumber() >= buildNumber;
  }
  
  public boolean isFirmwareVersionAtLeast(int versionNumber) {
    return metadata.getFeatures().getFirmwareVersion() >= versionNumber;
  }
  
  public boolean isTwinEngine() {
    return isModelNumber(760) || isModelNumber(960); 
  }
  
  public boolean isGallonsPerHour() {
    return metadata.getFuel().getFuelFlowUnits() == FuelFlowUnits.GPH;
  }
  
  public static final Function<MetadataUtil, Boolean> IS_TWIN_ENGINE = 
      new Function<MetadataUtil, Boolean>() {
    public Boolean apply(MetadataUtil metadataUtil) {
      return metadataUtil.isTwinEngine(); 
    }
  };
  
  public int findFlightMetadataIndexByFlightNumber(int flightNumber) {
    for (int i = 0; i < metadata.getFlightMetadataCount(); ++i) {
      if (metadata.getFlightMetadata(i).getFlightNumber() == flightNumber) {
        return i;
      }
    }
    throw new IllegalStateException("Flight " + flightNumber + " not found");
  }
  
  public boolean isLastFlight(int flightNumber) {
    return metadata.getFlightMetadata(metadata.getFlightMetadataCount() - 1).getFlightNumber() == flightNumber;
  }
}
