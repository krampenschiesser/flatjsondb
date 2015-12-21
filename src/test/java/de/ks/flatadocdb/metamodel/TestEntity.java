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

import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.annotation.QueryProvider;
import de.ks.flatadocdb.entity.NamedEntity;
import de.ks.flatadocdb.query.Query;

@Entity
public class TestEntity extends NamedEntity {
  @QueryProvider
  public static Query<TestEntity, String> attributeQuery() {
    return Query.of(TestEntity.class, TestEntity::getAttribute);
  }

  String attribute;

  protected TestEntity() {
    super(null);
  }

  public TestEntity(String name) {
    super(name);
  }

  public String getAttribute() {
    return attribute;
  }

  public TestEntity setAttribute(String attribute) {
    this.attribute = attribute;
    return this;
  }
}
