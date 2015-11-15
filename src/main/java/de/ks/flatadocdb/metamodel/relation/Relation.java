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

import de.ks.flatadocdb.session.Session;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

public abstract class Relation {
  protected final Class<?> relationType;
  protected final Field relationField;
  protected final boolean lazy;
  protected final MethodHandle getterHandle;
  protected final MethodHandle setterHandle;

  public Relation(Class<?> relationType, Field relationField, boolean lazy) {
    this.relationType = relationType;
    this.lazy = lazy;
    this.relationField = relationField;
    try {
      relationField.setAccessible(true);
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      getterHandle = lookup.unreflectGetter(relationField);
      setterHandle = lookup.unreflectSetter(relationField);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the actual type of the relation, in a ToOne this would be the fields type, in a collection this would be the generic element type
   */
  public Class<?> getRelationType() {
    return relationType;
  }

  public Field getRelationField() {
    return relationField;
  }

  public boolean isLazy() {
    return lazy;
  }

  public Object getFieldInstance() {
    return null;
  }

  public abstract boolean isCollection();

  public abstract void setupLazy(Object entity, Collection<String> ids, Session session);

  @SuppressWarnings("unchecked")
  public Collection<Object> getRelatedEntities(Object entity) {
    try {
      Object value = getterHandle.invoke(entity);
      if (value instanceof Collection) {
        return (Collection<Object>) value;
      } else if (value != null) {
        return Collections.singleton(value);
      } else {
        return Collections.emptyList();
      }
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  public void setRelatedEntities(Object owner, Collection<Object> relatedEntitities) {
    Object value = null;
    if (isCollection()) {
      @SuppressWarnings("unchecked")
      Collection<Object> collectionInstance = (Collection<Object>) getFieldInstance();
      collectionInstance.addAll(relatedEntitities);
      value = collectionInstance;
    } else {
      if (relatedEntitities.size() == 1) {
        value = relatedEntitities.iterator().next();
      }
    }
    setValue(owner, value);
  }

  protected void setValue(Object owner, Object value) {
    try {
      setterHandle.invoke(owner, value);
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Relation)) {
      return false;
    }
    Relation relation = (Relation) o;
    return relationField.equals(relation.relationField);
  }

  @Override
  public int hashCode() {
    return relationField.hashCode();
  }

}
