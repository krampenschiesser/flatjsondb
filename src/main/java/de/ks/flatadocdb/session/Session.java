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
import de.ks.flatadocdb.query.Query;
import de.ks.flatadocdb.session.dirtycheck.DirtyChecker;
import de.ks.flatadocdb.session.relation.LazyEntity;
import de.ks.flatadocdb.session.transaction.local.TransactionResource;
import de.ks.flatadocdb.util.TimeProfiler;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
  protected final Map<NaturalId, SessionEntry> entriesByNaturalId = new HashMap<>();
  protected final Map<Object, SessionEntry> entity2Entry = new IdentityHashMap<>();

  protected final List<SessionAction> actions = new LinkedList<>();
  protected final List<Consumer<LuceneIndex>> luceneUpdates = new LinkedList<>();
  protected final DirtyChecker dirtyChecker;
  protected final Thread thread;
  protected final List<Index> indexes;

  protected boolean rollbackonly = false;

  public Session(MetaModel metaModel, Repository repository) {
    this.metaModel = metaModel;
    this.repository = repository;
    this.globalIndex = repository.getIndex();
    this.luceneIndex = repository.getLuceneIndex();
    dirtyChecker = new DirtyChecker(repository, metaModel);
    this.thread = Thread.currentThread();
    indexes = Arrays.asList(this.luceneIndex, globalIndex);
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

    Object found = findById(id);
    if ((found != null || entity2Entry.containsKey(entity)) && !dirtyChecker.getDeletions().contains(entity)) {
      log.warn("Trying to persist entity {} [{}] twice", entity, complete);
      return;
    }

    entityDescriptor.writetId(entity, id);

    SessionEntry sessionEntry = new SessionEntry(entity, id, 0, naturalId == null ? null : new NaturalId(entityDescriptor.getEntityClass(), naturalId), complete, entityDescriptor);
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
      if (relationId == null || dirtyChecker.getDeletions().contains(related)) {
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
    removeSessionEntry(sessionEntry, entity, Collections.emptySet());
  }

  protected void removeSessionEntry(SessionEntry sessionEntry, Object entity, Set<Object> ignoredRelations) {
    EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());

    for (Relation relation : entityDescriptor.getChildRelations()) {
      Collection<Object> relatedEntities = relation.getRelatedEntities(entity);
      for (Object relatedEntity : relatedEntities) {
        relatedEntity = LazyEntity.getRealObject(relatedEntity);
        if (!ignoredRelations.contains(relatedEntity)) {
          EntityDescriptor relatedDescriptor = metaModel.getEntityDescriptor(relatedEntity.getClass());
          remove(relatedEntity);
          relatedDescriptor.writetId(relatedEntity, null);
        }
      }
    }
    if (sessionEntry == null) {
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
  public <E> E findByNaturalId(Class<E> clazz, Serializable naturalId) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(naturalId);

    NaturalId realNaturalId = new NaturalId(clazz, naturalId);
    SessionEntry sessionEntry = entriesByNaturalId.get(realNaturalId);
    if (sessionEntry == null) {
      IndexElement indexElement = globalIndex.getByNaturalId(realNaturalId);
      if (indexElement == null) {
        return null;
      } else {
        return (E) load(indexElement);
      }

    } else {
      return (E) sessionEntry.object;
    }
  }

  @SuppressWarnings("unchecked")
  public <E> E findById(Class<E> clazz, String id) {
    return (E) findById(id);
  }

  @SuppressWarnings("unchecked")
  public <E> E findById(String id) {
    Objects.requireNonNull(id);

    SessionEntry sessionEntry = entriesById.get(id);
    if (sessionEntry == null) {
      IndexElement indexElement = globalIndex.getById(id);
      if (indexElement == null) {
        return null;
      } else {
        return (E) load(indexElement);
      }
    } else {
      return (E) sessionEntry.object;
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
    descriptor.writePathInRepo(object, indexElement.getPathInRepository());

    byte[] md5Sum = indexElement.getMd5Sum();
    sessionEntry.setMd5(md5Sum);
    if (md5Sum == null) {
      try (FileInputStream stream = new FileInputStream(indexElement.getPathInRepository().toFile())) {
        sessionEntry.setMd5(DigestUtils.md5(stream));
      } catch (IOException e) {
        log.error("Could not get md5sum from {}", indexElement.getPathInRepository(), e);
      }
    }

    Path rootFolder = descriptor.getFolderGenerator().getFolder(repository, repository.getPath(), object);
    boolean isChild = !rootFolder.equals(sessionEntry.getCompletePath().getParent());
    sessionEntry.setChild(isChild);

    log.trace("Loaded {}", object);
    addToSession(sessionEntry);

    for (Map.Entry<Relation, Collection<String>> entry : relationIds.entrySet()) {
      Relation relation = entry.getKey();
      Collection<String> ids = entry.getValue();
      if (relation.isLazy()) {
        relation.setupLazy(object, ids, this);
      } else {
        List<Object> relatedEntities = ids.stream().sequential().map(this::findById).filter(o -> o != null).collect(Collectors.toList());
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
    Set<SessionEntry> renamed = handleRenames();

    Collection<SessionEntry> dirty = dirtyChecker.findDirty(this.entriesById.values());
    dirty.removeAll(renamed);
    dirty.forEach(e -> {
      e.getEntityDescriptor().getChildRelations().stream()//
        .flatMap(r -> r.getRelatedEntities(e.getObject()).stream())//
        .filter(o -> metaModel.getEntityDescriptor(o.getClass()).getId(o) == null)//
        .filter(o -> !entity2Entry.containsKey(o))//
        .forEach(this::persist);
    });
    dirty.stream().map(e -> new EntityUpdate(repository, e)).forEach(actions::add);

    for (SessionAction action : this.actions) {
      try {
        action.prepare(this);
      } catch (RuntimeException e) {
        rollbackonly = true;
        throw e;
      }
    }
  }

  private Set<SessionEntry> handleRenames() {
    Set<SessionEntry> renamed = entriesById.values().stream()//
      .filter(e -> !e.isChild())//
      .filter(e -> {
        EntityDescriptor entityDescriptor = e.getEntityDescriptor();
        String newFileName = entityDescriptor.getFileGenerator().getFileName(repository, entityDescriptor, e.object);
        return !newFileName.equals(e.getFileName());
      }).collect(Collectors.toSet());

    HashSet<Object> processed = new HashSet<>();
    for (SessionEntry sessionEntry : renamed) {
      boolean alreadyProcessed = processed.contains(sessionEntry.getObject());
      if (!alreadyProcessed) {
        removeSessionEntry(sessionEntry, sessionEntry.getObject(), processed);
        persist(sessionEntry.getObject());

        Set<Relation> allRelations = sessionEntry.getEntityDescriptor().getChildRelations();
        for (Relation child : allRelations) {
          Collection<Object> related = child.getRelatedEntities(sessionEntry.getObject());
          processed.addAll(related);
        }
        processed.add(sessionEntry.getObject());
      }
    }
    return renamed;
  }

  @Override
  public void commit() {
    if (isRollbackonly()) {
      return;
    }
    for (SessionAction action : actions) {
      try {
        action.commit(this);
      } catch (RuntimeException e) {
        rollbackonly = true;
        throw e;
      }
    }

    if (isRollbackonly()) {
      return;
    }
    TimeProfiler profiler = new TimeProfiler("Lucene update").start();
    luceneUpdates.forEach(u -> u.accept(luceneIndex));
    profiler.stop().logDebug(log);

    if (isRollbackonly()) {
      return;
    }
  }

  @Override
  public void rollback() {
    actions.forEach(a -> a.rollback(this));
    actions.clear();
    luceneUpdates.clear();
  }

  public void checkCorrectThread() {
    Thread currentThread = Thread.currentThread();
    if (!currentThread.equals(this.thread)) {
      throw new IllegalSessionThreadException("Trying to use session in thread " + currentThread + " but can only be used in " + thread);
    }
  }

  public <E> E lucene(LuceneReadFunction<E> read) {
    IndexReader reader = luceneIndex.getIndexReader();
    try {
      IndexSearcher indexSearcher = new IndexSearcher(reader);
      return read.apply(indexSearcher);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <R, E, V> Set<V> queryValues(Class<R> resultClass, Query<E, V> query, Predicate<V> filter) {
    Set<V> filteredFromSession = this.entriesById.values().stream()//
      .filter(entry -> resultClass.isAssignableFrom(entry.getObject().getClass()))//
      .map(entry -> query.getValue((E) entry.getObject()))//
      .filter(Objects::nonNull)//
      .filter(filter::test)//
      .collect(Collectors.toSet());

    Set<V> fromIndex = queryValuesFromIndex(query, filter, entriesById.keySet());

    HashSet<V> retval = new HashSet<>(fromIndex);
    retval.addAll(filteredFromSession);
    return retval;
  }

  private <E, V> Set<V> queryValuesFromIndex(Query<E, V> query, Predicate<V> filter, Set<String> idsToIgnore) {
    Map<IndexElement, Optional<V>> elements = globalIndex.getQueryElements(query);
    if (elements == null) {
      return Collections.emptySet();
    } else {
      return elements.entrySet().stream()//
        .filter(entry -> !idsToIgnore.contains(entry.getKey().getId()))//
        .filter(entry -> entry.getValue().isPresent())//
        .filter(entry -> filter.test(entry.getValue().get()))//
        .map(entry -> entry.getValue().get())//
        .collect(Collectors.toSet());
    }
  }

  @SuppressWarnings("unchecked")
  public <R, E, V> Collection<R> query(Class<R> resultClass, Query<E, V> query, Predicate<V> filter) {
    MultiQueyBuilder<R> builder = multiQuery(resultClass);
    return builder.query(query, filter).find();
  }

  public <E> MultiQueyBuilder<E> multiQuery(Class<E> resultClass) {
    return new MultiQueyBuilder<>(this, resultClass);
  }

  @FunctionalInterface
  public interface LuceneReadFunction<R> {
    R apply(IndexSearcher searcher) throws IOException;
  }

  private void checkRollbackOnly() {
    if (rollbackonly) {
      throw new IllegalStateException("Session marked as rollback only");
    }
  }

  public boolean isRollbackonly() {
    return rollbackonly;
  }

  public static class MultiQueyBuilder<E> {
    private final Session session;
    private final Class<E> resultClass;
    protected LinkedHashMap<Query<E, Object>, Predicate<Object>> queries = new LinkedHashMap<>();

    public MultiQueyBuilder(Session session, Class<E> resultClass) {
      this.session = session;
      this.resultClass = resultClass;
    }

    @SuppressWarnings("unchecked")
    public <V> MultiQueyBuilder<E> query(Query<?, V> query, Predicate<V> filter) {
      boolean isSubtype = resultClass.isAssignableFrom(query.getOwnerClass());
      boolean isSuperType = ReflectionUtils.getAllSuperTypes(resultClass).contains(query.getOwnerClass());

      if (isSubtype || isSuperType) {
        Query<E, Object> cast = (Query<E, Object>) query;
        queries.put(cast, (Predicate<Object>) filter);
        return this;
      } else {
        throw new IllegalArgumentException("Given query class " + query.getOwnerClass() + " and expected result class " + resultClass + " are totally unrelated.");
      }
    }

    public Set<E> find() {
      HashMap<String, Map<Query<E, Object>, Object>> indexElementMapHashMap = new HashMap<>();
      queries.keySet().forEach(query -> {
        Map<IndexElement, ? extends Optional<?>> queryElements = session.globalIndex.getQueryElements(query);

        queryElements.entrySet().stream()//
          .filter(e -> !session.entriesById.keySet().contains(e.getKey().getId()))//
          .filter(e -> resultClass.isAssignableFrom(e.getKey().getEntityClass()))//
          .forEach(entry -> {
            Map<Query<E, Object>, Object> map = indexElementMapHashMap.compute(entry.getKey().getId(), (indexElement, queryObjectMap) -> queryObjectMap == null ? new HashMap<Query<E, Object>, Object>() : queryObjectMap);
            map.put(query, entry.getValue().orElse(null));
          });

        session.entriesById.values().stream()//
          .filter(v -> resultClass.isAssignableFrom(v.getEntityDescriptor().getEntityClass()))//
          .forEach(v -> {
            String id = v.getId();
            Map<Query<E, Object>, Object> map = indexElementMapHashMap.compute(id, (indexElement, queryObjectMap) -> queryObjectMap == null ? new HashMap<Query<E, Object>, Object>() : queryObjectMap);

            @SuppressWarnings("unchecked")
            Object value = query.getValue((E) v.getObject());
            map.put(query, value);
          });
      });

      HashSet<String> results = new HashSet<>();
      for (Map.Entry<String, Map<Query<E, Object>, Object>> entry : indexElementMapHashMap.entrySet()) {
        boolean include = true;
        Map<Query<E, Object>, Object> value = entry.getValue();
        for (Map.Entry<Query<E, Object>, Object> queryObjectEntry : value.entrySet()) {
          Predicate<Object> predicate = queries.get(queryObjectEntry.getKey());
          include = predicate.test(queryObjectEntry.getValue());
          if (!include) {
            break;
          }
        }
        if (include) {
          results.add(entry.getKey());
        }
      }
      return results.stream().map(session::findById)//
        .map(o -> (E) o)//
        .collect(Collectors.toSet());
    }
  }
}
