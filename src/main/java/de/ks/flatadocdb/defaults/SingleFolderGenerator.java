/**
 * Copyright [2015] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ks.flatadocdb.defaults;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;

public class SingleFolderGenerator extends JoinedRootFolderGenerator {
  private static final Logger log = LoggerFactory.getLogger(SingleFolderGenerator.class);

  @Override
  public Path getFolder(Repository repository, @Nullable Path ownerPath, Object object) {
    Path rootFolder = super.getFolder(repository, ownerPath, object);

    EntityDescriptor descriptor = repository.getMetaModel().getEntityDescriptor(object.getClass());
    Object naturalId = descriptor.getNaturalId(object);
    if (naturalId != null) {
      String naturalIdString = NameStripper.stripName(String.valueOf(naturalId));
      String retval = naturalIdString;
      Path targetFolder = rootFolder.resolve(retval);
      targetFolder.toFile().mkdir();
      log.trace("Using folder \"{}\" via natural id for {}", targetFolder, object);
      return targetFolder;
    } else {
      String hexString = parseHashCode(object);
      String retval = hexString;
      Path targetFolder = rootFolder.resolve(retval);
      targetFolder.toFile().mkdir();
      log.trace("Using folder \"{}\" via hashcode for {}", targetFolder, object);
      return targetFolder;
    }
  }

  protected String parseHashCode(Object object) {
    int hashCode = Objects.hashCode(object);
    byte[] array = new byte[]{(byte) (hashCode >>> 24), (byte) (hashCode >>> 16), (byte) (hashCode >>> 8), (byte) hashCode};
    return Hex.encodeHexString(array);
  }
}
