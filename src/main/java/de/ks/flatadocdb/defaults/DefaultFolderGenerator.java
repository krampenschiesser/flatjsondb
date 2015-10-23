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

package de.ks.flatadocdb.defaults;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.ifc.FolderGenerator;

import java.nio.file.Path;

public class DefaultFolderGenerator implements FolderGenerator {
  @Override
  public Path getFolder(Repository repository, Object object) {
    Path path = repository.getPath();
    Path resolve = path.resolve(object.getClass().getSimpleName());
    if (!resolve.toFile().exists()) {
      resolve.toFile().mkdir();
    } else if (!resolve.toFile().isDirectory()) {
      throw new IllegalStateException("File " + resolve + " needs to be a directory.");
    }
    return resolve;
  }
}
