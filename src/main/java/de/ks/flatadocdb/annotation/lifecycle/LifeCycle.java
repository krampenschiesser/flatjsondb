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

package de.ks.flatadocdb.annotation.lifecycle;

import java.lang.annotation.Annotation;

public enum LifeCycle {
  POST_LOAD(PostLoad.class),
  POST_PERSIST(PostPersist.class),
  POST_REMOVE(PostRemove.class),
  POST_UPDATE(PostUpdate.class),
  PRE_PERSIST(PrePersist.class),
  PRE_REMOVE(PreRemove.class),
  PRE_UPDATE(PreUpdate.class);

  private final Class<? extends Annotation> annotation;

  LifeCycle(Class<? extends Annotation> annotation) {
    this.annotation = annotation;
  }

  public Class<? extends Annotation> getAnnotation() {
    return annotation;
  }
}
