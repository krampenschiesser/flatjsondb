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

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.DeleteDir;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.defaults.DefaultFileGenerator;
import de.ks.flatadocdb.index.LocalIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.*;

public class SessionTest {

  private MetaModel metamodel;
  private LocalIndex index;
  private Repository repository;
  private Path path;

  @Before
  public void setUp() throws Exception {
    metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);

    path = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "testRepository");
    new DeleteDir(path).delete();

    repository = new Repository(path);
    index = new LocalIndex();
  }

  @Test
  public void testFileWriting() throws Exception {
    Session session1 = new Session(metamodel, repository, index);
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
    Session session1 = new Session(metamodel, repository, index);
    Session session2 = new Session(metamodel, repository, index);
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
}
