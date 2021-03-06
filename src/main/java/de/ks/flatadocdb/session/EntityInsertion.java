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
import de.ks.flatadocdb.exception.StaleObjectFileException;
import de.ks.flatadocdb.ifc.EntityPersister;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.file.Path;

public class EntityInsertion extends SessionAction {
  public EntityInsertion(Repository repository, SessionEntry sessionEntry) {
    super(repository, sessionEntry);
  }

  @Override
  public void prepare(Session session) {
    if (sessionEntry.getCompletePath().toFile().exists()) {
      throw new StaleObjectFileException("Real file already exists" + sessionEntry.getCompletePath());
    }
    checkVersionIncrement(sessionEntry.getCompletePath(), sessionEntry.getVersion());
    checkNoFlushFileExists(getFlushPath());

    executeLifecycleAction(LifeCycle.PRE_PERSIST);
    executeLifecycleAction(LifeCycle.PRE_UPDATE);

    EntityPersister persister = sessionEntry.getEntityDescriptor().getPersister();
    byte[] fileContents = persister.createFileContents(repository, sessionEntry.getEntityDescriptor(), sessionEntry.getObject());

    writeFlushFile(fileContents);

    byte[] md5 = DigestUtils.md5(fileContents);
    sessionEntry.setMd5(md5);

    Path completePath = sessionEntry.getCompletePath();
    sessionEntry.getEntityDescriptor().writePathInRepo(sessionEntry.getObject(), completePath);

    checkAppendToComplete(sessionEntry.getCompletePath());//better to use Filelock if possible
  }

  @Override
  public void commit(Session session) {
    moveFlushFile(getFlushPath());
    addToIndex(session);
    executeLifecycleAction(LifeCycle.POST_PERSIST);
    executeLifecycleAction(LifeCycle.POST_UPDATE);
  }

  private void addToIndex(Session session) {
    session.globalIndex.addEntry(sessionEntry);
    session.luceneUpdates.add(index -> index.addEntry(sessionEntry));
  }
}
