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
package de.ks.flatadocdb.session.relation;

import de.ks.flatadocdb.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A collection wrapepr that lazily loads entities in a collection.
 *
 * @param <E>
 * @param <DelegateType>
 * @param <IdCollectionType>
 */
public class RelationCollection<E, DelegateType extends Collection<E>, IdCollectionType extends Collection<String>> {
  private static final Logger log = LoggerFactory.getLogger(RelationCollection.class);
  protected final DelegateType delegate;
  protected final IdCollectionType ids;
  protected final AtomicBoolean loaded = new AtomicBoolean(false);
  protected final Session session;

  public RelationCollection(DelegateType delegate, IdCollectionType ids, Session session) {
    this.delegate = delegate;
    this.ids = ids;
    this.session = session;
  }

  protected void checkInitialize() {
    if (!loaded.get()) {
      session.checkCorrectThread();
      log.debug("Loading {} elements of lazy collection", ids.size());

      for (String id : ids) {
        Optional<Object> found = session.findById(id);
        if (found.isPresent()) {
          @SuppressWarnings("unchecked")
          E e = (E) found.get();
          delegate.add(e);
          log.trace("Found related {} for id {}", e, id);
        }
      }
      if (ids.size() != delegate.size()) {
        log.warn("Got different collection size after loading. Expected {} elements but found {}", ids.size(), delegate.size());
      }
      loaded.set(true);
    }
  }
}
