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

package de.ks.flatadocdb.session;

import de.ks.flatadocdb.metamodel.EntityDescriptor;

import java.io.Serializable;
import java.nio.file.Path;

public class SessionEntry {
  protected final Object object;
  protected final Serializable naturalId;
  protected final String id;
  protected long version;
  protected final Path completePath;
  private final EntityDescriptor entityDescriptor;
  protected byte[] md5;

  public SessionEntry(Object object, String id, long version, Serializable naturalId, Path completePath, EntityDescriptor entityDescriptor) {
    this.object = object;
    this.id = id;
    this.version = version;
    this.naturalId = naturalId;
    this.completePath = completePath;
    this.entityDescriptor = entityDescriptor;
  }

  public Object getObject() {
    return object;
  }

  public String getId() {
    return id;
  }

  public Serializable getNaturalId() {
    return naturalId;
  }

  public long getVersion() {
    return version;
  }

  public Path getCompletePath() {
    return completePath;
  }

  public Path getFolder() {
    return completePath.toFile().getParentFile().toPath();
  }

  public String getFileName() {
    return completePath.getFileName().toFile().getName();
  }

  public byte[] getMd5() {
    return md5;
  }

  public void setMd5(byte[] md5) {
    this.md5 = md5;
  }

  public EntityDescriptor getEntityDescriptor() {
    return entityDescriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SessionEntry)) {
      return false;
    }

    SessionEntry that = (SessionEntry) o;
    return id.equals(that.id);

  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("SessionEntry{");
    sb.append("naturalId=").append(naturalId);
    sb.append(", version=").append(version);
    sb.append(", object=").append(object);
    sb.append('}');
    return sb.toString();
  }
}
