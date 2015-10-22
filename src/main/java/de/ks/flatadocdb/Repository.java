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

package de.ks.flatadocdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Repository {
  protected final Path path;
  protected final String name;
  protected final EncryptionMode encryption;

  public Repository(Path path) {
    this(path, EncryptionMode.NONE);
  }

  public Repository(Path path, EncryptionMode encryptionMode) {
    this.path = path;
    this.name = path.getName(path.getNameCount() - 1).toString();
    this.encryption = encryptionMode;
    if (!path.toFile().exists()) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Path getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public EncryptionMode getEncryption() {
    return encryption;
  }
}
