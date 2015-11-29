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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class Repository implements AutoCloseable {
  public static final String LUCENE_DIR = ".lucene";
  private static final Logger log = LoggerFactory.getLogger(Repository.class);

  protected final Path path;
  protected final String name;
  protected final EncryptionMode encryption;
  protected final Directory luceneDirectory;

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
    Path subPath = path.resolve(LUCENE_DIR);
    if (!Files.exists(subPath)) {
      try {
        Files.createDirectories(subPath);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      luceneDirectory = FSDirectory.open(subPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Path getPath() {
    return path;
  }

  public Set<Path> getAllFilesInRepository() {
    HashSet<Path> filesInRepository = new HashSet<>();
    SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toFile().exists()) {
          filesInRepository.add(file);
        }
        return super.visitFile(file, attrs);
      }
    };
    try {
      Files.walkFileTree(path, fileVisitor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return filesInRepository;
  }

  public String getName() {
    return name;
  }

  public EncryptionMode getEncryption() {
    return encryption;
  }

  @Override
  public void close() {
    try {
      luceneDirectory.close();
    } catch (IOException e) {
      log.error("Could not close lucene index {}", luceneDirectory, e);
    }
  }
}
