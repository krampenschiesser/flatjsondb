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
import java.util.function.UnaryOperator;

public class RelationList<E> extends RelationCollection<E, List<E>, List<String>> implements List<E> {
  public RelationList(List<String> ids, Session session) {
    super(new ArrayList<>(), ids, session);
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
  public boolean addAll(int index, Collection<? extends E> c) {
    checkInitialize();
    return delegate.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    checkInitialize();
    return delegate.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    checkInitialize();
    return delegate.retainAll(c);
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    checkInitialize();
    delegate.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super E> c) {
    checkInitialize();
    delegate.sort(c);
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
  public E get(int index) {
    checkInitialize();
    return delegate.get(index);
  }

  @Override
  public E set(int index, E element) {
    checkInitialize();
    return delegate.set(index, element);
  }

  @Override
  public void add(int index, E element) {
    checkInitialize();
    delegate.add(index, element);
  }

  @Override
  public E remove(int index) {
    checkInitialize();
    return delegate.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    checkInitialize();
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    checkInitialize();
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator() {
    checkInitialize();
    return delegate.listIterator();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    checkInitialize();
    return delegate.listIterator(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    checkInitialize();
    return delegate.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<E> spliterator() {
    checkInitialize();
    return delegate.spliterator();
  }
}
