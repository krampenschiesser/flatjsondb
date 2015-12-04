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
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.lucene.document.Document;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionLuceneDocumentExtractor implements LuceneDocumentExtractor {
  static ConcurrentHashMap<Class<?>, Set<Field>> cache = new ConcurrentHashMap<>();

  @Override
  public Document createDocument(Object instance) {
    Class<?> clazz = instance.getClass();
    Set<Field> fields = getFields(clazz);
    for (Field field : fields) {

    }

    return null;
  }

  protected Set<Field> getFields(Class<?> clazz) {

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

      boolean validType = isValidBaseType(type);

      Type genericType = f.getGenericType();
      if (!validType && Collection.class.isAssignableFrom(type) && genericType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
        if (actualTypeArguments.length == 1 && actualTypeArguments[0] instanceof Class) {
          validType = isValidBaseType((Class<?>) actualTypeArguments[0]);
        }
      }
      if (!validType && TypeUtils.isArrayType(type)) {
        Type arrayComponentType = TypeUtils.getArrayComponentType(type);
        if (arrayComponentType instanceof Class) {
          validType = isValidBaseType((Class<?>) arrayComponentType);
        }
      }
      return validType;
    }
    return false;
  }

  private boolean isValidBaseType(Class<?> type) {
    boolean validType = Primitives.allPrimitiveTypes().contains(Primitives.unwrap(type));
    validType = validType || type.equals(String.class);
    validType = validType || type.equals(LocalDate.class);
    validType = validType || type.equals(LocalTime.class);
    validType = validType || type.equals(LocalDateTime.class);
    validType = validType || Enum.class.isAssignableFrom(type);
    return validType;
  }
}
