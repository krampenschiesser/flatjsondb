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

import de.ks.flatadocdb.annotation.Id;
import de.ks.flatadocdb.annotation.Version;
import de.ks.flatadocdb.annotation.lifecycle.PreUpdate;

import java.time.LocalDateTime;

/**
 * Base class you can use. If you have your own domain model you can just add the corresponding fields and annotations.
 */
public class BaseEntity {
  @Version
  protected long version;
  @Id
  protected String id;

  protected LocalDateTime creationTime;
  protected LocalDateTime updateTime;

  protected BaseEntity() {
    creationTime = LocalDateTime.now();
  }

  public long getVersion() {
    return version;
  }

  public String getId() {
    return id;
  }

  @PreUpdate
  void preUpdate() {
    updateTime = LocalDateTime.now();
  }

  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }
}
