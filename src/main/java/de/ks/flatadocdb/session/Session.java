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

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.DefaultIdGenerator;
import de.ks.flatadocdb.exception.NoIdField;
import de.ks.flatadocdb.exception.StaleObjectFileException;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.index.LocalIndex;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;

@NotThreadSafe//can only be used as ThreadLocal
public class Session {
  private static final Logger log = LoggerFactory.getLogger(Session.class);

  protected final MetaModel metaModel;
  protected final Repository repository;
  protected final LocalIndex localIndex;
  protected final DefaultIdGenerator idGenerator = new DefaultIdGenerator();

  protected final Map<String, SessionEntry> entriesById = new HashMap<>();
  protected final Map<Object, SessionEntry> entriesByNaturalId = new HashMap<>();
  protected final Map<Object, SessionEntry> entity2Entry = new HashMap<>();

  protected final LinkedList<Consumer<Session>> prepareActions = new LinkedList<>();
  protected final LinkedList<Consumer<Session>> commitActions = new LinkedList<>();

  public Session(MetaModel metaModel, Repository repository, LocalIndex localIndex) {
    this.metaModel = metaModel;
    this.repository = repository;
    this.localIndex = localIndex;
  }

  public void persist(Object entity) {
    Objects.requireNonNull(entity);

    EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(entity.getClass());
    Object naturalId = entityDescriptor.getNaturalId(entity);

    Path folder = entityDescriptor.getFolderGenerator().getFolder(repository, entity);
    String fileName = entityDescriptor.getFileGenerator().getFileName(repository, entityDescriptor, entity);
    String flushFile = entityDescriptor.getFileGenerator().getFlushFileName(repository, entityDescriptor, entity);

    Path flushComplete = folder.resolve(flushFile);
    Path complete = folder.resolve(fileName);

    String id = idGenerator.getSha1Hash(repository.getPath(), complete);
    entityDescriptor.writetId(entity, id);

    SessionEntry sessionEntry = new SessionEntry(entity, id, 0, complete);
    entriesById.put(id, sessionEntry);
    if (naturalId != null) {
      entriesByNaturalId.put(naturalId, sessionEntry);
    }
    prepareActions.add(s -> {
      checkNoFlushFileExists(folder, fileName);
      entityDescriptor.getPersister().save(entityDescriptor, flushComplete, entity);
      applyWindowsHiddenAttribute(flushComplete);
      checkAppendToComplete(complete);
    });
    commitActions.add(s -> {
      try {
        Files.move(flushComplete, complete, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected void checkNoFlushFileExists(Path flushComplete, String fileName) {
    File[] files = flushComplete.toFile().listFiles(f -> f.isFile() && f.getName().startsWith("." + fileName));
    if (files.length != 0) {
      throw new StaleObjectFileException("Flush file already exists" + flushComplete);
    }
  }

  protected void checkAppendToComplete(Path complete) {
    if (complete.toFile().exists()) {
      try {
        Files.write(complete, Arrays.asList(""), StandardOpenOption.APPEND);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected void applyWindowsHiddenAttribute(Path flushComplete) {
    if (StandardSystemProperty.OS_NAME.value().toLowerCase(Locale.ROOT).contains("win")) {
      try {
        Files.setAttribute(flushComplete, "dos:hidden", true);
      } catch (IOException e) {
        log.warn("Cannot set hidden attribute on {}", flushComplete, e);
      }
    }
  }

  public void remove(Object entity) {
    if (entity != null) {

    }
  }

  @SuppressWarnings("unchecked")
  public <E> Optional<E> findByNaturalId(Class<E> clazz, Object naturalId) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(naturalId);

    SessionEntry sessionEntry = entriesByNaturalId.get(naturalId);
    if (sessionEntry == null) {
      IndexElement indexElement = localIndex.getByNaturalId(naturalId);
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
      IndexElement indexElement = localIndex.getById(id);
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
    Object load = persister.load(descriptor);
//    SessionEntry sessionEntry = new SessionEntry();
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

  protected void prepare() {
    prepareActions.forEach(a -> a.accept(this));
  }

  protected void commit() {
    commitActions.forEach(a -> a.accept(this));
  }
}
