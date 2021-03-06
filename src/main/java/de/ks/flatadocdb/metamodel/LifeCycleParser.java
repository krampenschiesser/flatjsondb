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

import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parses a class for all lifecycle annotations, like @PrePersist and returns the handles
 */
public class LifeCycleParser {
  private static final Logger log = LoggerFactory.getLogger(LifeCycleParser.class);

  public Map<LifeCycle, Set<MethodHandle>> parseMethods(Class<?> clazz) {
    HashMap<LifeCycle, Set<MethodHandle>> lifecycleMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    Set<Method> methods = ReflectionUtils.getAllMethods(clazz, c -> !Modifier.isStatic(c.getModifiers()));

    for (Method method : methods) {
      for (LifeCycle value : LifeCycle.values()) {
        if (method.isAnnotationPresent(value.getAnnotation())) {
          checkMethod(method);
          log.debug("Discovered lifecycle method {} in {} annotated with {}", method, clazz.getSimpleName(), value.getAnnotation());

          lifecycleMap.putIfAbsent(value, new HashSet<>());
          method.setAccessible(true);
          try {
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            lifecycleMap.get(value).add(handle);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return lifecycleMap;
  }

  private void checkMethod(Method method) {
    if (method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Method " + method.getName() + " has parameters but is not allowed to have any.");
    }
  }
}
