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

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import de.ks.flatadocdb.annotation.Child;
import de.ks.flatadocdb.annotation.Children;
import de.ks.flatadocdb.annotation.ToMany;
import de.ks.flatadocdb.annotation.ToOne;
import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.ifc.*;
import de.ks.flatadocdb.metamodel.relation.*;
import de.ks.flatadocdb.query.Query;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all meta inforamtion needed to store/load a given entity.
 */
@Immutable
public class EntityDescriptor {
  public static class Builder {

    public static Builder create() {
      return new Builder();
    }

    private MethodHandle versionGetterAccess;
    private MethodHandle versionSetterAccess;
    private MethodHandle naturalIdFieldAccess;
    private MethodHandle idGetterAccess;
    private MethodHandle idSetterAccess;
    private EntityPersister persister;
    private FolderGenerator folderGenerator;
    private FileGenerator fileGenerator;
    private LuceneDocumentExtractor extractor;
    private Class<?> entityClass;
    private Map<LifeCycle, Set<MethodHandle>> lifecycleMethods;
    private final Map<Field, PropertyPersister> propertyPersisters = new HashMap<>();
    private final Set<ToOneRelation> toOneRelations = new HashSet<>();
    private final Set<ToManyRelation> toManyRelations = new HashSet<>();
    private final Set<ToOneChildRelation> toOneChildRelations = new HashSet<>();
    private final Set<ToManyChildRelation> toManyChildRelations = new HashSet<>();
    private final Set<Query<?, ?>> queries = new HashSet<>();

    private Builder() {
      //
    }

    public Builder entity(Class<?> entity) {
      entityClass = entity;
      return this;
    }

    public Builder version(MethodHandle getter, MethodHandle setter) {
      versionSetterAccess = setter;
      versionGetterAccess = getter;
      return this;
    }

