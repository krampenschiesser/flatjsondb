/**
 * Copyright [2015] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ks.flatadocdb.query;

import de.ks.reflection.PropertyPath;

import java.util.function.Function;

public class Query<E, V> {
  @SuppressWarnings("unchecked")
  public static <E, V> Query<E, V> of(Class<E> clazz, Function<E, V> result) {
    return new Query(clazz, PropertyPath.ofTypeSafe(clazz, result::apply));
  }

  protected Class<E> clazz;
  protected PropertyPath propertyPath;

  public Query(Class<E> clazz, PropertyPath propertyPath) {
    this.clazz = clazz;
    this.propertyPath = propertyPath;
  }

  public Class<E> getOwnerClass() {
    return clazz;
  }

  public V getValue(E instance) {
    return propertyPath.getValue(instance);
  }

  public String getName() {
    return propertyPath.getPropertyPath();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Query)) {
      return false;
    }
    Query<?, ?> query = (Query<?, ?>) o;
    return clazz.equals(query.clazz) && propertyPath.equals(query.propertyPath);
  }

  @Override
  public int hashCode() {
    int result = clazz.hashCode();
    result = 31 * result + propertyPath.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return propertyPath.toString();
  }
}
