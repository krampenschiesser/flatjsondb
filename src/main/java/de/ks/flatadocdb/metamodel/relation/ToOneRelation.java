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
import de.ks.flatadocdb.session.relation.LazyEntity;

import java.lang.reflect.Field;
import java.util.Collection;

public class ToOneRelation extends Relation {
  public ToOneRelation(Class<?> relationType, Field relationField, boolean lazy) {
    super(relationType, relationField, lazy);
  }

  @Override
  public boolean isCollection() {
    return false;
  }

  @Override
  public void setupLazy(Object entity, Collection<String> ids, Session session) {
    if (ids.size() == 1) {
      Object value = LazyEntity.proxyFor(getRelationType(), ids.iterator().next(), session, entity, getRelationField());
      setValue(entity, value);
    }
  }
}
