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
package de.ks.flatadocdb.session.dirtycheck;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.SessionEntry;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Very simple dirty checker right now.
 * Need to balloon it up to something hibernate uses, storing the initial state and then comparing all fields
 */
public class DirtyChecker {
  private final Repository repository;
  private final MetaModel metaModel;
  private final Set<Object> insertions = new HashSet<>();

  public DirtyChecker(Repository repository, MetaModel metaModel) {
    this.repository = repository;
    this.metaModel = metaModel;
  }

  public void trackLoad(SessionEntry sessionEntry) {
    //maybe store initial state und do comparison like hibernate?
  }

  public void trackPersist(SessionEntry sessionEntry) {
    insertions.add(sessionEntry.getObject());
  }

  public void trackDelete(SessionEntry sessionEntry) {
  }

  public Collection<SessionEntry> findDirty(Collection<SessionEntry> values) {
    return values.stream().filter(e -> !insertions.contains(e.getObject())).filter(e -> {
      EntityDescriptor entityDescriptor = e.getEntityDescriptor();
      EntityPersister persister = entityDescriptor.getPersister();
      byte[] fileContents = persister.createFileContents(repository, entityDescriptor, e.getObject());
      byte[] md5 = DigestUtils.md5(fileContents);
      return !Arrays.equals(md5, e.getMd5());
    }).collect(Collectors.toSet());
  }
}