/*
 * Copyright [2015] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ks.flatadocdb.defaults.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RelationCollectionPropertyWriter extends BeanPropertyWriter {
  private final MetaModel metaModel;

  public RelationCollectionPropertyWriter(BeanPropertyWriter base, MetaModel metaModel) {
    super(base);
    this.metaModel = metaModel;
  }

  @Override
  public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
    Object value = (_accessorMethod == null) ? _field.get(bean) : _accessorMethod.invoke(bean);
    if (value instanceof Collection) {
      Collection<String> ids = null;
      if (value instanceof List) {
        ids = new ArrayList<>();
      } else {
        ids = new HashSet<>();
      }
      for (Object object : ((Collection) value)) {
        if (object != null) {
          EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(object.getClass());
          String id = entityDescriptor.getId(object);
          ids.add(id);
        }
      }
      value = ids;
    } else if (value != null) {
      EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(value.getClass());
      String id = entityDescriptor.getId(value);
      value = id;
    }
    // Null handling is bit different, check that first
    if (value == null) {
      if (_nullSerializer != null) {
        gen.writeFieldName(_name);
        _nullSerializer.serialize(null, gen, prov);
      }
      return;
    }
    // then find serializer to use
    JsonSerializer<Object> ser = _serializer;
    if (ser == null) {
      Class<?> cls = value.getClass();
      PropertySerializerMap m = _dynamicSerializers;
      ser = m.serializerFor(cls);
      if (ser == null) {
        ser = _findAndAddDynamic(m, cls, prov);
      }
    }
    // and then see if we must suppress certain values (default, empty)
    if (_suppressableValue != null) {
      if (MARKER_FOR_EMPTY == _suppressableValue) {
        if (ser.isEmpty(prov, value)) {
          return;
        }
      } else if (_suppressableValue.equals(value)) {
        return;
      }
    }
    // For non-nulls: simple check for direct cycles
    if (value == bean) {
      // three choices: exception; handled by call; or pass-through
      if (_handleSelfReference(bean, gen, prov, ser)) {
        return;
      }
    }
    gen.writeFieldName(_name);
    if (_typeSerializer == null) {
      ser.serialize(value, gen, prov);
    } else {
      ser.serializeWithType(value, gen, prov, _typeSerializer);
    }
  }
}
