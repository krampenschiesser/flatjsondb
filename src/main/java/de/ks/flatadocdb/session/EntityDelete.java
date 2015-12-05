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
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class EntityDelete extends SessionAction {
  private static final Logger log = LoggerFactory.getLogger(EntityDelete.class);

  public EntityDelete(Repository repository, SessionEntry sessionEntry) {
    super(repository, sessionEntry);
  }

  @Override
  public void prepare(Session session) {
    EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();

    boolean removeFolderOnDelete = entityDescriptor.getFolderGenerator().isRemoveFolderOnDelete();
    if (removeFolderOnDelete) {
      Path folder = sessionEntry.getFolder();
      try {
        String deletionTarget = folder.toFile().getName() + "_del";
        Files.move(folder, folder.getParent().resolve(deletionTarget), StandardCopyOption.ATOMIC_MOVE);
        log.debug("Moved folder scheduled for deletion from {} to {} for {}", sessionEntry.getFileName(), deletionTarget, sessionEntry.getObject());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      Path completePath = sessionEntry.getCompletePath();
      try {
        Files.move(completePath, completePath.getParent().resolve(completePath.toFile().getName() + "_del"), StandardCopyOption.ATOMIC_MOVE);
        log.debug("Moved file scheduled for deletion from {} to {} for {}", sessionEntry.getFileName(), completePath, sessionEntry.getObject());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void commit(Session session) {
    session.removeFromSession(sessionEntry);

    EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();
    boolean removeFolderOnDelete = entityDescriptor.getFolderGenerator().isRemoveFolderOnDelete();
    if (removeFolderOnDelete) {
      Path folder = sessionEntry.getFolder();
      try {
        Path deletionTarget = folder.getParent().resolve(folder.toFile().getName() + "_del");
        Files.delete(deletionTarget);
        log.debug("Deleted folder {}(originally {}) for {}", deletionTarget, sessionEntry.getFileName(), sessionEntry.getObject());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      Path completePath = sessionEntry.getCompletePath();
      try {
        Files.delete(completePath.getParent().resolve(completePath.toFile().getName() + "_del"));
        log.debug("Deleted folder {}(originally {}) for {}", completePath, sessionEntry.getFileName(), sessionEntry.getObject());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    session.globalIndex.removeEntry(sessionEntry);
    session.luceneIndex.removeEntry(sessionEntry);
  }

  @Override
  public void rollback(Session session) {
    EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();

    boolean removeFolderOnDelete = entityDescriptor.getFolderGenerator().isRemoveFolderOnDelete();
    if (removeFolderOnDelete) {
      Path folder = sessionEntry.getFolder();
      try {
        Path moveFolder = folder.getParent().resolve(folder.toFile().getName() + "_del");
        if (moveFolder.toFile().exists()) {
          Files.move(moveFolder, folder, StandardCopyOption.ATOMIC_MOVE);
          log.debug("Rolled back folder scheduled for deletion from {} to {} for {}", moveFolder, sessionEntry.getFileName(), sessionEntry.getObject());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      Path completePath = sessionEntry.getCompletePath();
      try {
        Path moveFile = completePath.getParent().resolve(completePath.toFile().getName() + "_del");
        if (moveFile.toFile().exists()) {
          Files.move(moveFile, completePath, StandardCopyOption.ATOMIC_MOVE);
          log.debug("Rolled back file scheduled for deletion from {} to {} for {}", moveFile, sessionEntry.getFileName(), sessionEntry.getObject());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
