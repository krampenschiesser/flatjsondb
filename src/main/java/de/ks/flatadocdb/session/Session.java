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
import de.ks.flatadocdb.exception.IllegalSessionThreadException;
import de.ks.flatadocdb.exception.NoIdField;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.ifc.FolderGenerator;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.index.Index;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.index.LuceneIndex;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.relation.ChildRelation;
import de.ks.flatadocdb.metamodel.relation.Relation;
import de.ks.flatadocdb.session.dirtycheck.DirtyChecker;
import de.ks.flatadocdb.session.transaction.local.TransactionResource;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@NotThreadSafe//can only be used as ThreadLocal
public class Session implements TransactionResource {
  private static final Logger log = LoggerFactory.getLogger(Session.class);

  protected final MetaModel metaModel;
  protected final Repository repository;
  protected final GlobalIndex globalIndex;
  protected final LuceneIndex luceneIndex;
  protected final DefaultIdGenerator idGenerator = new DefaultIdGenerator();

  protected final Map<String, SessionEntry> entriesById = new HashMap<>();
  protected final Map<Object, SessionEntry> entriesByNaturalId = new HashMap<>();
  protected final Map<Object, SessionEntry> entity2Entry = new HashMap<>();

  protected final List<SessionAction> actions = new LinkedList<>();
  protected final DirtyChecker dirtyChecker;
  protected final Thread thread;
  protected final List<Index> indexes;

  public Session(MetaModel metaModel, Repository repository, GlobalIndex globalIndex, LuceneIndex luceneIndex) {
    this.metaModel = metaModel;
    this.repository = repository;
    this.globalIndex = globalIndex;
    this.luceneIndex = luceneIndex;
    dirtyChecker = new DirtyChecker(repository, metaModel);
    this.thread = Thread.currentThread();
    indexes = Arrays.asList(luceneIndex, globalIndex);
  }

  public Repository getRepository() {
    return repository;
  }

  public MetaModel getMetaModel() {
    return metaModel;
  }

  public void persist(Object entity) {
    Objects.requireNonNull(entity);

    EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());

    Path folder = entityDescriptor.getFolderGenerator().getFolder(repository, repository.getPath(), entity);
    String fileName = entityDescriptor.getFileGenerator().getFileName(repository, entityDescriptor, entity);

