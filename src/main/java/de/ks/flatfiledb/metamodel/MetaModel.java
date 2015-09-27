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
package de.ks.flatfiledb.metamodel;

import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public class MetaModel {
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();
  protected final List<EntityDescriptor> entities = new LinkedList<EntityDescriptor>();

  public List<EntityDescriptor> getEntities() {
    lock.readLock().lock();
    try {
      return entities;
    } finally {
      lock.readLock().unlock();
    }
  }

  public void addEntity(Class<?> clazz) {
    lock.writeLock().lock();
    try {
      EntityDescriptor entityDescriptor = new Parser().parse(clazz);
      entities.add(entityDescriptor);
    } finally {
      lock.writeLock().unlock();
    }
  }
}
