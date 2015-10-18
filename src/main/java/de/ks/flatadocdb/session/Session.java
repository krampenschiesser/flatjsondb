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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

@NotThreadSafe//can only be used as ThreadLocal
public class Session {
  protected final Map<String, SessionEntry> entriesById = new HashMap<>();
  protected final Map<Object, SessionEntry> entriesByNaturalId = new HashMap<>();
//  protected final IdentityHashMap<Long,SessionEntry> entriesById = new HashMap<>();

  public void persist(Object entity) {

  }

  public void remove(Object entity) {

  }

  public <E> E findByNaturalId(Class<E> clazz, Object naturalId) {
    return null;
  }

  public <E> E findById(Class<E> clazz, String id) {
    return null;
  }

  public String getId(Object object) {
    return null;
  }
}
