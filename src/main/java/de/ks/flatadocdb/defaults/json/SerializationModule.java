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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SerializationModule extends Module {
  private final MetaModel metaModel;

  public SerializationModule(MetaModel metaModel) {
    this.metaModel = metaModel;
  }

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
      public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
        EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(beanDesc.getBeanClass());
        return propDefs.stream().filter(p -> !entityDescriptor.isRelation(p.getPrimaryMember())).collect(Collectors.toList());
      }
    });
  }
}
