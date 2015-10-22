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

package de.ks.flatadocdb.exception;

import java.util.Set;

public class EntityNotRegisteredException extends RuntimeException {
  private final Class<?> requestedClass;
  private final Set<Class<?>> registeredClasses;

  public EntityNotRegisteredException(Class<?> requestedClass, Set<Class<?>> registeredClasses) {
    super("Requested entity class " + requestedClass + " not found in registered classes " + registeredClasses);
    this.requestedClass = requestedClass;
    this.registeredClasses = registeredClasses;
  }

  public Class<?> getRequestedClass() {
    return requestedClass;
  }

  public Set<Class<?>> getRegisteredClasses() {
    return registeredClasses;
  }
}
