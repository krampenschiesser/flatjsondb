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

package de.ks.flatadocdb.session.relation;

import de.ks.flatadocdb.session.Session;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LazyEntity implements MethodHandler {
  private static final Logger log = LoggerFactory.getLogger(LazyEntity.class);
  private static final ObjenesisStd objenesisStd = new ObjenesisStd(true);
  private static final ConcurrentHashMap<Class<?>, Class<?>> proxyClasses = new ConcurrentHashMap<>();

  public static <E> E proxyFor(Class<E> clazz, String id, Session session) {
    return proxyFor(clazz, id, session, null, null);
  }

  public static <E> E proxyFor(Class<E> clazz, String id, Session session, Object owner, Field ownerField) {
    Class<?> proxy = proxyClasses.computeIfAbsent(clazz, c -> {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(clazz);
      return factory.createClass();
    });

    LazyEntity lazyEntity = new LazyEntity(id, session, owner, ownerField);
    @SuppressWarnings("unchecked")
    E instance = (E) objenesisStd.newInstance(proxy);

    ((ProxyObject) instance).setHandler(lazyEntity);
    return instance;
  }

  protected final AtomicBoolean loaded = new AtomicBoolean(false);
  protected final String id;
  protected final Session session;
  private final Object owner;
  private final Field ownerField;
  protected final AtomicReference<Object> delegate = new AtomicReference<>();

  public LazyEntity(String id, Session session, @Nullable Object owner, @Nullable Field ownerField) {
    this.id = id;
    this.session = session;
    if (owner != null) {
      Objects.requireNonNull(ownerField);
    }
    this.owner = owner;
    this.ownerField = ownerField;
    if (ownerField != null) {
      ownerField.setAccessible(true);
    }
  }

  @Override
  public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
    if (!loaded.get()) {
      session.checkCorrectThread();
      Object found = session.findById(id);
      if (found == null) {
        log.warn("For {} loaded lazy entity {} but found none", ownerField, id);
      } else {
        log.debug("For {} loaded lazy entity {}({})", ownerField, found, id);
      }
      delegate.set(found);
      applyToOwnerField();
      loaded.set(true);
    }
    return thisMethod.invoke(delegate.get(), args);
  }

  private void applyToOwnerField() {
    if (owner != null) {
      Object fieldInstance = delegate.get();
      try {
        ownerField.set(owner, fieldInstance);
      } catch (Exception e) {
        log.warn("Cannot set owner field {}", ownerField, e);
      }
    }
  }
}
