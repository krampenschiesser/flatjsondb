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
package de.ks.flatadocdb.session;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.apache.commons.codec.digest.DigestUtils;

public class EntityUpdate extends SessionAction {
  public EntityUpdate(Repository repository, SessionEntry sessionEntry) {
    super(repository, sessionEntry);
  }

  @Override
  public void prepare(Session session) {
    EntityDescriptor entityDescriptor = sessionEntry.getEntityDescriptor();
    Object entity = sessionEntry.getObject();

    checkVersionIncrement(sessionEntry.getCompletePath(), sessionEntry.getVersion());
    checkNoFlushFileExists(getFlushPath());

    long version = entityDescriptor.getVersion(entity);
    entityDescriptor.writeVersion(entity, version + 1);
    sessionEntry.version++;

    executeLifecycleAction(LifeCycle.PRE_UPDATE);

    EntityPersister persister = entityDescriptor.getPersister();
    byte[] fileContents = persister.createFileContents(repository, entityDescriptor, entity);

    writeFlushFile(fileContents);

    byte[] md5 = DigestUtils.md5(fileContents);
    sessionEntry.setMd5(md5);


    checkAppendToComplete(sessionEntry.getCompletePath());//better to use Filelock if possible
  }

  @Override
  public void commit(Session session) {
    moveFlushFile(getFlushPath());

    executeLifecycleAction(LifeCycle.POST_UPDATE);

    session.globalIndex.updateEntry(sessionEntry);
    session.luceneUpdates.add(index -> index.updateEntry(sessionEntry));
  }
}
