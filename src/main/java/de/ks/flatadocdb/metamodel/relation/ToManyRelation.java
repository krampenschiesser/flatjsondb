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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class ToManyRelation {
  protected final Class<?> relationType;
  protected final Class<?> collectionType;
  protected final Field relationField;
  protected final boolean lazy;
  protected final MethodHandle getterHandle;
  protected final MethodHandle setterHandle;

  public ToManyRelation(Class<?> relationType, Class<?> collectionType, Field relationField, boolean lazy) {
    this.relationType = relationType;
    this.collectionType = collectionType;
    this.relationField = relationField;
    this.lazy = lazy;
    try {
      relationField.setAccessible(true);
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      getterHandle = lookup.unreflectGetter(relationField);
      setterHandle = lookup.unreflectSetter(relationField);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public Class<?> getRelationType() {
    return relationType;
  }

  public Class<?> getCollectionType() {
    return collectionType;
  }

  public Field getRelationField() {
    return relationField;
  }

  public boolean isLazy() {
    return lazy;
  }

}
