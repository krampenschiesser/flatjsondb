/*
 * Copyright [2014] [Christian Loehnert, krampenschiesser@gmail.com]
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

package de.ks.reflection;

import com.google.common.primitives.Primitives;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.objenesis.ObjenesisStd;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class PropertyPath {
  private static final ObjenesisStd objenesis = new ObjenesisStd();

  public static <T> String methodName(Class<T> clazz, Consumer<T> consumer) {
    PropertyPath path = new PropertyPath(clazz);
    consumer.accept(path.build());
    return path.getLastMethodName();
  }

  public static <T> String property(Class<T> clazz, Consumer<T> consumer) {
    PropertyPath path = new PropertyPath(clazz);
    consumer.accept(path.build());
    if (path.getPropertyPath().isEmpty()) {
      String methodName = path.getLastMethodName();
      if (methodName.startsWith("get")) {
        return toFirstLowerCase(methodName, 3);
      } else if (methodName.startsWith("is")) {
        return toFirstLowerCase(methodName, 2);
      }
      throw new IllegalArgumentException("Could not find property for " + path);
    } else {
      return path.getPropertyPath();
    }
  }

  public static String toFirstLowerCase(String methodName, int start) {
    String substring = methodName.substring(start);
    if (substring.length() == 1) {
      return substring.toLowerCase(Locale.ROOT);
    } else {
      return substring.substring(0, 1).toLowerCase(Locale.ROOT) + substring.substring(1);
    }
  }

  public static <T> PropertyPath of(Class<T> clazz, Consumer<T> consumer) {
    PropertyPath path = new PropertyPath(clazz);
    consumer.accept(path.build());
    return path;
  }

  public static <T> PropertyPath ofTypeSafe(Class<T> clazz, Function<T, ?> function) {
    PropertyPath path = new PropertyPath(clazz);
    function.apply(path.build());
    return path;
  }

  public static <T> PropertyPath of(Class<T> clazz) {
    return new PropertyPath(clazz);
  }

  private static final Logger log = LoggerFactory.getLogger(PropertyPath.class);

  protected Class<?> root;
  protected List<Method> methodPath = new ArrayList<>(25);
  protected List<String> stringPath = new ArrayList<>(25);
  protected List<String> fieldPath = new ArrayList<>(25);
  protected boolean setter;
  protected boolean getter;
  protected Class<?>[] parameterTypes;
  protected Class<?> returnType;
  protected Field field;

  public PropertyPath(Class<?> root) {
    this.root = root;
  }

  @SuppressWarnings("unchecked")
  public <T> T build() {
    stringPath.clear();
    fieldPath.clear();
    setter = false;
    getter = false;
    parameterTypes = null;
    returnType = null;
    field = null;

    return (T) callBack(root);
  }

  @SuppressWarnings("unchecked")
  protected Object callBack(Class<?> clazz) {
    if (Modifier.isFinal(clazz.getModifiers())) {
      return null;
    }
    ProxyFactory factory = new ProxyFactory();
    factory.setSuperclass(clazz);
    Class proxy = factory.createClass();

    Object retval = objenesis.newInstance(proxy);
    ((Proxy) retval).setHandler(new MethodHandler() {
      @Override
      public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {

        String methodName = thisMethod.getName();
        switch (methodName) {
          case "finalize":
            return null;
          case "toString":
            return null;
          default:
            break;
        }
        stringPath.add(methodName);
        methodPath.add(thisMethod);

        setter = isSetter(thisMethod);
        getter = isGetter(thisMethod);
        parameterTypes = thisMethod.getParameterTypes();
        returnType = thisMethod.getReturnType();

        field = discoverField(thisMethod);
        if (field != null) {
          fieldPath.add(field.getName());
        }

        Class<?> returnType = thisMethod.getReturnType();
        if (boolean.class.equals(Primitives.unwrap(returnType))) {
          return false;
        } else if (int.class.equals(Primitives.unwrap(returnType))) {
          return 42;
        } else if (long.class.equals(Primitives.unwrap(returnType))) {
          return 42L;
        } else if (short.class.equals(Primitives.unwrap(returnType))) {
          return (short) 42;
        } else if (char.class.equals(Primitives.unwrap(returnType))) {
          return (char) 42;
        } else if (byte.class.equals(Primitives.unwrap(returnType))) {
          return (byte) 4;
        } else if (float.class.equals(Primitives.unwrap(returnType))) {
          return 42F;
        } else if (double.class.equals(Primitives.unwrap(returnType))) {
          return 42D;
        }
        if (returnType.equals(Void.TYPE)) {
          return null;
        } else if (isSetter()) {
          return null;
        } else if (returnType.getPackage().getName().startsWith("java")) {//sadly, sadly, sadly javassist move java classes to new packages which are then incompatible...
          return null;
        } else {
          return callBack(thisMethod.getReturnType());
        }
      }
    });

    return retval;
  }

  public static Logger getLog() {
    return log;
  }

  @SuppressWarnings("unchecked")
  public <T> Class<T> getRoot() {
    return (Class<T>) root;
  }

  public Class<?>[] getParameterTypes() {
    return parameterTypes;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public boolean isSetter() {
    return setter;
  }

  public boolean isGetter() {
    return getter;
  }

  public boolean isFieldAvailable() {
    return stringPath.size() == fieldPath.size();
  }

  public void setValue(Object source, Object value) {
    Object instance = source;
    Method lastMethod = methodPath.get(methodPath.size() - 1);
    for (Method method : methodPath) {
      if (method.equals(lastMethod)) {
        break;
      }
      try {
        instance = method.invoke(instance);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        log.error("Could not follow path {}: ", this, e);
        return;
      }
    }
    try {
      String methodName = lastMethod.getName().toLowerCase(Locale.ROOT);

      boolean isBooleanGetter = methodName.startsWith("is");
      boolean isSimpleGetter = methodName.startsWith("get");

      if (lastMethod.getParameterTypes().length == 0 && isBooleanGetter || isSimpleGetter) {
        int index = isBooleanGetter ? 2 : 3;
        Class<?> declaringClass = lastMethod.getDeclaringClass();
        Optional<Method> methodOptional = Arrays.asList(declaringClass.getDeclaredMethods()).stream()//
          .filter((m) -> m.getName().startsWith("set") && m.getName().toLowerCase(Locale.ROOT).endsWith(methodName.substring(index)))//
          .findFirst();
        if (methodOptional.isPresent()) {
          Method method = methodOptional.get();
          method.setAccessible(true);
          method.invoke(instance, value);
        }
      } else {
        lastMethod.setAccessible(true);
        lastMethod.invoke(instance, value);
      }
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      log.error("Could not invoke setter on path {}: ", this, e);
    }
  }

  @SuppressWarnings("unchecked")
  public <U> U getValue(Object source) {
    if (!isGetter()) {
      log.error("Declared path [{}]is no getter", this);
      return null;
    }
    Object instance = source;
    for (Method method : methodPath) {
      if (instance == null) {
        return null;
      }
      try {
        method.setAccessible(true);
        instance = method.invoke(instance);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        log.error("Could not follow path {}: ", this, e);
        return null;
      }
    }
    return (U) instance;
  }

  public void walk(Object source) {
    Object instance = source;
    for (Method method : methodPath) {
      if (instance == null) {
        return;
      }
      if (method.getParameterTypes().length > 0) {
        return;
      }
      try {
        instance = method.invoke(instance);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        log.error("Could not follow path {}: ", this, e);
        return;
      }
    }
  }

  protected boolean isGetter(Method method) {
    if (method.getParameterTypes().length > 0) {
      return false;
    }

    String name = method.getName();
    Class<?> returnType = method.getReturnType();

    if (name.startsWith("is")) {
      if (Boolean.TYPE.equals(returnType) || (Boolean.class.equals(returnType))) {
        return true;
      }
    }
    if (name.startsWith("get") && !Void.TYPE.equals(returnType)) {
      return true;
    }
    return false;
  }

  protected boolean isSetter(Method method) {
    if (method.getParameterTypes().length != 1) {
      return false;
    }

    if (method.getName().startsWith("set")) {
      return true;
    } else {
      return false;
    }
  }

  protected Field discoverField(Method method) {
    String methodName = method.getName();
    if (methodName.startsWith("get") || methodName.startsWith("set")) {
      methodName = methodName.substring(3);
    } else if (methodName.startsWith("is")) {
      methodName = methodName.substring(2);
    }
    methodName = methodName.toLowerCase(Locale.ROOT);
    @SuppressWarnings("unchecked")
    Set<Field> fieldsRecursive = ReflectionUtils.getAllFields(method.getDeclaringClass());
    for (Field field : fieldsRecursive) {
      if (field.getName().toLowerCase(Locale.ROOT).equals(methodName)) {
        return field;
      }
    }
    return null;
  }

  public String getStringFieldPath() {
    StringBuilder builder = new StringBuilder();
    builder.append(root.getSimpleName()).append(".");
    for (String fieldName : fieldPath) {
      builder.append(fieldName).append(".");
    }
    String path = builder.toString();
    return path.substring(0, path.length() - 1);
  }

  public String getPropertyPath() {
    StringBuilder builder = new StringBuilder();
    for (String fieldName : fieldPath) {
      builder.append(fieldName).append(".");
    }
    String path = builder.toString();
    if (path.isEmpty()) {
      return path;
    } else {
      return path.substring(0, path.length() - 1);
    }
  }

  public Method getLastMethod() {
    return methodPath.get(methodPath.size() - 1);
  }

  public String getLastMethodName() {
    return methodPath.get(methodPath.size() - 1).getName();
  }

  public String toLocalizationPath() {
    StringBuilder builder = new StringBuilder();
    builder.append(root.getSimpleName()).append(".");
    for (String fieldName : fieldPath) {
      builder.append(fieldName).append(".");
    }
    String path = builder.toString();
    return path.substring(0, path.length() - 1);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((methodPath == null) ? 0 : methodPath.hashCode());
    result = (prime * result) + ((root == null) ? 0 : root.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PropertyPath)) {
      return false;
    }
    PropertyPath other = (PropertyPath) obj;
    if (methodPath == null) {
      if (other.methodPath != null) {
        return false;
      }
    } else if (!methodPath.equals(other.methodPath)) {
      return false;
    }
    if (root == null) {
      if (other.root != null) {
        return false;
      }
    } else if (!root.equals(other.root)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(root.getSimpleName()).append(".");
    for (String methodName : stringPath) {
      builder.append(methodName).append("().");
    }

    String path = builder.toString();
    return path.substring(0, path.length() - 3);
  }

}
