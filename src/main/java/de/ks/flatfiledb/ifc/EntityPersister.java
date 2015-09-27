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
package de.ks.flatfiledb.ifc;

import de.ks.flatfiledb.metamodel.EntityDescriptor;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;

/**
 * A persister is local for each entity and can store stateful information.
 * It has to be threadsafe as it is used and accessed by all modifying/reading jvm threads.
 */
@ThreadSafe
public interface EntityPersister {
  Object load(EntityDescriptor descriptor);

  void save(EntityDescriptor descriptor, File path, Object object);
}
