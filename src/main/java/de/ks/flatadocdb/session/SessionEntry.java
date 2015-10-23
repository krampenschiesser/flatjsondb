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

import java.nio.file.Path;

public class SessionEntry {
  protected final Object object;
  protected final Object naturalId;
  protected final String id;
  protected long version;
  protected final Path completePath;

  public SessionEntry(Object object, String id, long version, Object naturalId, Path completePath) {
    this.object = object;
    this.id = id;
    this.version = version;
    this.naturalId = naturalId;
    this.completePath = completePath;
  }

  public Object getObject() {
    return object;
  }

  public String getId() {
    return id;
  }

  public Object getNaturalId() {
    return naturalId;
  }

  public long getVersion() {
    return version;
  }

  public Path getCompletePath() {
    return completePath;
  }
}
