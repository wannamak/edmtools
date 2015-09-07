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

import java.util.Iterator;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;

/** 
 * Addresses and updates a proto {@code Builder} using a simple xpath-like syntax.
 * Path is something like field_name[index].field_name
 */
class BuilderUtil {
  private Builder rootBuilder;
  private Splitter PATH_SPLITTER = Splitter.on('.');
  private Splitter INDEX_SPLITTER = Splitter.on(CharMatcher.anyOf("[]"));
  
  public BuilderUtil(Builder rootBuilder) {
    this.rootBuilder = rootBuilder;
  }
  
  public boolean hasField(String path) {
    return new Context(rootBuilder, path).hasField();
  }
  
  public Object getFieldValue(String path) {
    Context context = new Context(rootBuilder, path);
    if (!context.hasField()) {
      return null;
    }
    return context.getFieldValue();
  }
  
  public void setFieldValue(String path, Object object) {
    new Context(rootBuilder, path).setFieldValue(object);
  }
  
  public void clearField(String path) {
    new Context(rootBuilder, path).clearField();
  }

  private class Context {
    /** The containing builder of the field to be updated. */
    private Builder builder;
    /** The field to be updated. */
    private FieldDescriptor fieldDescriptor;
    /** If repeated, the index of the repeated field to be updated. */
    private int repeatedFieldIndex;
    private boolean foundField = true;

    /** This constructor decodes the xpath-like path to appropriate member variables. */
    private Context(Builder rootBuilder, String path) {
      builder = rootBuilder;
      Iterator<String> components = PATH_SPLITTER.split(path).iterator();
      while (components.hasNext()) {
        Iterator<String> subcomponents = INDEX_SPLITTER.split(components.next()).iterator();
        String fieldName = subcomponents.next();
        repeatedFieldIndex = subcomponents.hasNext() ? Integer.parseInt(subcomponents.next()) : -1;
        fieldDescriptor = builder.getDescriptorForType().findFieldByName(fieldName);
        if (fieldDescriptor == null) {
          foundField = false;
          return;
        }
        if (components.hasNext()) {
          if (fieldDescriptor.isRepeated()) {
            // We can't get an added builder directly (or I don't know how to do this).
            // Adds and empty message to avoid the IndexOutOfBounds for getRepeatedFieldBuilder.
            while (builder.getRepeatedFieldCount(fieldDescriptor) <= repeatedFieldIndex) {
              builder.addRepeatedField(fieldDescriptor, 
                  builder.newBuilderForField(fieldDescriptor).build());
            }
            builder = builder.getRepeatedFieldBuilder(fieldDescriptor, repeatedFieldIndex);
          } else {
            builder = builder.getFieldBuilder(fieldDescriptor);
          }
        }
      }
    }
    
    public boolean hasField() {
      if (!foundField) {
        return false;
      }
      return fieldDescriptor.isRepeated()
          ? builder.getRepeatedFieldCount(fieldDescriptor) > repeatedFieldIndex
          : builder.hasField(fieldDescriptor);
    }
    
    public Object getFieldValue() {
      Preconditions.checkState(foundField);
      return fieldDescriptor.isRepeated()
          ? builder.getRepeatedField(fieldDescriptor, repeatedFieldIndex)
          : builder.getField(fieldDescriptor);
    }
        
    public void setFieldValue(Object object) {
      Preconditions.checkState(foundField);
      object = maybeCoerceTypes(object);
      if (fieldDescriptor.isRepeated()) {
        if (builder.getRepeatedFieldCount(fieldDescriptor) <= repeatedFieldIndex) {
          builder.addRepeatedField(fieldDescriptor, object);
        } else {
          builder.setRepeatedField(fieldDescriptor, repeatedFieldIndex, object);
        }
      } else {
        builder.setField(fieldDescriptor, object);
      }
    }
    
    private Object maybeCoerceTypes(Object object) {
      switch (fieldDescriptor.getJavaType()) {
        case INT:  return ((Number) object).intValue();
        case FLOAT:  return Math.round(((Number) object).floatValue() * 10) / 10.0f;
        case ENUM:
          if (object instanceof Number) {
            return fieldDescriptor.getEnumType().findValueByNumber(((Number) object).intValue());
          }
        default:  return object;
      }
    }
    
    /** "Clearing" a repeated field means setting it to 0. */
    public void clearField() {
      if (!hasField() || !foundField) {
        return;
      }
      if (fieldDescriptor.isRepeated()) {
        setFieldValue(0);
      } else {
        builder.clearField(fieldDescriptor);
      }
    }
  }
}