    public Builder id(MethodHandle getter, MethodHandle setter) {
      idGetterAccess = getter;
      idSetterAccess = setter;
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

    public Builder fileGenerator(FileGenerator f) {
      fileGenerator = f;
      return this;
    }

    public Builder folderGenerator(FolderGenerator f) {
      folderGenerator = f;
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

    public Builder extractor(LuceneDocumentExtractor extractor) {
      this.extractor = extractor;
      return this;
    }

    public Builder lifecycle(Map<LifeCycle, Set<MethodHandle>> methods) {
      this.lifecycleMethods = methods;
      return this;
    }

    public Builder toOnes(Set<ToOneRelation> relations) {
      this.toOneRelations.addAll(relations);
      return this;
    }

    public Builder toMany(Set<ToManyRelation> relations) {
      this.toManyRelations.addAll(relations);
      return this;
    }

    public Builder toOneChild(Set<ToOneChildRelation> relations) {
      this.toOneChildRelations.addAll(relations);
      return this;
    }

    public Builder toManyChild(Set<ToManyChildRelation> relations) {
      this.toManyChildRelations.addAll(relations);
      return this;
    }

    public Builder queries(Set<Query<?, ?>> relations) {
      this.queries.addAll(relations);
      return this;
    }

    public EntityDescriptor build() {
      return new EntityDescriptor(this);
    }
  }

  protected final Class<?> entityClass;
  @Nullable
  protected final MethodHandle naturalIdFieldAccess;
  protected final MethodHandle versionGetterAccess;
  protected final MethodHandle versionSetterAccess;
  protected final MethodHandle idGetterAccess;
  protected final MethodHandle idSetterAccess;
  protected final EntityPersister persister;
  protected final FolderGenerator folderGenerator;
  protected final FileGenerator fileGenerator;
  protected final LuceneDocumentExtractor luceneExtractor;
  protected final Map<LifeCycle, Set<MethodHandle>> lifecycleMethods;
  protected final Map<Field, PropertyPersister> propertyPersisters;
  protected final Set<ToOneRelation> toOneRelations;
  protected final Set<ToManyRelation> toManyRelations;
  protected final Set<ToOneChildRelation> toOneChildRelations;
  protected final Set<ToManyChildRelation> toManyChildRelations;
  protected final Set<Relation> allRelations;
  protected final Set<Relation> childRelations;
  protected final Set<Relation> normalRelations;
  protected final Set<Query<?, ?>> queries;

  public EntityDescriptor(Builder b) {
    this.entityClass = b.entityClass;
    this.persister = b.persister;
    this.idGetterAccess = b.idGetterAccess;
    this.idSetterAccess = b.idSetterAccess;
    this.naturalIdFieldAccess = b.naturalIdFieldAccess;
    this.versionGetterAccess = b.versionGetterAccess;
    this.versionSetterAccess = b.versionSetterAccess;
    this.lifecycleMethods = Collections.unmodifiableMap(b.lifecycleMethods);
    this.propertyPersisters = Collections.unmodifiableMap(b.propertyPersisters);
    this.toOneRelations = Collections.unmodifiableSet(b.toOneRelations);
    this.toManyRelations = Collections.unmodifiableSet(b.toManyRelations);
    this.toOneChildRelations = Collections.unmodifiableSet(b.toOneChildRelations);
    this.toManyChildRelations = Collections.unmodifiableSet(b.toManyChildRelations);
    this.folderGenerator = b.folderGenerator;
    this.fileGenerator = b.fileGenerator;
    this.luceneExtractor = b.extractor;
    this.queries = Collections.unmodifiableSet(b.queries);

    HashSet<Relation> allRels = new HashSet<>();
    allRels.addAll(toManyChildRelations);
    allRels.addAll(toManyRelations);
    allRels.addAll(toOneChildRelations);
    allRels.addAll(toOneRelations);
    this.allRelations = Collections.unmodifiableSet(allRels);

    HashSet<Relation> childRelations = new HashSet<>();
    childRelations.addAll(toManyChildRelations);
    childRelations.addAll(toOneChildRelations);
    this.childRelations = Collections.unmodifiableSet(childRelations);

    HashSet<Relation> normalRelations = new HashSet<>();
    normalRelations.addAll(toManyRelations);
    normalRelations.addAll(toOneRelations);
    this.normalRelations = Collections.unmodifiableSet(normalRelations);
  }

  public Class<?> getEntityClass() {
    return entityClass;
  }

  public EntityPersister getPersister() {
    return persister;
  }

  public FileGenerator getFileGenerator() {
    return fileGenerator;
  }

  public FolderGenerator getFolderGenerator() {
    return folderGenerator;
  }

  public boolean isVersioned() {
    return versionGetterAccess != null;
  }

  public boolean hasNaturalId() {
    return naturalIdFieldAccess != null;
  }

  public Optional<PropertyPersister> getPropertyPersister(Field field) {
    return Optional.ofNullable(propertyPersisters.get(field));
  }

  public MethodHandle getIdGetterAccess() {
    return idGetterAccess;
  }

  public MethodHandle getIdSetterAccess() {
    return idSetterAccess;
  }

  public Set<ToOneRelation> getToOneRelations() {
    return toOneRelations;
  }

  public Set<ToManyRelation> getToManyRelations() {
    return toManyRelations;
  }

  public Set<ToOneChildRelation> getToOneChildRelations() {
    return toOneChildRelations;
  }

  public Set<ToManyChildRelation> getToManyChildRelations() {
    return toManyChildRelations;
  }

  public Set<Relation> getNormalRelations() {
    return normalRelations;
  }

  public Set<Relation> getChildRelations() {
    return childRelations;
  }

  @Nullable
  public String getId(Object entity) {
    return invokeGetter(idGetterAccess, entity);
  }

  public long getVersion(Object entity) {
    return invokeGetter(versionGetterAccess, entity);
  }

  public void writetId(Object entity, String id) {
    invokeSetter(idSetterAccess, entity, id);
  }

  public void writeVersion(Object entity, long version) {
    invokeSetter(versionSetterAccess, entity, version);
  }

  @Nullable
  public Serializable getNaturalId(Object entity) {
    if (hasNaturalId()) {
      return invokeGetter(naturalIdFieldAccess, entity);
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T invokeGetter(MethodHandle handle, Object instance) {
    try {
      return (T) handle.invoke(instance);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T invokeSetter(MethodHandle handle, Object instance, Object param) {
    try {
      return (T) handle.invoke(instance, param);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public boolean hasIdAccess() {
    return idGetterAccess != null;
  }

  public boolean isCollectionRelation(AnnotatedMember member) {
    if (member instanceof AnnotatedField) {
      Field field = ((AnnotatedField) member).getAnnotated();
      return field.isAnnotationPresent(Children.class) || field.isAnnotationPresent(ToMany.class);
    }
    return false;
  }

  public boolean isRelation(AnnotatedMember member) {
    if (member instanceof AnnotatedField) {
      Field field = ((AnnotatedField) member).getAnnotated();
      return field.isAnnotationPresent(Child.class) ||
        field.isAnnotationPresent(Children.class) ||
        field.isAnnotationPresent(ToOne.class) ||
        field.isAnnotationPresent(ToMany.class);
    }
    return false;
  }

  /**
   * @param property field name
   * @return the persister if present
   * @throws IllegalStateException when 2 fields have the same name.
   */
  public Optional<PropertyPersister> getPropertyPersister(String property) throws IllegalStateException {
    Set<PropertyPersister> persisters = this.propertyPersisters.entrySet().stream().filter(e -> e.getKey().getName().equals(property)).map(Map.Entry::getValue).collect(Collectors.toSet());
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

  public Set<MethodHandle> getLifeCycleMethods(LifeCycle lifeCycle) {
    Set<MethodHandle> methodHandles = this.lifecycleMethods.get(lifeCycle);
    methodHandles = methodHandles == null ? Collections.emptySet() : methodHandles;
    return methodHandles;
  }

  public Set<Relation> getAllRelations() {
    return allRelations;
  }

  public LuceneDocumentExtractor getLuceneExtractor() {
    return luceneExtractor;
  }

  public Set<Query<?, ?>> getQueries() {
    return queries;
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
