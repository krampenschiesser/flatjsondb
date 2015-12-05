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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates the unqiue id(sha1 hex) for a file in the repository.
 */
public class DefaultIdGenerator {
  private static final Logger log = LoggerFactory.getLogger(DefaultIdGenerator.class);
  static final ThreadLocal<java.security.MessageDigest> sha1 = new ThreadLocal<MessageDigest>() {
    @Override
    protected MessageDigest initialValue() {
      try {
        return MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
  };

  public String getSha1Hash(Path repository, Path targetPath) {//for debugging and readability the string is used instead of the byte array. Also avoids possible programming errors using the byte array in equals/hashcode.
    String relative = getRelativePath(repository, targetPath);
    MessageDigest digest = sha1.get();
    byte[] checksum = digest.digest(relative.getBytes(Charsets.UTF_16));
    String hexString = Hex.encodeHexString(checksum);
    log.trace("Generated {} \"{}\" for {}", digest.getAlgorithm(), hexString, relative);
    return hexString;
  }

  protected String getRelativePath(Path repository, Path targetPath) {
    return StringUtils.replace(repository.relativize(targetPath).toString(), "\\", "/");
  }
}
