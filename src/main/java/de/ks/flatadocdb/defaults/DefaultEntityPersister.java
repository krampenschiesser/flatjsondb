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
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.json.RelationCollectionPropertyWriter;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultEntityPersister implements EntityPersister {
  private static final Logger log = LoggerFactory.getLogger(DefaultEntityPersister.class);
  final ObjectMapper mapper = new ObjectMapper();

  public DefaultEntityPersister() {
    mapper.findAndRegisterModules();
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.enableDefaultTyping();
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  //FIXME need to add this to initialization cycle
  public void initialize(Repository repository, MetaModel metaModel) {
    mapper.registerModule(new Module() {
      @Override
      public String getModuleName() {
        return "JSONFilter";
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
            EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(beanDesc.getBeanClass());

            List<BeanPropertyWriter> relationProperties = beanProperties.stream()//
              .filter(p -> entityDescriptor.isRelation(p.getMember())).collect(Collectors.toList());

            ArrayList<BeanPropertyWriter> all = new ArrayList<>(beanProperties);
            all.removeAll(relationProperties);
            relationProperties.stream().map(old -> new RelationCollectionPropertyWriter(old, metaModel)).forEach(n -> all.add(n));
            return all;
          }
        });

        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
          @Override
          public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
            EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(beanDesc.getBeanClass());
//builder.getProperties().next().
            beanDesc.findProperties().stream().filter(p -> !entityDescriptor.isRelation(p.getPrimaryMember())).forEach(p -> builder.removeProperty(p.getFullName()));
            return super.updateBuilder(config, beanDesc, builder);
          }
        });
//        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
//          @Override
//          public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
//            EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(beanDesc.getBeanClass());
//
//            List<BeanPropertyDefinition> properties = beanDesc.findProperties().stream().filter(p -> entityDescriptor.isCollectionRelation(p.getPrimaryMember())).collect(Collectors.toList());
//            for (BeanPropertyDefinition property : properties) {
//              CollectionType javaType = context.getTypeFactory().constructCollectionType((Class<? extends Collection>) property.getField().getRawType(), String.class);
//              FieldProperty fieldProperty = new FieldProperty(property, javaType, null, null, property.getField());
//              builder.addOrReplaceProperty(fieldProperty, true);
//            }
//
//            log.info("updateBuilder for {}", beanDesc.getBeanClass());
//            return super.updateBuilder(config, beanDesc, builder);
//          }
//
//          @Override
//          public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
//
//            log.info("updateProperties for {}", beanDesc.getBeanClass());
//            return super.updateProperties(config, beanDesc, propDefs);
//          }
//
//          @Override
//          public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
//            log.info("modifyDeserializer for {}", beanDesc.getBeanClass());
//            return super.modifyDeserializer(config, beanDesc, deserializer);
//          }
//        });
      }
    });
  }

  @Override
  public Object load(Repository repository, EntityDescriptor descriptor, Path path) {
    try {
      JsonNode jsonNode = mapper.readTree(path.toFile());
      descriptor.getAllRelations().forEach(rel -> {
        String name = rel.getRelationField().getName();
        JsonNode jsonValue = jsonNode.findValue(name);
        if (jsonValue.elements().hasNext()) {
          jsonValue = jsonValue.elements().next();
        }
        String text = jsonValue.asText();
        text.toCharArray();
      });
      return mapper.readValue(path.toFile(), descriptor.getEntityClass());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] createFileContents(Repository repository, EntityDescriptor descriptor, Object object) {
    try {
//      return mapper.writeValueAsBytes(object);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canParse(Path path, EntityDescriptor descriptor) {
    if (path.toFile().exists()) {
      try (FileInputStream stream = new FileInputStream(path.toFile())) {
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream))) {
          String line1 = reader.readLine();
          String line2 = reader.readLine();
          line1 = line1 == null ? "" : line1.trim();
          line2 = line2 == null ? "" : line2.trim();
          if (checkLine(line1, line2, descriptor.getEntityClass())) {
            return true;
          } else {
            return false;
          }
        }
      } catch (IOException e) {
        return false;
      }
    }
    return false;
  }

  private boolean checkLine(String line1, String line2, Class<?> clazz) {
    if (line2.startsWith("\"" + clazz.getName() + "\"")) {
      return true;
    } else if (line1.startsWith("{\"" + clazz.getName() + "\":")) {
      return true;
    } else {
      return false;
    }
  }
}
