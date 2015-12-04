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

package de.ks.flatadocdb.ifc;

import de.ks.flatadocdb.Repository;

import javax.annotation.Nullable;
import java.nio.file.Path;

@FunctionalInterface
public interface FolderGenerator {
  /**
   * Returns the folder used to store the content of the given object.
   * This folder needs to exist(be created by the implementation)
   *
   * If an entity is persisted as a child the ownerPath is given and needs to be taking into consideration by the implementation.
   *
   * @param repository
   * @param ownerPath  present if the given object is a child of another entity
   * @param object
   * @return existing path to a directory
   */
  Path getFolder(Repository repository, @Nullable Path ownerPath, Object object);

  /**
   * Implementations can define if the whole folder shalle be deleted with its entity.
   * This might be useful to cleanup related resources that are not known to the program.
   *
   * @return default false
   */
  default boolean isRemoveFolderOnDelete() {
    return false;
  }
}
