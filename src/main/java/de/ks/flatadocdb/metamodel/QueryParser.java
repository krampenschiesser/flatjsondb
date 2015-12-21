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
package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.query.Query;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryParser {

  public Set<Query<?, ?>> getQueries(Class<?> clazz) {
    @SuppressWarnings("unchecked")
    Set<Method> allMethods = ReflectionUtils.getAllMethods(clazz, m -> Modifier.isStatic(m.getModifiers()), //
      m -> Modifier.isPublic(m.getModifiers()), //
      m -> m.getReturnType().equals(Query.class));
    return allMethods.stream().map(this::invoke).collect(Collectors.toSet());
  }

  private Query<?, ?> invoke(Method method) {
    try {
      Object value = method.invoke(null);
      return (Query<?, ?>) value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
