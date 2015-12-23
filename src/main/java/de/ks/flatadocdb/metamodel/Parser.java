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

package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.annotation.*;
import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.ifc.*;
import de.ks.flatadocdb.metamodel.relation.RelationParser;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.ReflectionUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses the entitydescriptor from a given class file.
 */
public class Parser extends BaseParser {

  public EntityDescriptor parse(Class<?> clazz) throws ParseException {
    Entity annotation = checkEntityAnnotation(clazz);
    EntityPersister persister = getInstance(annotation.persister());
    FileGenerator fileGenerator = getInstance(annotation.fileGenerator());
    FolderGenerator folderGenerator = getInstance(annotation.folderGenerator());
    LuceneDocumentExtractor luceneDocumentExtractor = getInstance(annotation.luceneDocExtractor());

    @SuppressWarnings("unchecked")
    Set<Field> allFields = ReflectionUtils.getAllFields(clazz, this::filterField);

    MethodHandle idGetterHandle = resolveIdFieldGetter(clazz, allFields);
    MethodHandle idSetterHandle = resolveIdFieldSetter(clazz, allFields);
    MethodHandle versionGetterHandle = resolveVersionFieldGetter(clazz, allFields);
    MethodHandle versionSetterHandle = resolveVersionFieldSetter(clazz, allFields);
    MethodHandle naturalIdHandle = resolveNaturalIdField(clazz, allFields);
    Pair<MethodHandle, MethodHandle> pathInRepo = resolvePathInRepoField(clazz, allFields);


    Map<Field, PropertyPersister> propertyPersisters = resolvePropertyPersisters(allFields);
    Map<LifeCycle, Set<MethodHandle>> lifecycleMethods = new LifeCycleParser().parseMethods(clazz);

    RelationParser relationParser = new RelationParser();

    QueryParser queryParser = new QueryParser();

    EntityDescriptor.Builder builder = EntityDescriptor.Builder.create();
    builder.entity(clazz);
    builder.id(idGetterHandle, idSetterHandle);
    builder.version(versionGetterHandle, versionSetterHandle);
    builder.natural(naturalIdHandle);
    builder.persister(persister);
    builder.extractor(luceneDocumentExtractor);
    builder.fileGenerator(fileGenerator);
    builder.folderGenerator(folderGenerator);
    builder.properties(propertyPersisters);
    builder.lifecycle(lifecycleMethods);
    builder.toOnes(relationParser.parseToOneRelations(clazz));
    builder.toMany(relationParser.parseToManyRelations(clazz));
    builder.toOneChild(relationParser.parseToOneChildRelations(clazz));
    builder.toManyChild(relationParser.parseToManyChildRelations(clazz));
    builder.queries(queryParser.getQueries(clazz));
    if (pathInRepo != null) {
      builder.pathInRepo(pathInRepo.getKey(), pathInRepo.getValue());
    }
    return builder.build();
  }

  private Map<Field, PropertyPersister> resolvePropertyPersisters(Set<Field> allFields) {
    HashMap<Field, PropertyPersister> retval = new HashMap<>();
    Set<Field> fields = allFields.stream().filter(f -> f.isAnnotationPresent(Property.class)).collect(Collectors.toSet());
    for (Field field : fields) {
      Property annotation = field.getAnnotation(Property.class);
      Class<? extends PropertyPersister> persisterClass = annotation.value();
      PropertyPersister instance = getInstance(persisterClass);
      retval.put(field, instance);
    }
    return retval;
  }

  @SuppressWarnings("unchecked")


  private <T extends EntityPersister> Entity checkEntityAnnotation(Class<?> clazz) {
    check(clazz, c -> !c.isAnnotationPresent(Entity.class), c -> "Annotation " + Entity.class.getName() + " not found on class " + c);
    Entity annotation = clazz.getAnnotation(Entity.class);
    return annotation;
  }

