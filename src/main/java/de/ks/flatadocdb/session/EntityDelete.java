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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class EntityDelete extends SessionAction {
  private static final Logger log = LoggerFactory.getLogger(EntityDelete.class);
  public static final String DELETION_SUFFIX = "_del";

  public EntityDelete(Repository repository, SessionEntry sessionEntry) {
    super(repository, sessionEntry);
  }

  @Override
  public void prepare(Session session) {
    executeLifecycleAction(LifeCycle.POST_REMOVE);
    Path completePath = sessionEntry.getCompletePath();
    try {
      Files.move(completePath, completePath.getParent().resolve(completePath.toFile().getName() + DELETION_SUFFIX), StandardCopyOption.ATOMIC_MOVE);
      log.debug("Moved file scheduled for deletion from {} to {} for {}", sessionEntry.getFileName(), completePath, sessionEntry.getObject());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void commit(Session session) {
    session.removeFromSession(sessionEntry);

    Path completePath = sessionEntry.getCompletePath();
    try {
      Files.delete(completePath.getParent().resolve(completePath.toFile().getName() + DELETION_SUFFIX));
      log.debug("Deleted folder {}(originally {}) for {}", completePath, sessionEntry.getFileName(), sessionEntry.getObject());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    session.globalIndex.removeEntry(sessionEntry);
    session.luceneUpdates.add(index -> index.removeEntry(sessionEntry));
    executeLifecycleAction(LifeCycle.POST_REMOVE);
    removeEmptyFolders(completePath);
  }

  private void removeEmptyFolders(Path completePath) {
    for (Path parent = completePath.getParent(); !repository.getPath().equals(parent); parent = parent.getParent()) {
      try {
        if (Files.exists(parent)) {
          Stream<Path> list = Files.list(parent);
          long count = list.count();
          if (count == 0) {
            Files.deleteIfExists(parent);
          } else {
            break;
          }
        }
      } catch (IOException e) {
        log.error("Could not remove empty dir {}", completePath.getParent(), e);
      }
    }
  }

  @Override
  public void rollback(Session session) {
    Path completePath = sessionEntry.getCompletePath();
    try {
      Path moveFile = completePath.getParent().resolve(completePath.toFile().getName() + DELETION_SUFFIX);
      if (moveFile.toFile().exists()) {
        Files.move(moveFile, completePath, StandardCopyOption.ATOMIC_MOVE);
        log.debug("Rolled back file scheduled for deletion from {} to {} for {}", moveFile, sessionEntry.getFileName(), sessionEntry.getObject());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
