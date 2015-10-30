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

package de.ks.flatadocdb.index;

import de.ks.flatadocdb.Repository;

import java.nio.file.Path;

public class IndexElement {
  private Repository repository;
  private Path pathInRepository;
  private String id;
  private Object naturalId;
  private Class<?> entityClass;
  private byte[] md5Sum;
  private long lastModified;

  public IndexElement(Repository repository, Path pathInRepository, String id, Object naturalId, Class<?> entityClass) {
    this.repository = repository;
    this.pathInRepository = pathInRepository;
    this.id = id;
    this.naturalId = naturalId;
    this.entityClass = entityClass;
  }

  public byte[] getMd5Sum() {
    return md5Sum;
  }

  public void setMd5Sum(byte[] md5Sum) {
    this.md5Sum = md5Sum;
  }

  public Class<?> getEntityClass() {
    return entityClass;
  }

  public Repository getRepository() {
    return repository;
  }

  public Path getPathInRepository() {
    return pathInRepository;
  }

  public String getId() {
    return id;
  }

  public Object getNaturalId() {
    return naturalId;
  }

  public boolean hasNaturalId() {
    return naturalId != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IndexElement)) {
      return false;
    }

    IndexElement that = (IndexElement) o;

    return id.equals(that.id);

  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
