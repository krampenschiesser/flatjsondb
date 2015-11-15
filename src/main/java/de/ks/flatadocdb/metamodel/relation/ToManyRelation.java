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
import de.ks.flatadocdb.session.relation.RelationList;
import de.ks.flatadocdb.session.relation.RelationSet;

import java.lang.reflect.Field;
import java.util.*;

public class ToManyRelation extends Relation {
  protected final Class<?> collectionType;

  public ToManyRelation(Class<?> relationType, Class<?> collectionType, Field relationField, boolean lazy) {
    super(relationType, relationField, lazy);
    this.collectionType = collectionType;
  }

  public Class<?> getCollectionType() {
    return collectionType;
  }

  @Override
  public Object getFieldInstance() {
    if (collectionType.equals(List.class)) {
      return new ArrayList<>();
    } else if (collectionType.equals(Set.class)) {
      return new HashSet<>();
    } else {
      throw new IllegalArgumentException("Unkown collection type " + collectionType);
    }
  }

  @Override
  public boolean isCollection() {
    return true;
  }

  @Override
  public void setupLazy(Object entity, Collection<String> ids, Session session) {
    if (collectionType.equals(List.class)) {
      RelationList<Object> relation = new RelationList<>(new ArrayList<>(ids), session);
      setValue(entity, relation);
    } else if (collectionType.equals(Set.class)) {
      RelationSet<Object> relation = new RelationSet<>(new HashSet<>(ids), session);
      setValue(entity, relation);
    } else {
      throw new IllegalArgumentException("Unkown collection type " + collectionType);
    }
  }
}
