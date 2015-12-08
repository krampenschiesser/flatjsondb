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

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.SessionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * General interface class for indexes
 */
public abstract class Index {
  private static final Logger log = LoggerFactory.getLogger(Index.class);

  protected final Repository repository;
  protected final MetaModel metaModel;
  protected final ExecutorService executorService;

  public Index(Repository repository, MetaModel metaModel, ExecutorService executorService) {
    this.repository = repository;
    this.metaModel = metaModel;
    this.executorService = executorService;
  }

  public abstract void addEntry(SessionEntry entry);

  public abstract void removeEntry(SessionEntry entry);

  public abstract void updateEntry(SessionEntry entry);

  public abstract void recreate();

  public abstract void close();

  protected Map<EntityDescriptor, Set<Path>> mapToEntityDescriptors(Set<Path> allFiles) {
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

  protected Future<?> parseSingleFile(Map<EntityDescriptor, Set<Path>> discovered, List<EntityDescriptor> entities, Path file) {
    return executorService.submit(() -> {
      for (EntityDescriptor entityDescriptor : entities) {
        EntityPersister persister = entityDescriptor.getPersister();
        if (persister.canParse(file, entityDescriptor)) {
          discovered.get(entityDescriptor).add(file);
          log.debug("Found file {} which can be parsed as {}", file, entityDescriptor.getEntityClass().getSimpleName());
          return;
        }
      }
      log.debug("Could not find any entity descriptor for {}", file);
    });
  }

}
