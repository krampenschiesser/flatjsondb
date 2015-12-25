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

package de.ks.flatadocdb.entity;

import de.ks.flatadocdb.annotation.NaturalId;
import de.ks.flatadocdb.annotation.QueryProvider;
import de.ks.flatadocdb.query.Query;

/**
 * Class supporting a natural id that is the name.
 */
public class NamedEntity extends BaseEntity {
  @QueryProvider
  public static Query<NamedEntity, String> nameQuery() {
    return Query.of(NamedEntity.class, NamedEntity::getName);
  }

  @NaturalId
  protected String name;//not final, under some circumstances you might want to set the name later

  public NamedEntity(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamedEntity)) {
      return false;
    }
    NamedEntity that = (NamedEntity) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append("{");
    sb.append("name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
