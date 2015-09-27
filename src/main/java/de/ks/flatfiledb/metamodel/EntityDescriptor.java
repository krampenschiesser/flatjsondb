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
package de.ks.flatfiledb.metamodel;

import de.ks.flatfiledb.ifc.EntityPersister;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;

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

    public EntityDescriptor build() {
      return new EntityDescriptor(this);
    }
  }

  protected final Class<?> entityClass;
  @Nullable
  protected final MethodHandle naturalIdFieldAccess;
  protected final MethodHandle versionAccess;
  protected final MethodHandle idAccess;
  protected EntityPersister persister;

  public EntityDescriptor(Builder b) {
    this.entityClass = b.entityClass;
    this.persister = b.persister;
    this.idAccess = b.idAccess;
    this.naturalIdFieldAccess = b.naturalIdFieldAccess;
    this.versionAccess = b.versionAccess;
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
