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
package de.ks.flatadocdb.defaults;

import com.google.common.primitives.Primitives;
import de.ks.flatadocdb.ifc.LuceneDocumentExtractor;
import org.apache.lucene.document.Document;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionLuceneDocumentExtractor implements LuceneDocumentExtractor {
  static ConcurrentHashMap<Class<?>, Set<Field>> cache = new ConcurrentHashMap<>();

  @Override
  public Document createDocument(Object instance) {
    Set<Field> fields = getFields(instance);
    for (Field field : fields) {

    }

    return null;
  }

  protected Set<Field> getFields(Object instance) {
    Class<?> clazz = instance.getClass();

    if (!cache.containsKey(clazz)) {
      @SuppressWarnings("unchecked")
      Set<Field> allFields = ReflectionUtils.getAllFields(clazz, this::filterField);
      cache.putIfAbsent(clazz, allFields);
    }
    return cache.get(clazz);
  }

  protected boolean filterField(Field f) {
    boolean noStatic = !Modifier.isStatic(f.getModifiers());
    if (noStatic) {
      Class<?> type = f.getType();

      boolean validType = Primitives.allPrimitiveTypes().contains(Primitives.unwrap(type));
      validType = validType || type.equals(String.class);
      validType = validType || type.equals(Enum.class);
      Type genericType = f.getGenericType();
      validType = validType || type.equals(Array.class);
      validType = validType || Collection.class.isAssignableFrom(type);
      return validType;
    }
    return false;
  }
}
