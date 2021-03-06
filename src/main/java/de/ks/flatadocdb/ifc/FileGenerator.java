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

public interface FileGenerator {
  /**
   * Generates a filename used for the given object
   *
   * @param repository
   * @param descriptor
   * @param object
   * @return
   */
  String getFileName(Repository repository, EntityDescriptor descriptor, Object object);

  /**
   * Generates a filename used during the flush phase of the transaction.
   *
   * @param repository
   * @param descriptor
   * @param object
   * @return
   */

  String getFlushFileName(Repository repository, EntityDescriptor descriptor, Object object);
}
