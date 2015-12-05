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
import de.ks.flatadocdb.exception.StaleObjectFileException;
import de.ks.flatadocdb.exception.StaleObjectStateException;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.index.LuceneIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class SessionTest {

  private MetaModel metamodel;
  private GlobalIndex index;
  private Repository repository;
  private Path path;

  @Rule
  public TempRepository tempRepository = new TempRepository();
  private LuceneIndex luceneIndex;

  @Before
  public void setUp() throws Exception {
    metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);

    repository = tempRepository.getRepository();
    path = tempRepository.getPath();
    index = new GlobalIndex(repository, metamodel);
    luceneIndex = new LuceneIndex(repository);
  }

  @After
  public void tearDown() throws Exception {
    if (luceneIndex != null) {
      luceneIndex.close();
    }
    if (repository != null) {
      repository.close();
    }
  }

  @Test
  public void testFileWriting() throws Exception {
    Session session1 = new Session(metamodel, repository, index, luceneIndex);
    TestEntity testEntity = new TestEntity("Schnitzel");
    session1.persist(testEntity);

    session1.prepare();
    Path entityFolder = path.resolve(TestEntity.class.getSimpleName());
    assertTrue(entityFolder.toFile().exists());
    assertTrue(entityFolder.toFile().isDirectory());
    File[] files = entityFolder.toFile().listFiles();
    assertThat(files, Matchers.arrayWithSize(1));

    session1.commit();
    Path entityFile = entityFolder.resolve("Schnitzel." + DefaultFileGenerator.EXTENSION);
    assertTrue(entityFile.toFile().exists());
    assertFalse(files[0].exists());
  }

  @Test
  public void testLocalSessionView() {
    Session session1 = new Session(metamodel, repository, index, luceneIndex);
    Session session2 = new Session(metamodel, repository, index, luceneIndex);
    TestEntity testEntity = new TestEntity("Schnitzel");

    session1.persist(testEntity);
    assertNotNull(testEntity.getId());

    Optional<TestEntity> result = session1.findByNaturalId(TestEntity.class, "Schnitzel");
    assertTrue(result.isPresent());

    result = session2.findByNaturalId(TestEntity.class, "Schnitzel");
    assertFalse(result.isPresent());

    session1.prepare();
    session1.commit();

    result = session2.findByNaturalId(TestEntity.class, "Schnitzel");
    assertTrue(result.isPresent());
  }

  @Test
  public void testFlushFileExists() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");

    Session session1 = new Session(metamodel, repository, index, luceneIndex);
    session1.persist(testEntity);
    Session session2 = new Session(metamodel, repository, index, luceneIndex);
    session2.persist(testEntity);
    session1.prepare();
    try {
      session2.prepare();
      fail("Got no exception from second flush file!");
    } catch (StaleObjectFileException e) {
      //ok
    }
  }

  @Test
  public void testDoublePersist() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository, index, luceneIndex);

    session.persist(testEntity);
    session.persist(testEntity);
    assertEquals(1, session.actions.size());
  }

  @Test
  public void testTwoSessionCommit() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");

    Session session1 = new Session(metamodel, repository, index, luceneIndex);
    Session session2 = new Session(metamodel, repository, index, luceneIndex);
    session1.persist(testEntity);
    session2.persist(testEntity);
    session1.prepare();
    session1.commit();

    try {
      session2.prepare();
      fail("No " + StaleObjectStateException.class.getSimpleName() + " although version was increased by session1");
    } catch (StaleObjectFileException e) {
      //ok
    }
  }

  @Test
  public void testRollBack() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository, index, luceneIndex);

    session.persist(testEntity);
    session.prepare();
    SessionEntry entry = session.entriesById.values().iterator().next();
    File[] files = entry.getFolder().toFile().listFiles();
    assertThat(files, Matchers.not(Matchers.emptyArray()));
    assertThat(files[0].getName(), Matchers.startsWith("."));

    session.rollback();
    files = entry.getFolder().toFile().listFiles();
    assertThat(files, Matchers.emptyArray());
  }

  @Test
  public void testVersionIncrement2Sessions() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");

    Session session1 = new Session(metamodel, repository, index, luceneIndex);
    session1.persist(testEntity);
    session1.prepare();
    session1.commit();

    session1 = new Session(metamodel, repository, index, luceneIndex);
    Session session2 = new Session(metamodel, repository, index, luceneIndex);

    TestEntity first = session1.findById(TestEntity.class, testEntity.getId()).get();
    TestEntity second = session2.findById(TestEntity.class, testEntity.getId()).get();

    second.setAttribute("bla");
    session2.prepare();
    session2.commit();

    first.setAttribute("bla");
    try {
      session1.prepare();
      fail("No " + StaleObjectStateException.class.getSimpleName() + " although version was increased by session2");
    } catch (StaleObjectStateException e) {
      //ok
    }
  }

  @Test
  public void testVersionIncrement() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");

    Session session = new Session(metamodel, repository, index, luceneIndex);
    session.persist(testEntity);
    session.prepare();
    session.commit();

    for (int i = 0; i < 5; i++) {
      session = new Session(metamodel, repository, index, luceneIndex);
      testEntity = session.findByNaturalId(TestEntity.class, "Schnitzel").get();
      testEntity.setAttribute("att" + i);
      session.prepare();
      session.commit();
    }
    assertEquals(5, testEntity.getVersion());
  }

  @Test
  public void testSameInstance1Session() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository, index, luceneIndex);
    session.persist(testEntity);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository, index, luceneIndex);
    testEntity = session.findByNaturalId(TestEntity.class, "Schnitzel").get();
    TestEntity testEntity2 = session.findByNaturalId(TestEntity.class, "Schnitzel").get();
    assertSame(testEntity, testEntity2);
  }

  @Test
  public void testRemove() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository, index, luceneIndex);
    session.persist(testEntity);
    session.prepare();
    session.commit();

    IndexElement indexElement = index.getById(testEntity.getId());
    assertNotNull(indexElement);

    Path pathInRepository = indexElement.getPathInRepository();
    assertTrue(pathInRepository.toFile().exists());

    session = new Session(metamodel, repository, index, luceneIndex);
    testEntity = session.findById(TestEntity.class, testEntity.getId()).get();
    session.remove(testEntity);
    session.prepare();
    session.commit();

    assertFalse(pathInRepository.toFile().exists());
    assertNull(index.getById(testEntity.getId()));
  }
}
