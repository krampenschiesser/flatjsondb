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

package de.ks.flatadocdb.defaults;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultEntityPersister implements EntityPersister {
  final ObjectMapper mapper = new ObjectMapper();

  public DefaultEntityPersister() {
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.enableDefaultTyping();
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.registerModule(new Module() {
      @Override
      public String getModuleName() {
        return "FlatAdocDBFilter";
      }

      @Override
      public Version version() {
        return new Version(1, 0, 0, "none", null, null);
      }

      @Override
      public void setupModule(SetupContext context) {
        context.addBeanSerializerModifier(new BeanSerializerModifier() {
          @Override
          public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()//
//                .filter(p -> !p.getType().getRawClass().equals(Core.class))//
//                .filter(p -> !p.getName().equals("properties"))//
              .collect(Collectors.toList());
          }
        });
      }
    });
  }

  @Override
  public Object load(Repository repository, EntityDescriptor descriptor, Path path) {
    try {
      return mapper.readValue(path.toFile(), descriptor.getEntityClass());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Repository repository, EntityDescriptor descriptor, Path target, Object object) {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
