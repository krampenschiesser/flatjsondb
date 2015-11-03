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

import de.ks.flatadocdb.annotation.Child;
import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.annotation.ToMany;
import de.ks.flatadocdb.annotation.ToOne;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

public class RelationParser {
  static final List<Class<?>> allowedCollectionTypes = new ArrayList<Class<?>>() {{
    add(List.class);
    add(Set.class);
  }};

  public Set<Relation> parse(Class<?> clazz) {
    HashSet<Relation> retval = new HashSet<>();

    Set<Field> toOneFields = getFields(clazz, ToOne.class, this::checkToOneFields);
    Set<Field> toManyFields = getFields(clazz, ToMany.class, this::checkToManyFields);
    Set<Field> childFields = getFields(clazz, Child.class, this::checkChildRelation);


    return retval;
  }

  private void checkChildRelation(Set<Field> fields) {
    for (Field field : fields) {
      Class<?> type = field.getType();
      if (allowedCollectionTypes.contains(type)) {
        checkToManyFields(Collections.singleton(field));
      } else {
        checkToOneFields(Collections.singleton(field));
      }
    }
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
