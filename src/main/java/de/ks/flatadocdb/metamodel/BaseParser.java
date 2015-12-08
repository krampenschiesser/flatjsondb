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

import org.reflections.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class BaseParser {
  @SuppressWarnings("unchecked")
  protected <T> T getInstance(Class<?> clazz) {
    Set<Constructor> persisterConstructors = ReflectionUtils.getConstructors(clazz, ctor -> ctor.getParameterCount() == 0 && Modifier.isPublic(ctor.getModifiers()));
    try {
      if (persisterConstructors.size() > 0) {
        check(persisterConstructors, c -> c.size() != 1, c -> "Found no matching default constructor on given implementation " + clazz.getName());
        return (T) persisterConstructors.iterator().next().newInstance();
      } else {
        return (T) clazz.newInstance();
      }
    } catch (Exception e) {
      throw new ParseException("Could not instantiate default constructor on " + clazz);
    }
  }

  protected <T> void check(T t, Predicate<T> filter, Function<T, String> errorMsg) {
    if (filter.test(t)) {
      throw new ParseException(errorMsg.apply(t));
    }
  }
}
