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

import java.util.*;

public class RelationSet<E> extends RelationCollection<E, Set<E>, Set<String>> implements Set<E> {
  public RelationSet(Set<String> ids, Session session) {
    super(new HashSet<>(), ids, session);
  }

  @Override
  public int size() {
    checkInitialize();
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    checkInitialize();
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    checkInitialize();
    return delegate.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    checkInitialize();
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    checkInitialize();
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    checkInitialize();
    return delegate.toArray(a);
  }

  @Override
  public boolean add(E e) {
    checkInitialize();
    return delegate.add(e);
  }

  @Override
  public boolean remove(Object o) {
    checkInitialize();
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    checkInitialize();
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkInitialize();
    return delegate.addAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    checkInitialize();
    return delegate.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    checkInitialize();
    return delegate.removeAll(c);
  }

  @Override
  public void clear() {
    checkInitialize();
    delegate.clear();
  }

  @Override
  public boolean equals(Object o) {
    checkInitialize();
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    checkInitialize();
    return delegate.hashCode();
  }

  @Override
  public Spliterator<E> spliterator() {
    checkInitialize();
    return delegate.spliterator();
  }
}
