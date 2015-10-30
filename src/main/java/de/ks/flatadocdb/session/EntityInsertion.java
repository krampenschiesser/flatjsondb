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
import de.ks.flatadocdb.exception.AggregateException;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EntityInsertion extends SessionAction {
  private static final Logger log = LoggerFactory.getLogger(EntityInsertion.class);

  private final Path flushPath;
  private final List<Runnable> rollbacks = new LinkedList<>();

  public EntityInsertion(Repository repository, SessionEntry sessionEntry, EntityDescriptor entityDescriptor, Path flushPath) {
    super(repository, sessionEntry);
    this.flushPath = flushPath;
  }

  @Override
  public void prepare(Session session) {
    checkVersionIncrement(sessionEntry.getCompletePath(), sessionEntry.getVersion());
    checkNoFlushFileExists(sessionEntry.getFolder(), sessionEntry.getFileName());

    EntityPersister persister = sessionEntry.getEntityDescriptor().getPersister();
    byte[] fileContents = persister.createFileContents(repository, sessionEntry.getEntityDescriptor(), sessionEntry.getObject());

    writeFlushFile(fileContents);

    byte[] md5 = DigestUtils.md5(fileContents);
    sessionEntry.setMd5(md5);

    checkAppendToComplete(sessionEntry.getCompletePath());//better to use Filelock if possible
  }

  @Override
  public void commit(Session session) {
    moveFlushFile(flushPath);
    addToIndex(session);
  }

  private void addToIndex(Session session) {
    session.globalIndex.addEntry(new IndexElement(repository, sessionEntry.getCompletePath(), sessionEntry.getId(), sessionEntry.getNaturalId(), sessionEntry.getObject().getClass()));
  }

  @Override
  public void rollback(Session session) {
    ArrayList<Exception> exceptions = new ArrayList<>();

    for (Runnable rollback : rollbacks) {
      try {
        rollback.run();
      } catch (Exception e) {
        log.error("Got exception during rollback: ", e);
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw new AggregateException(exceptions);
    }
  }

}
