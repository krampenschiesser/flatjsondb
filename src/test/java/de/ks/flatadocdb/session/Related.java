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

import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.annotation.ToOne;
import de.ks.flatadocdb.entity.NamedEntity;

@Entity
public class Related extends NamedEntity {
  @ToOne
  protected RelationOwner owner;

  protected Related() {
    super(null);
  }

  public Related(String name) {
    super(name);
  }

  public Related setId(String id) {
    this.id = id;
    return this;
  }
}
