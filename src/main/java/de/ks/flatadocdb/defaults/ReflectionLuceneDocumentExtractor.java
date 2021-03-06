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
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Extracts indexable fields from a class via reflection.
 * The following dataTypes are valid:
 *
 * * primitives
 * * enums
 * * Strings
 * * LocalDate
 * * LocalTime
 * * LocalDateTime
 *
 * And all collections and arrays of the above types.
 */
public class ReflectionLuceneDocumentExtractor implements LuceneDocumentExtractor {
  private static final Logger log = LoggerFactory.getLogger(ReflectionLuceneDocumentExtractor.class);
  public static final int MAX_LENGHT_STRINGFIELD = 75;

  static final ConcurrentHashMap<Class<?>, Set<DocField>> cache = new ConcurrentHashMap<>();

  @Override
  public Document createDocument(Object instance) {
    Class<?> clazz = instance.getClass();
    Set<DocField> fields = getFields(clazz);

    Document doc = new Document();
    fields.stream().map(f -> f.apply(instance)).filter(Objects::nonNull).forEach(doc::add);
    return doc;
  }

  protected Set<DocField> getFields(Class<?> clazz) {

    if (!cache.containsKey(clazz)) {
      @SuppressWarnings("unchecked")
      Set<Field> allFields = ReflectionUtils.getAllFields(clazz, this::filterField);
      Set<DocField> docFields = allFields.stream().map(this::createDocField).filter(Objects::nonNull).collect(Collectors.toSet());
      docFields.forEach(f -> log.debug("Found indexable lucene field {} for {}", f.getField(), clazz.getSimpleName()));
      cache.putIfAbsent(clazz, docFields);
    }
    return cache.get(clazz);
  }

  protected DocField createDocField(Field f) {
    try {
      Class<?> type = f.getType();
      f.setAccessible(true);
      MethodHandle getter = MethodHandles.lookup().unreflectGetter(f);

      if (TypeUtils.isArrayType(type)) {
        return createArrayDocField(f, getter);
      } else if (Collection.class.isAssignableFrom(type)) {
        return createCollectionDocField(f, getter);
      } else if (String.class.equals(type)) {
        return createStringDocField(f, getter);
      } else if (LocalDateTime.class.equals(type)) {
        return createLocalDateTimeDocField(f, getter);
      } else {
        return new DocField(f, getter, (id, value) -> new StringField(id, String.valueOf(value), org.apache.lucene.document.Field.Store.YES));
      }
    } catch (Exception e) {
      log.error("Could not extract docfield from {}", f, e);
      throw new RuntimeException(e);
    }
  }

  private DocField createLocalDateTimeDocField(Field f, MethodHandle getter) {
    return new DocField(f, getter, (id, value) -> {
      LocalDateTime localDateTime = (LocalDateTime) value;
      long utcTime = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
      return new LongField(id, utcTime, org.apache.lucene.document.Field.Store.YES);
    });
  }

  private ReflectionLuceneDocumentExtractor.DocField createStringDocField(Field f, MethodHandle getter) {
    return new DocField(f, getter, (id, value) -> {
      String stringValue = String.valueOf(value);
      if (stringValue.length() > MAX_LENGHT_STRINGFIELD) {
        return new TextField(id, stringValue, org.apache.lucene.document.Field.Store.YES);
      } else {
        return new StringField(id, stringValue, org.apache.lucene.document.Field.Store.YES);
      }
    });
  }

  private ReflectionLuceneDocumentExtractor.DocField createCollectionDocField(Field f, MethodHandle getter) {
    return new DocField(f, getter, (id, value) -> {
      @SuppressWarnings("unchecked")
      String string = ((Collection<Object>) value).stream().map(String::valueOf).collect(Collectors.joining(", "));
      return new StringField(id, string, org.apache.lucene.document.Field.Store.YES);
    });
  }

  private ReflectionLuceneDocumentExtractor.DocField createArrayDocField(Field f, MethodHandle getter) {
    return new DocField(f, getter, (id, value) -> {
      StringBuilder builder = new StringBuilder();

      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        Object element = Array.get(value, i);
        builder.append(element);
        if (i != length - 1) {
          builder.append(", ");
        }
      }
      return new StringField(id, builder.toString(), org.apache.lucene.document.Field.Store.YES);
    });
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

  public static class DocField {
    private static final Logger log = LoggerFactory.getLogger(DocField.class);
    private final Field field;
    private final MethodHandle handle;
    private final BiFunction<String, Object, ? extends IndexableField> fieldSupplier;

    public DocField(Field field, MethodHandle handle) {
      this(field, handle, (id, valueObject) -> new TextField(id, String.valueOf(valueObject), null));
    }

    public DocField(Field field, MethodHandle handle, BiFunction<String, Object, ? extends IndexableField> fieldSupplier) {
      this.field = field;
      this.handle = handle;
      this.fieldSupplier = fieldSupplier;
    }

    public IndexableField apply(Object instance) {
      try {
        Object value = handle.invoke(instance);
        if (value == null) {
          log.trace("Ignoring null value for {}", field);
          return null;
        } else {
          IndexableField apply = fieldSupplier.apply(field.getName(), value);
          log.trace("Indexing value for {}. name={}, value={} ", field, apply.name(), apply.stringValue().length() > 100 ? apply.stringValue().substring(0, 100) : apply.stringValue());
          return apply;
        }
      } catch (Throwable t) {
        log.error("Could not get value from field", t);
        return null;
      }
    }

    public Field getField() {
      return field;
    }
  }
}
