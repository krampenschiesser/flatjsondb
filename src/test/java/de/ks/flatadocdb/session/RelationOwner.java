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

import de.ks.flatadocdb.annotation.Child;
import de.ks.flatadocdb.annotation.Children;
import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.annotation.ToMany;
import de.ks.flatadocdb.entity.NamedEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class RelationOwner extends NamedEntity {
  @ToMany
  protected List<Related> relatedList = new ArrayList<>();
  @ToMany(lazy = false)
  protected Set<Related> relatedSet = new HashSet<>();
  @Children
  protected List<Related> relatedChildren = new ArrayList<>();
  @Child
  protected Related child;

  public RelationOwner(String name) {
    super(name);
  }

  protected RelationOwner() {
    super(null);
  }

  public List<Related> getRelatedList() {
    return relatedList;
  }

  public Set<Related> getRelatedSet() {
    return relatedSet;
  }

  public List<Related> getRelatedChildren() {
    return relatedChildren;
  }

  public Related getChild() {
    return child;
  }

  public void setChild(Related child) {
    this.child = child;
  }
}
