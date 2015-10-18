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
package de.ks.flatadocdb.session;

import de.ks.flatadocdb.exception.NoIdField;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.index.LocalIndex;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@NotThreadSafe//can only be used as ThreadLocal
public class Session {
  protected final MetaModel metaModel;
  protected final LocalIndex localIndex;

  protected final Map<String, SessionEntry> entriesById = new HashMap<>();
  protected final Map<Object, SessionEntry> entriesByNaturalId = new HashMap<>();
  protected final Map<Object, SessionEntry> entity2Entry = new HashMap<>();

  public Session(MetaModel metaModel, LocalIndex localIndex) {
    this.metaModel = metaModel;
    this.localIndex = localIndex;
  }

  public void persist(Object entity) {
    Objects.requireNonNull(entity);

  }

  public void remove(Object entity) {
    if (entity != null) {

    }
  }

  public <E> Optional<E> findByNaturalId(Class<E> clazz, Object naturalId) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findById(Class<E> clazz, String id) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(id);

    SessionEntry sessionEntry = entriesById.get(id);
    if (sessionEntry == null) {
      IndexElement indexElement = localIndex.getById(id);
      return Optional.ofNullable((E) load(indexElement));
    } else {
      return Optional.of((E) sessionEntry.object);
    }
  }

  private Object load(IndexElement indexElement) {
    EntityDescriptor descriptor = metaModel.getEntityDescriptor(indexElement.getEntityClass());
    EntityPersister persister = descriptor.getPersister();
    Object load = persister.load(descriptor);
    SessionEntry sessionEntry = new SessionEntry();
    return null;
  }

  public Optional<String> getId(Object entity) {
    Objects.requireNonNull(entity);

    SessionEntry sessionEntry = entity2Entry.get(entity);
    if (sessionEntry == null) {//read from entity
      EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());
      if (entityDescriptor.hasIdAccess()) {
        return Optional.ofNullable(entityDescriptor.getId(entity));
      } else {
        throw new NoIdField(entity.getClass());
      }
    } else {
      return Optional.of(sessionEntry.id);
    }
  }

}