  private MethodHandle resolveIdFieldGetter(Class<?> clazz, Set<Field> allFields) {
    Field idField = resolveExactlyOneField(clazz, allFields, Id.class, "ID", true);
    check(idField, f -> f.getType() != String.class, f -> "Type of ID field is no 'long' on " + clazz.getName());
    MethodHandle idHandle = getGetter(idField);
    return idHandle;
  }

  private MethodHandle resolveIdFieldSetter(Class<?> clazz, Set<Field> allFields) {
    Field idField = resolveExactlyOneField(clazz, allFields, Id.class, "ID", true);
    check(idField, f -> f.getType() != String.class, f -> "Type of ID field is no 'long' on " + clazz.getName());
    MethodHandle idHandle = getSetter(idField);
    return idHandle;
  }

  private MethodHandle resolveVersionFieldGetter(Class<?> clazz, Set<Field> allFields) {
    Field idField = resolveExactlyOneField(clazz, allFields, Version.class, "Version", true);
    check(idField, f -> f.getType() != long.class, f -> "Type of Version field is no 'long' on " + clazz.getName());
    MethodHandle versionHandle = getGetter(idField);
    return versionHandle;
  }

  private MethodHandle resolveVersionFieldSetter(Class<?> clazz, Set<Field> allFields) {
    Field idField = resolveExactlyOneField(clazz, allFields, Version.class, "Version", true);
    check(idField, f -> f.getType() != long.class, f -> "Type of Version field is no 'long' on " + clazz.getName());
    MethodHandle versionHandle = getSetter(idField);
    return versionHandle;
  }

  private Pair<MethodHandle, MethodHandle> resolvePathInRepoField(Class<?> clazz, Set<Field> allFields) {
    Field idField = resolveExactlyOneField(clazz, allFields, PathInRepo.class, "PathInRepo", false);
    if (idField == null) {
      return null;
    } else {
      check(idField, f -> f.getType() != Path.class, f -> "Type of PathInRepo field is no 'java.nio.file.Path' on " + clazz.getName());
      MethodHandle getter = getGetter(idField);
      MethodHandle setter = getGetter(idField);
      return Pair.of(getter, setter);
    }
  }

  private MethodHandle resolveNaturalIdField(Class<?> clazz, Set<Field> allFields) {
    Field naturalIdField = resolveExactlyOneField(clazz, allFields, NaturalId.class, "NaturalID", false);
    if (naturalIdField != null) {
      check(naturalIdField, f -> !Serializable.class.isAssignableFrom(f.getType()), f -> "Type of NaturalId field is not Serializable on " + clazz.getName());
      MethodHandle versionHandle = getGetter(naturalIdField);
      return versionHandle;
    } else {
      return null;
    }
  }

  private Field resolveExactlyOneField(Class<?> clazz, Set<Field> allFields, Class<? extends Annotation> annotation, String member, boolean hasToExist) {
    Set<Field> idFields = allFields.stream().filter(f -> f.isAnnotationPresent(annotation)).collect(Collectors.toSet());
    check(idFields, c -> c.size() > 1, c -> "Multiple " + member + " fields found on " + clazz.getName() + ": " + c);
    if (hasToExist) {
      check(idFields, c -> c.size() == 0, c -> "No " + member + " field found on " + clazz.getName() + ".");
      return idFields.iterator().next();
    } else {
      if (idFields.isEmpty()) {
        return null;
      } else {
        return idFields.iterator().next();
      }
    }
  }

  protected MethodHandle getGetter(Field f) {
    try {
      f.setAccessible(true);
      return MethodHandles.lookup().unreflectGetter(f);
    } catch (IllegalAccessException e) {
      throw new ParseException("Could not extract getter handle for " + f + " on " + f.getDeclaringClass().getName(), e);
    }
  }

  protected MethodHandle getSetter(Field f) {
    try {
      f.setAccessible(true);
      return MethodHandles.lookup().unreflectSetter(f);
    } catch (IllegalAccessException e) {
      throw new ParseException("Could not extract setter handle for " + f + " on " + f.getDeclaringClass().getName(), e);
    }
  }

  protected boolean filterField(Field f) {
    int modifiers = f.getModifiers();
    return !Modifier.isStatic(modifiers);
  }

}
