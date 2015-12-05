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

package de.ks.flatadocdb.metamodel.relation;

import de.ks.flatadocdb.annotation.*;
import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.ifc.FolderGenerator;
import de.ks.flatadocdb.metamodel.BaseParser;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RelationParser extends BaseParser {
  private static final Logger log = LoggerFactory.getLogger(RelationParser.class);

  static final List<Class<?>> allowedCollectionTypes = new ArrayList<Class<?>>() {
    private static final long serialVersionUID = 1L;

    {
      add(List.class);
      add(Set.class);
    }
  };

  public Set<ToOneChildRelation> parseToOneChildRelations(Class<?> clazz) {
    HashSet<ToOneChildRelation> retval = new HashSet<>();
    Set<Field> toOneFields = getFields(clazz, Child.class, this::checkToOneFields);
    for (Field toOneField : toOneFields) {
      Class<?> type = toOneField.getType();
      Child annotation = toOneField.getAnnotation(Child.class);
      boolean lazy = annotation.lazy();
      FileGenerator fileGenerator = getInstance(annotation.fileGenerator());
      FolderGenerator folderGenerator = getInstance(annotation.folderGenerator());
      log.debug("Found {} at {} in {}", ToManyChildRelation.class.getSimpleName(), toOneField, clazz);
      retval.add(new ToOneChildRelation(type, toOneField, lazy, folderGenerator, fileGenerator));
    }
    return retval;
  }

  public Set<ToManyChildRelation> parseToManyChildRelations(Class<?> clazz) {
    HashSet<ToManyChildRelation> retval = new HashSet<>();
    Set<Field> toManyFields = getFields(clazz, Children.class, this::checkToManyFields);
    for (Field toManyField : toManyFields) {
      Class<?> collectionType = toManyField.getType();
      ParameterizedType genericType = (ParameterizedType) toManyField.getGenericType();
      Class<?> type = (Class<?>) genericType.getActualTypeArguments()[0];
      Children annotation = toManyField.getAnnotation(Children.class);
      boolean lazy = annotation.lazy();
      FileGenerator fileGenerator = getInstance(annotation.fileGenerator());
      FolderGenerator folderGenerator = getInstance(annotation.folderGenerator());
      log.debug("Found {} at {} in {}", ToManyChildRelation.class.getSimpleName(), toManyField, clazz);
      retval.add(new ToManyChildRelation(type, collectionType, toManyField, lazy, folderGenerator, fileGenerator));
    }
    return retval;
  }

  public Set<ToOneRelation> parseToOneRelations(Class<?> clazz) {
    HashSet<ToOneRelation> retval = new HashSet<>();
    Set<Field> toOneFields = getFields(clazz, ToOne.class, this::checkToOneFields);
    for (Field toOneField : toOneFields) {
      Class<?> type = toOneField.getType();
      boolean lazy = toOneField.getAnnotation(ToOne.class).lazy();
      log.debug("Found {} at {} in {}", ToOneRelation.class.getSimpleName(), toOneField, clazz);
      retval.add(new ToOneRelation(type, toOneField, lazy));
    }
    return retval;
  }

  public Set<ToManyRelation> parseToManyRelations(Class<?> clazz) {
    HashSet<ToManyRelation> retval = new HashSet<>();
    Set<Field> toManyFields = getFields(clazz, ToMany.class, this::checkToManyFields);
    for (Field toManyField : toManyFields) {
      Class<?> collectionType = toManyField.getType();
      ParameterizedType genericType = (ParameterizedType) toManyField.getGenericType();
      Class<?> type = (Class<?>) genericType.getActualTypeArguments()[0];
      boolean lazy = toManyField.getAnnotation(ToMany.class).lazy();
      log.debug("Found {} at {} in {}", ToManyRelation.class.getSimpleName(), toManyField, clazz);
      retval.add(new ToManyRelation(type, collectionType, toManyField, lazy));
    }
    return retval;
  }

  private void checkToManyFields(Set<Field> fields) {
    for (Field field : fields) {
      Class<?> type = field.getType();
      if (!allowedCollectionTypes.contains(type)) {
        throw new IllegalArgumentException("Type of " + ToMany.class.getSimpleName() + " assocation has to be one of the following: " + allowedCollectionTypes);
      }

      Type genericType = field.getGenericType();
      if (genericType instanceof ParameterizedType) {
        checkParameterizedType(field, (ParameterizedType) genericType);
      } else {
        throw new IllegalArgumentException("No parameterized generic type for field " + field);
      }
    }
  }

  private void checkParameterizedType(Field field, ParameterizedType genericType) {
    ParameterizedType parameterized = genericType;
    Type[] actualTypeArguments = parameterized.getActualTypeArguments();
    for (Type actualTypeArgument : actualTypeArguments) {
      if (actualTypeArgument instanceof Class) {
        Class<?> clazz = (Class<?>) actualTypeArgument;
        boolean isEntity = clazz.isAnnotationPresent(Entity.class);
        if (!isEntity) {
          throw new IllegalArgumentException("No entity as generic type argument on " + field);
        }
      } else {
        throw new IllegalArgumentException("Unkown generic type " + actualTypeArgument + " on " + field);
      }
    }
  }

  private void checkToOneFields(Set<Field> fields) {
    for (Field field : fields) {
      if (!field.getType().isAnnotationPresent(Entity.class)) {
        throw new IllegalArgumentException("Annotated field " + field + " is no entity");
      }
    }
  }

  private Set<Field> getFields(Class<?> clazz, Class<? extends Annotation> annotation, Consumer<Set<Field>> checker) {
    @SuppressWarnings("unchecked")
    Set<Field> allFields = ReflectionUtils.getAllFields(clazz, f -> !Modifier.isStatic(f.getModifiers()), f -> f.isAnnotationPresent(annotation));
    checker.accept(allFields);
    return allFields;
  }
}
