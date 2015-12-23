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

import de.ks.flatadocdb.annotation.QueryProvider;
import de.ks.flatadocdb.query.Query;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryParser {

  public Set<Query<?, ?>> getQueries(Class<?> clazz) {
    @SuppressWarnings("unchecked")
    Set<Method> annotatedMethods = ReflectionUtils.getAllMethods(clazz, m -> m.isAnnotationPresent(QueryProvider.class));

    annotatedMethods.stream().filter(m -> !Modifier.isPublic(m.getModifiers())).findAny().ifPresent(m -> {
      throw new ParseException("Found non public query provider method " + m);
    });
    annotatedMethods.stream().filter(m -> !Modifier.isStatic(m.getModifiers())).findAny().ifPresent(m -> {
      throw new ParseException("Found non static query provider method " + m);
    });
    annotatedMethods.stream().filter(m -> !m.getReturnType().equals(Query.class)).findAny().ifPresent(m -> {
      throw new ParseException("Found provider method with invalid return type " + m);
    });

    @SuppressWarnings("unchecked")
    Set<Method> nonAnnotatedMethods = ReflectionUtils.getAllMethods(clazz, m -> Modifier.isStatic(m.getModifiers()), //
      m -> !m.isAnnotationPresent(QueryProvider.class),//
      m -> Modifier.isPublic(m.getModifiers()), //
      m -> m.getReturnType().equals(Query.class));

    if (nonAnnotatedMethods.size() > 0) {
      throw new ParseException("Found query provider methods which are not annotated: " + nonAnnotatedMethods);
    }

    return annotatedMethods.stream().map(this::invoke).collect(Collectors.toSet());
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
