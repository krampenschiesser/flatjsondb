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
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.Path;

/**
 * A persister is local for each entity and can store stateful information.
 * It has to be threadsafe as it is used and accessed by all modifying/reading jvm threads.
 */
@ThreadSafe
public interface EntityPersister {
  default void initialize(Repository repository, MetaModel metaModel) {
    //
  }

  Object load(Repository repository, EntityDescriptor descriptor, Path path);

  /**
   * Generates the file contents for the given object
   *
   * @param repository
   * @param descriptor
   * @param object
   * @return
   */
  byte[] createFileContents(Repository repository, EntityDescriptor descriptor, Object object);

  /**
   * Will be used to check if this file can be handled by this entity persister.
   * This is needed in order to rebuild the index.
   *
   * @param path
   * @param descriptor
   * @return true/false
   */
  boolean canParse(Path path, EntityDescriptor descriptor);
}
