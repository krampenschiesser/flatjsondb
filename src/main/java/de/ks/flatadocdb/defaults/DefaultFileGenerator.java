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
import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DefaultFileGenerator implements FileGenerator {
  private static final Logger log = LoggerFactory.getLogger(DefaultFileGenerator.class);

  //  public final String pidAndHost = ManagementFactory.getRuntimeMXBean().getName();
  public static final String EXTENSION = "json";

  @Override
  public String getFileName(Repository repository, EntityDescriptor descriptor, Object object) {
    Object naturalId = descriptor.getNaturalId(object);
    if (naturalId != null) {
      String naturalIdString = NameStripper.stripName(String.valueOf(naturalId));
      String retval = naturalIdString + "." + EXTENSION;
      log.trace("Generated file name \"{}\" via natural id for {}", retval, object);
      return retval;
    } else {
      String hexString = parseHashCode(object);
      String retval = hexString + "." + EXTENSION;
      log.trace("Generated file name \"{}\" via hashcode for {}", retval, object);
      return retval;
    }
  }

  @Override
  public String getFlushFileName(Repository repository, EntityDescriptor descriptor, Object object) {
    return "." + getFileName(repository, descriptor, object) + ".flush";
  }

  protected String parseHashCode(Object object) {
    int hashCode = Objects.hashCode(object);
    byte[] array = new byte[]{(byte) (hashCode >>> 24), (byte) (hashCode >>> 16), (byte) (hashCode >>> 8), (byte) hashCode};
    return Hex.encodeHexString(array);
  }
}
