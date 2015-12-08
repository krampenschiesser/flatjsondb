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

package de.ks.flatadocdb.index;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.DefaultIdGenerator;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.SessionEntry;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Stores the indexed contents of a single repository:
 *
 * * Id
 * * natural id
 * * relative path in repo
 * * Entity class
 * * md5sum (for rebuild checking)
 * * last modified(for rebuild checking)
 */
public class GlobalIndex extends Index {
  public static final String INDEX_FOLDER = ".index";
  public static final String INDEX_FILE = "index.json";
  private static final Logger log = LoggerFactory.getLogger(GlobalIndex.class);

  protected final Map<Serializable, IndexElement> naturalIdToElement = new ConcurrentHashMap<>();
  protected final Map<String, IndexElement> idToElement = new ConcurrentHashMap<>();


  public GlobalIndex(Repository repository, MetaModel metaModel) {
    this(repository, metaModel, Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build()));
  }

  public GlobalIndex(Repository repository, MetaModel metaModel, ExecutorService executorService) {
    super(repository, metaModel, executorService);
  }

  @Override
  public void addEntry(SessionEntry sessionEntry) {
    IndexElement element = new IndexElement(repository, sessionEntry.getCompletePath(), sessionEntry.getId(), sessionEntry.getNaturalId(), sessionEntry.getObject().getClass());
    idToElement.put(element.getId(), element);
    if (element.hasNaturalId()) {
      naturalIdToElement.put(element.getNaturalId(), element);
    }
  }

  @Override
  public void updateEntry(SessionEntry sessionEntry) {
    removeEntry(sessionEntry);
    addEntry(sessionEntry);
  }

  @Override
  public void removeEntry(SessionEntry sessionEntry) {
    IndexElement element = new IndexElement(repository, sessionEntry.getCompletePath(), sessionEntry.getId(), sessionEntry.getNaturalId(), sessionEntry.getObject().getClass());
    idToElement.remove(element.getId());
    if (element.hasNaturalId()) {
      naturalIdToElement.remove(element.getNaturalId());
    }
  }

  public IndexElement getById(String id) {
    return idToElement.get(id);
  }

  public IndexElement getByNaturalId(Serializable id) {
    return naturalIdToElement.get(id);
  }

  public Collection<IndexElement> getAllOf(Class<?> entity) {
    return idToElement.values().stream().filter(v -> v.getEntityClass().equals(entity)).collect(Collectors.toSet());
  }

  public Collection<String> getAllIds() {
    return Collections.unmodifiableCollection(idToElement.keySet());
  }

  @Override
  public void recreate() {
    Set<Path> allFiles = repository.getAllFilesInRepository();

    Map<EntityDescriptor, Set<Path>> discovered = mapToEntityDescriptors(allFiles);
    if (log.isDebugEnabled()) {
      discovered.entrySet().forEach(e -> log.debug("For class {} found {} elements", e.getKey().getEntityClass().getSimpleName(), e.getValue().size()));
    }

    DefaultIdGenerator idGenerator = new DefaultIdGenerator();

    List<Future<IndexElement>> futures = discovered.entrySet().stream().map(entry -> {
      EntityDescriptor descriptor = entry.getKey();
      Set<Path> paths = entry.getValue();
      ArrayList<Future<IndexElement>> retval = new ArrayList<>();
      for (Path path : paths) {
        Future<IndexElement> future = executorService.submit(() -> {
          String id = idGenerator.getSha1Hash(repository.getPath(), path);
          byte[] md5 = readMd5(path);
          long lastModified = getLastModified(path);
          Object loaded = descriptor.getPersister().load(repository, descriptor, path, new HashMap<>());
          Serializable naturalId = descriptor.getNaturalId(loaded);
          IndexElement indexElement = new IndexElement(repository, path, id, naturalId, descriptor.getEntityClass());
          indexElement.setMd5Sum(md5).setLastModified(lastModified);
          log.trace("Created index element {}", indexElement);
          return indexElement;
        });
        retval.add(future);
      }
      return retval;
    }).reduce(new ArrayList<>(), (l1, l2) -> {
      l1.addAll(l2);
      return l1;
    });

    for (Future<IndexElement> future : futures) {
      try {
        IndexElement indexElement = future.get();
        this.idToElement.put(indexElement.getId(), indexElement);
        this.naturalIdToElement.put(indexElement.getNaturalId(), indexElement);
      } catch (Exception e) {
        log.error("Could not retrieve index element", e);
      }

    }
  }

  protected long getLastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      log.error("Could not get last modification time from {}", path, e);
      return 0;
    }
  }

  protected byte[] readMd5(Path path) {
    byte[] md5 = new byte[0];
    try (FileInputStream stream = new FileInputStream(path.toFile())) {
      try (BufferedInputStream buffered = new BufferedInputStream(stream)) {
        md5 = DigestUtils.md5(buffered);

      }
    } catch (IOException e) {
      log.error("Could not get md5 of {}", path, e);
    }
    return md5;
  }

  @Override
  public void close() {

  }

  public void flush() {
    Path folder = repository.getPath().resolve(INDEX_FOLDER);
    if (!Files.exists(folder)) {
      try {
        Files.createDirectories(folder);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    final ObjectMapper mapper = getMapper();
    ArrayList<IndexElement> elements = new ArrayList<>(idToElement.values());
    try {
      mapper.writeValue(repository.getPath().resolve(INDEX_FOLDER).resolve(INDEX_FILE).toFile(), elements);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void load() {
    final ObjectMapper mapper = getMapper();

    Path filePath = repository.getPath().resolve(INDEX_FOLDER).resolve(INDEX_FILE);
    if (Files.exists(filePath)) {
      try {
        @SuppressWarnings("unchecked")
        ArrayList<IndexElement> values = mapper.readValue(filePath.toFile(), ArrayList.class);
        for (IndexElement element : values) {
          element.setRepository(repository);
          String id = element.getId();
          Serializable naturalId = element.getNaturalId();
          idToElement.put(id, element);
          naturalIdToElement.put(naturalId, element);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected ObjectMapper getMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.enableDefaultTyping();
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
    return mapper;
  }
}
