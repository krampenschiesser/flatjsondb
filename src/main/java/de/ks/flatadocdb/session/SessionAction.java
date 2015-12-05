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
import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.exception.AggregateException;
import de.ks.flatadocdb.exception.StaleObjectFileException;
import de.ks.flatadocdb.exception.StaleObjectStateException;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Base class for events that happen in a session.
 */
public abstract class SessionAction {
  private static final Logger log = LoggerFactory.getLogger(SessionAction.class);

  protected final Repository repository;
  protected final SessionEntry sessionEntry;
  private final List<Runnable> rollbacks = new LinkedList<>();

  public SessionAction(Repository repository, SessionEntry sessionEntry) {
    this.repository = repository;
    this.sessionEntry = sessionEntry;
  }

  public abstract void prepare(Session session);

  public abstract void commit(Session session);

  public void rollback(Session session) {
    ArrayList<Throwable> exceptions = new ArrayList<>();

    for (Runnable rollback : rollbacks) {
      try {
        log.debug("Rolling back for {}", sessionEntry);
        rollback.run();
      } catch (Exception e) {
        log.error("Got exception during rollback: ", e);
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw new AggregateException(exceptions);
    }
  }

  protected void addFileDeleteRollback(Path path) {
    rollbacks.add(() -> {
      try {
        Files.delete(path);
        log.debug("Deleting {} because of rollback for {}", path, sessionEntry);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected void checkVersionIncrement(Path completePath, long version) {
    if (completePath.toFile().exists()) {
      EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();
      Object load = entityDescriptor.getPersister().load(repository, entityDescriptor, completePath, new HashMap<>());
      long currentVersion = entityDescriptor.getVersion(load);
      log.trace("Got version {} from {}, session version={}. ({})", currentVersion, completePath, version, sessionEntry);
      if (currentVersion > version) {
        throw new StaleObjectStateException("Entity version changed, file=" + currentVersion + ", session=" + version + ". Path:" + completePath);
      }
    }
  }

  protected void checkNoFlushFileExists(Path flushPath) {
    if (Files.exists(flushPath)) {
      throw new StaleObjectFileException("Flush file already exists" + getFlushPath());
    }
  }

  protected void checkAppendToComplete(Path complete) {
    if (complete.toFile().exists()) {
      try {
        Files.write(complete, Collections.singleton(""), StandardOpenOption.APPEND);
        log.trace("Could append to {}", complete);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected void applyWindowsHiddenAttribute(Path flushComplete) {
    if (StandardSystemProperty.OS_NAME.value().toLowerCase(Locale.ROOT).contains("win")) {
      try {
        Files.setAttribute(flushComplete, "dos:hidden", true);
        log.trace("Hiding flush file {}", flushComplete);
      } catch (IOException e) {
        log.warn("Cannot set hidden attribute on {}", flushComplete, e);
      }
    }
  }

  protected void moveFlushFile(Path flushPath) {
    try {
      Files.move(flushPath, sessionEntry.getCompletePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      log.trace("Moved flush {} file to real file for {}", flushPath.getFileName(), sessionEntry);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeFlushFile(byte[] fileContents) {
    Path flushPath = getFlushPath();
    try {
      Files.write(flushPath, fileContents);
      log.trace("Wrote contents of {} to flush file {}", sessionEntry, flushPath);
      addFileDeleteRollback(flushPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    applyWindowsHiddenAttribute(flushPath);
  }

  protected Path getFlushPath() {
    Path folder = sessionEntry.getFolder();
    String flushFileName = sessionEntry.getEntityDescriptor().getFileGenerator().getFlushFileName(repository, sessionEntry.getEntityDescriptor(), sessionEntry.getObject());
    return folder.resolve(flushFileName);
  }

  protected void executeLifecycleAction(LifeCycle lifeCycle) {
    Object entity = sessionEntry.getObject();
    EntityDescriptor descriptor = sessionEntry.getEntityDescriptor();

    Set<MethodHandle> lifeCycleMethods = descriptor.getLifeCycleMethods(lifeCycle);
    for (MethodHandle handle : lifeCycleMethods) {
      try {
        log.trace("Invoking lifecycle method {} for {}", handle, sessionEntry);
        handle.invoke(entity);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }
}
