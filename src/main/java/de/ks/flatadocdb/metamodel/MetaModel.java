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

package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.exception.EntityNotRegisteredException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public class MetaModel {
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();
  protected final Map<Class<?>, EntityDescriptor> clazz2EntityDescriptor = new HashMap<>();

  public List<EntityDescriptor> getEntities() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(clazz2EntityDescriptor.values());
    } finally {
      lock.readLock().unlock();
    }
  }

  public void addEntity(Class<?> clazz) {
    lock.writeLock().lock();
    try {
      EntityDescriptor entityDescriptor = new Parser().parse(clazz);
      clazz2EntityDescriptor.put(entityDescriptor.getEntityClass(), entityDescriptor);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public EntityDescriptor getEntityDescriptor(Class<?> clazz) throws EntityNotRegisteredException {
    lock.readLock().lock();
    try {
      EntityDescriptor retval = clazz2EntityDescriptor.get(clazz);
      if (retval == null) {
        throw new EntityNotRegisteredException(clazz, new HashSet<>(clazz2EntityDescriptor.keySet()));
      } else {
        return retval;
      }
    } finally {
      lock.readLock().unlock();
    }
  }
}