    persist(entity, entityDescriptor, folder, fileName);
  }

  protected void persist(Object entity, EntityDescriptor entityDescriptor, Path folder, String fileName) {
    Serializable naturalId = entityDescriptor.getNaturalId(entity);

    Path complete = folder.resolve(fileName);

    String id = idGenerator.getSha1Hash(repository.getPath(), complete);

    Optional<?> found = findById(id);
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

    persistRelations(entityDescriptor.getNormalRelations(), entity, sessionEntry);
    persistRelations(entityDescriptor.getChildRelations(), entity, sessionEntry);
  }

  protected void persistRelations(Collection<Relation> relations, Object parent, SessionEntry sessionEntry) {
    for (Relation relation : relations) {
      Collection<Object> relatedEntities = relation.getRelatedEntities(parent);

      persistSingleRelation(sessionEntry, relation, relatedEntities);
    }
  }

  private void persistSingleRelation(SessionEntry sessionEntry, Relation relation, Collection<Object> relatedEntities) {
    for (Object related : relatedEntities) {
      EntityDescriptor descriptor = metaModel.getEntityDescriptor(related.getClass());
      String relationId = descriptor.getId(related);
      if (relationId == null) {
        if (relation instanceof ChildRelation) {
          ChildRelation childRelation = (ChildRelation) relation;

          Path parentFolder = sessionEntry.getFolder();

          FileGenerator fileGenerator = childRelation.getFileGenerator();
          FolderGenerator folderGenerator = childRelation.getFolderGenerator();

          Path folder = folderGenerator.getFolder(repository, parentFolder, related);
          String fileName = fileGenerator.getFileName(repository, descriptor, related);

          persist(related, descriptor, folder, fileName);
        } else {
          persist(related);
        }
      }
    }
  }

  public void remove(Object entity) {
    Objects.requireNonNull(entity);
    SessionEntry sessionEntry = entity2Entry.get(entity);
    if (sessionEntry == null) {
      EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());
      String id = entityDescriptor.getId(entity);
      if (id != null) {
        IndexElement indexElement = globalIndex.getById(id);
        sessionEntry = loadSessionEntry(indexElement);
      }
    }
    if (sessionEntry != null) {
      dirtyChecker.trackDelete(sessionEntry);
      actions.add(new EntityDelete(repository, sessionEntry));
    }
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findByNaturalId(Class<E> clazz, Serializable naturalId) {
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
    return (Optional<E>) findById(id);
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findById(String id) {
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

  private SessionEntry loadSessionEntry(IndexElement indexElement) {
    Objects.requireNonNull(indexElement);
    EntityDescriptor descriptor = metaModel.getEntityDescriptor(indexElement.getEntityClass());
    HashMap<Relation, Collection<String>> relationIds = new HashMap<>();
    descriptor.getAllRelations().forEach(rel -> relationIds.put(rel, new ArrayList<>()));
    EntityPersister persister = descriptor.getPersister();
    Object object = persister.load(repository, descriptor, indexElement.getPathInRepository(), relationIds);
    SessionEntry sessionEntry = new SessionEntry(object, indexElement.getId(), descriptor.getVersion(object), indexElement.getNaturalId(), indexElement.getPathInRepository(), descriptor);
    addToSession(sessionEntry);

    for (Map.Entry<Relation, Collection<String>> entry : relationIds.entrySet()) {
      Relation relation = entry.getKey();
      Collection<String> ids = entry.getValue();
      if (relation.isLazy()) {
        relation.setupLazy(object, ids, this);
      } else {
        List<Object> relatedEntities = ids.stream().sequential().map(this::findById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        relation.setRelatedEntities(object, relatedEntities);
      }
    }
    return sessionEntry;
  }

  private Object load(IndexElement indexElement) {
    SessionEntry sessionEntry = loadSessionEntry(indexElement);
    EntityDescriptor descriptor = sessionEntry.getEntityDescriptor();
    Object object = sessionEntry.getObject();
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

  protected void removeFromSession(SessionEntry sessionEntry) {
    this.entriesById.remove(sessionEntry.getId());
    this.entriesByNaturalId.remove(sessionEntry.getNaturalId());
    this.entity2Entry.remove(sessionEntry.getObject());
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

  @Override
  public void prepare() {
    Collection<SessionEntry> dirty = dirtyChecker.findDirty(this.entriesById.values());
    dirty.stream().map(e -> new EntityUpdate(repository, e)).forEach(actions::add);
    actions.forEach(a -> a.prepare(this));
    indexes.forEach(Index::afterPrepare);
  }

  @Override
  public void commit() {
    indexes.forEach(Index::beforeCommit);
    actions.forEach(a -> a.commit(this));
    indexes.forEach(Index::afterCommit);
  }

  @Override
  public void rollback() {
    actions.forEach(a -> a.rollback(this));
    indexes.forEach(Index::afterRollback);
  }

  @Override
  public void close() {
    indexes.forEach(Index::close);
  }

  public void checkCorrectThread() {
    Thread currentThread = Thread.currentThread();
    if (!currentThread.equals(this.thread)) {
      throw new IllegalSessionThreadException("Trying to use session in thread " + currentThread + " but can only be used in " + thread);
    }
  }

  public <E> E lucene(LuceneReadFunction<E> read) {
    try (DirectoryReader reader = DirectoryReader.open(luceneIndex.getDirectory())) {
      IndexSearcher indexSearcher = new IndexSearcher(reader);
      return read.apply(indexSearcher);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  public interface LuceneReadFunction<R> {
    R apply(IndexSearcher searcher) throws IOException;
  }
}
