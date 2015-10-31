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

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.defaults.DefaultIdGenerator;
import de.ks.flatadocdb.exception.NoIdField;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.dirtycheck.DirtyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.*;

@NotThreadSafe//can only be used as ThreadLocal
public class Session {
  private static final Logger log = LoggerFactory.getLogger(Session.class);

  protected final MetaModel metaModel;
  protected final Repository repository;
  protected final GlobalIndex globalIndex;
  protected final DefaultIdGenerator idGenerator = new DefaultIdGenerator();

  protected final Map<String, SessionEntry> entriesById = new HashMap<>();
  protected final Map<Object, SessionEntry> entriesByNaturalId = new HashMap<>();
  protected final Map<Object, SessionEntry> entity2Entry = new HashMap<>();

  protected final List<SessionAction> actions = new LinkedList<>();
  protected final DirtyChecker dirtyChecker;

  public Session(MetaModel metaModel, Repository repository, GlobalIndex globalIndex) {
    this.metaModel = metaModel;
    this.repository = repository;
    this.globalIndex = globalIndex;
    dirtyChecker = new DirtyChecker(repository, metaModel);
  }

  public void persist(Object entity) {
    Objects.requireNonNull(entity);

    EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());
    Serializable naturalId = entityDescriptor.getNaturalId(entity);

    Path folder = entityDescriptor.getFolderGenerator().getFolder(repository, entity);
    String fileName = entityDescriptor.getFileGenerator().getFileName(repository, entityDescriptor, entity);

    Path complete = folder.resolve(fileName);

    String id = idGenerator.getSha1Hash(repository.getPath(), complete);

    Optional<?> found = findById(entity.getClass(), id);
    if (found.isPresent()) {
      log.warn("Trying to persist entity {} [{}] twice", entity, complete);
      return;
    }

    entityDescriptor.writetId(entity, id);

    SessionEntry sessionEntry = new SessionEntry(entity, id, 0, naturalId, complete, entityDescriptor);
    addToSession(sessionEntry);

    dirtyChecker.trackPersist(sessionEntry);

    EntityInsertion singleEntityInsertion = new EntityInsertion(repository, sessionEntry);
    actions.add(singleEntityInsertion);
  }

  public void remove(Object entity) {
    Objects.requireNonNull(entity);
    SessionEntry sessionEntry = entity2Entry.get(entity);
    dirtyChecker.trackDelete(sessionEntry);
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findByNaturalId(Class<E> clazz, Object naturalId) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(naturalId);

    SessionEntry sessionEntry = entriesByNaturalId.get(naturalId);
    if (sessionEntry == null) {
      IndexElement indexElement = globalIndex.getByNaturalId(naturalId);
      if (indexElement == null) {
        return Optional.empty();
      } else {
        return Optional.ofNullable((E) load(indexElement));
      }

    } else {
      return Optional.of((E) sessionEntry.object);
    }
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findById(Class<E> clazz, String id) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(id);

    SessionEntry sessionEntry = entriesById.get(id);
    if (sessionEntry == null) {
      IndexElement indexElement = globalIndex.getById(id);
      if (indexElement == null) {
        return Optional.empty();
      } else {
        return Optional.ofNullable((E) load(indexElement));
      }
    } else {
      return Optional.of((E) sessionEntry.object);
    }
  }

  private Object load(IndexElement indexElement) {
    Objects.requireNonNull(indexElement);
    EntityDescriptor descriptor = metaModel.getEntityDescriptor(indexElement.getEntityClass());
    EntityPersister persister = descriptor.getPersister();
    Object object = persister.load(repository, descriptor, indexElement.getPathInRepository());
    SessionEntry sessionEntry = new SessionEntry(object, indexElement.getId(), descriptor.getVersion(object), indexElement.getNaturalId(), indexElement.getPathInRepository(), descriptor);
    addToSession(sessionEntry);
    dirtyChecker.trackLoad(sessionEntry);

    Set<MethodHandle> lifeCycleMethods = descriptor.getLifeCycleMethods(LifeCycle.POST_LOAD);
    for (MethodHandle handle : lifeCycleMethods) {
      try {
        handle.invoke(object);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
    return object;
  }

  private void addToSession(SessionEntry sessionEntry) {
    this.entriesById.put(sessionEntry.getId(), sessionEntry);
    this.entriesByNaturalId.put(sessionEntry.getNaturalId(), sessionEntry);
    this.entity2Entry.put(sessionEntry.getObject(), sessionEntry);
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

  public void prepare() {
    Collection<SessionEntry> dirty = dirtyChecker.findDirty(this.entriesById.values());
    dirty.stream().map(e -> new EntityUpdate(repository, e)).forEach(actions::add);
    actions.forEach(a -> a.prepare(this));
  }

  public void commit() {
    actions.forEach(a -> a.commit(this));
  }

  public void rollback() {
    actions.forEach(a -> a.rollback(this));
  }
}
