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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.DefaultIdGenerator;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
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

public class GlobalIndex {
  private static final Logger log = LoggerFactory.getLogger(GlobalIndex.class);

  protected Map<Serializable, IndexElement> naturalIdToElement = new ConcurrentHashMap<>();
  protected Map<String, IndexElement> idToElement = new ConcurrentHashMap<>();

  protected final Repository repository;
  protected final MetaModel metaModel;
  protected final ExecutorService executorService;

  public GlobalIndex(Repository repository, MetaModel metaModel) {
    this(repository, metaModel, Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build()));
  }

  public GlobalIndex(Repository repository, MetaModel metaModel, ExecutorService executorService) {
    this.repository = repository;
    this.metaModel = metaModel;
    this.executorService = executorService;
  }

  public void addEntry(IndexElement element) {
    idToElement.put(element.getId(), element);
    if (element.hasNaturalId()) {
      naturalIdToElement.put(element.getNaturalId(), element);
    }
  }

  public void removeEntry(IndexElement element) {
    idToElement.remove(element.getId());
    if (element.hasNaturalId()) {
      naturalIdToElement.remove(element.getNaturalId());
    }
  }

  public IndexElement getById(String id) {
    return idToElement.get(id);
  }

  public IndexElement getByNaturalId(Object id) {
    return naturalIdToElement.get(id);
  }

  public Collection<IndexElement> getAllOf(Class<?> entity) {
    return idToElement.values().stream().filter(v -> v.getEntityClass().equals(entity)).collect(Collectors.toSet());
  }

  public void recreate() {
    Set<Path> allFiles = repository.getAllFilesInRepository();

    Map<EntityDescriptor, Set<Path>> discovered = mapToEntityDescriptors(allFiles);

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
          Object loaded = descriptor.getPersister().load(repository, descriptor, path);
          Serializable naturalId = descriptor.getNaturalId(loaded);
          IndexElement indexElement = new IndexElement(repository, path, id, naturalId, descriptor.getEntityClass());
          indexElement.setMd5Sum(md5).setLastModified(lastModified);
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

  private long getLastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      log.error("Could not get last modification time from {}", path, e);
      return 0;
    }
  }

  private byte[] readMd5(Path path) {
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

  private Map<EntityDescriptor, Set<Path>> mapToEntityDescriptors(Set<Path> allFiles) {
    Map<EntityDescriptor, Set<Path>> discovered = new ConcurrentHashMap<>();
    List<EntityDescriptor> entities = metaModel.getEntities();
    entities.forEach(e -> discovered.put(e, new HashSet<>()));

    Map<Path, Future<?>> futures = new LinkedHashMap<>();
    for (Path file : allFiles) {
      futures.put(file, parseSingleFile(discovered, entities, file));
    }

    futures.forEach((p, f) -> {
      try {
        f.get();
      } catch (Exception e) {
        log.error("Could not  determine responsible entity descriptor for {}", p, e);
      }
    });
    return discovered;
  }

  private Future<?> parseSingleFile(Map<EntityDescriptor, Set<Path>> discovered, List<EntityDescriptor> entities, Path file) {
    return executorService.submit(() -> {
      for (EntityDescriptor entityDescriptor : entities) {
        EntityPersister persister = entityDescriptor.getPersister();
        if (persister.canParse(file, entityDescriptor)) {
          discovered.get(entityDescriptor).add(file);
          return;
        }
      }
      log.debug("Could not find any entity descriptor for {}", file);
    });
  }
}
