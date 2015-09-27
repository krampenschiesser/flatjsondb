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

import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.ifc.PropertyPersister;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Immutable
public class EntityDescriptor {
  public static class Builder {
    public static Builder create() {
      return new Builder();
    }

    private MethodHandle versionAccess;
    private MethodHandle naturalIdFieldAccess;
    private MethodHandle idAccess;
    private EntityPersister persister;
    private Class<?> entityClass;
    private final Map<Field, PropertyPersister> propertyPersisters = new HashMap<>();

    private Builder() {
      //
    }

    public Builder entity(Class<?> entity) {
      entityClass = entity;
      return this;
    }

    public Builder version(MethodHandle h) {
      versionAccess = h;
      return this;
    }

    public Builder id(MethodHandle h) {
      idAccess = h;
      return this;
    }

    public Builder natural(MethodHandle h) {
      naturalIdFieldAccess = h;
      return this;
    }

    public Builder persister(EntityPersister p) {
      persister = p;
      return this;
    }

    public Builder property(Field field, PropertyPersister persister) {
      propertyPersisters.put(field, persister);
      return this;
    }

    public Builder properties(Map<Field, PropertyPersister> persisters) {
      propertyPersisters.putAll(persisters);
      return this;
    }

    public EntityDescriptor build() {
      return new EntityDescriptor(this);
    }
  }

  protected final Class<?> entityClass;
  @Nullable
  protected final MethodHandle naturalIdFieldAccess;
  protected final MethodHandle versionAccess;
  protected final MethodHandle idAccess;
  protected final EntityPersister persister;
  protected final Map<Field, PropertyPersister> propertyPersisters;

  public EntityDescriptor(Builder b) {
    this.entityClass = b.entityClass;
    this.persister = b.persister;
    this.idAccess = b.idAccess;
    this.naturalIdFieldAccess = b.naturalIdFieldAccess;
    this.versionAccess = b.versionAccess;
    this.propertyPersisters = Collections.unmodifiableMap(b.propertyPersisters);
  }

  public Class<?> getEntityClass() {
    return entityClass;
  }

  public EntityPersister getPersister() {
    return persister;
  }

  public boolean isVersioned() {
    return versionAccess != null;
  }

  public boolean hasNaturalId() {
    return naturalIdFieldAccess != null;
  }

  public Optional<PropertyPersister> getPropertyPersister(Field field) {
    return Optional.ofNullable(propertyPersisters.get(field));
  }

  /**
   * @param property field name
   * @return the persister if present
   * @throws IllegalStateException when 2 fields have the same name.
   */
  public Optional<PropertyPersister> getPropertyPersister(String property) throws IllegalStateException {
    Set<PropertyPersister> persisters = this.propertyPersisters.entrySet().stream().filter(e -> e.getKey().getName().equals(property)).map(e -> e.getValue()).collect(Collectors.toSet());
    if (persisters.size() > 1) {
      throw new IllegalStateException("Found multiple property persisters for property " + property + " on " + entityClass.getName());
    } else if (persisters.size() == 1) {
      return Optional.of(persisters.iterator().next());
    } else {
      return Optional.empty();
    }
  }

  public Map<Field, PropertyPersister> getPropertyPersisters() {
    return propertyPersisters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityDescriptor)) {
      return false;
    }
    EntityDescriptor entityDescriptor = (EntityDescriptor) o;
    return !(entityClass != null ? !entityClass.equals(entityDescriptor.entityClass) : entityDescriptor.entityClass != null);
  }

  @Override
  public int hashCode() {
    return entityClass != null ? entityClass.hashCode() : 0;
  }
}
