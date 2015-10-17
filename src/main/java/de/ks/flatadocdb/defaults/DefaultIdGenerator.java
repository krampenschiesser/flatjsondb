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

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DefaultIdGenerator {
  static final java.security.MessageDigest sha1;

  static {
    try {
      sha1 = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String getSha1Hash(Path repository, Path targetPath) {//for debugging and readability the string is used instead of the byte array. Also avoids possible programming errors using the byte array in equals/hashcode.
    String relative = getRelativePath(repository, targetPath);
    byte[] checksum = sha1.digest(relative.getBytes(Charsets.UTF_16));
    return Hex.encodeHexString(checksum);
  }

  protected String getRelativePath(Path repository, Path targetPath) {
    return StringUtils.replace(repository.relativize(targetPath).toString(), "\\", "/");
  }
}
