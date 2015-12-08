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
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.defaults.DefaultFileGenerator;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class SessionRollbackTest {
  private MetaModel metamodel;
  private Repository repository;
  private Path path;
  private GlobalIndex index;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(TestEntity.class);
    path = tempRepository.getPath();
    index = repository.getIndex();
  }

  @Test
  public void testRollbackOnlyInFlush() throws Exception {
    Session session = new Session(metamodel, repository);
    session.actions.add(new SessionAction(repository, null) {
      @Override
      public void prepare(Session session) {
        throw new RuntimeException("Rollback please");
      }

      @Override
      public void commit(Session session) {

      }
    });
    TestEntity testEntity = new TestEntity("Schnitzel");
    session.persist(testEntity);

    try {
      session.prepare();
    } catch (RuntimeException e) {
      //ok
    }
    assertTrue(session.isRollbackonly());
    session.commit();
  }

  @Test
  public void testRollbackDuringCommit() throws Exception {
    Session session = new Session(metamodel, repository);
    session.actions.add(new SessionAction(repository, null) {
      @Override
      public void prepare(Session session) {
      }

      @Override
      public void commit(Session session) {
        throw new RuntimeException("Rollback please");
      }
    });
    TestEntity testEntity = new TestEntity("Schnitzel");
    session.persist(testEntity);
    session.prepare();

    try {
      session.commit();
    } catch (RuntimeException e) {
      //ok
    }
    assertTrue(session.isRollbackonly());
  }

  @Test
  public void testRollbackAfterCommit() throws Exception {
    Session session = new Session(metamodel, repository);
    TestEntity testEntity = new TestEntity("Schnitzel");
    session.persist(testEntity);
    session.prepare();
    session.actions.add(new SessionAction(repository, null) {
      @Override
      public void prepare(Session session) {
      }

      @Override
      public void commit(Session session) {
        throw new RuntimeException("Rollback please");
      }
    });

    try {
      session.commit();
    } catch (RuntimeException e) {
      //ok
    }
    assertTrue(session.isRollbackonly());

    Path entityFolder = path.resolve(TestEntity.class.getSimpleName());
    Path entityFile = entityFolder.resolve("Schnitzel." + DefaultFileGenerator.EXTENSION);
    assertTrue(Files.exists(entityFile));

    session.rollback();
  }
}
