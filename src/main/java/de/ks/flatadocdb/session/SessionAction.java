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
import de.ks.flatadocdb.exception.StaleObjectFileException;
import de.ks.flatadocdb.exception.StaleObjectStateException;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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

  public abstract void rollback(Session session);

  protected void addFileDeleteRollback(Path path) {
    rollbacks.add(() -> {
      try {
        Files.delete(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected void checkVersionIncrement(Path completePath, long version) {
    if (completePath.toFile().exists()) {
      EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();
      Object load = entityDescriptor.getPersister().load(repository, entityDescriptor, completePath);
      long currentVersion = entityDescriptor.getVersion(load);
      if (currentVersion >= version) {
        throw new StaleObjectStateException("Entity version changed, file=" + currentVersion + ", session=" + version + ". Path:" + completePath);
      }
    }
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

  protected void moveFlushFile(Path flushPath) {
    try {
      Files.move(flushPath, sessionEntry.getCompletePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeFlushFile(byte[] fileContents) {
    Path folder = sessionEntry.getFolder();
    String flushFileName = sessionEntry.getEntityDescriptor().getFileGenerator().getFlushFileName(repository, sessionEntry.getEntityDescriptor(), sessionEntry.getObject());
    Path flushPath = folder.resolve(flushFileName);

    try {
      Files.write(flushPath, fileContents);
      addFileDeleteRollback(flushPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    applyWindowsHiddenAttribute(flushPath);
  }

}
