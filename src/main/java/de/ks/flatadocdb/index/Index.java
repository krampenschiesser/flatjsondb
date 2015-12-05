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

import de.ks.flatadocdb.session.SessionEntry;

/**
 * General interface class for indexes
 */
public interface Index {
  void addEntry(SessionEntry entry);

  void removeEntry(SessionEntry entry);

  void updateEntry(SessionEntry entry);

  void recreate();

  void afterPrepare();

  void beforeCommit();

  void afterCommit();

  void afterRollback();

  void close();
}
