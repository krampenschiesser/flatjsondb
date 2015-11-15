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
import com.fasterxml.jackson.databind.*;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.json.SerializationModule;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.relation.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void initialize(MetaModel metaModel) {
    Module module = new SerializationModule(metaModel);
    mapper.registerModule(module);
  }

  @Override
  public Object load(Repository repository, EntityDescriptor descriptor, Path path, Map<Relation, Collection<String>> relationIds) {
    try {
      JsonNode jsonNode = mapper.readTree(path.toFile());
      descriptor.getAllRelations().forEach(rel -> {
        String name = rel.getRelationField().getName();
        JsonNode jsonValue = jsonNode.findValue(name);
        if (jsonValue.elements().hasNext()) {
          jsonValue = jsonValue.elements().next();
        }

        ArrayList<String> ids = new ArrayList<>();
        relationIds.put(rel, ids);

        if (jsonValue.isContainerNode()) {
          jsonValue.elements().forEachRemaining(id -> ids.add(id.asText()));
        } else if (!jsonValue.isNull()) {
          ids.add(jsonValue.asText());
        }
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
          return checkLine(line1, line2, descriptor.getEntityClass());
        }
      } catch (IOException e) {
        return false;
      }
    }
    return false;
  }

  private boolean checkLine(String line1, String line2, Class<?> clazz) {
    boolean prettyPrintedLine = line2.startsWith("\"" + clazz.getName() + "\"");
    boolean inlineDeclaration = line1.startsWith("{\"" + clazz.getName() + "\":");
    if (prettyPrintedLine) {
      return true;
    } else {
      if (inlineDeclaration) {
        return true;
      } else {
        return false;
      }
    }
  }
}
