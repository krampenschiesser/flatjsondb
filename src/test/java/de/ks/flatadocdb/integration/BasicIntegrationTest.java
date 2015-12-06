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
package de.ks.flatadocdb.integration;

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.SessionFactory;
import de.ks.flatadocdb.util.DeleteDir;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class BasicIntegrationTest {

  private SessionFactory sessionFactory;
  private Repository repository;

  @Before
  public void setUp() throws Exception {
    Path repoPath = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "tempRepo");
    new DeleteDir(repoPath).delete();
    Files.createDirectories(repoPath);

    repository = new Repository(repoPath, new MetaModel());
    sessionFactory = new SessionFactory(repository, TestEntity.class.getPackage().getName());
  }

  @Test
  public void testBasicIntegration() throws Exception {
    sessionFactory.transactedSession(repository, session -> {
      TestEntity entity = new TestEntity("blubber").setAttribute("Steak");
      session.persist(entity);
    });

    TestEntity entity = sessionFactory.transactedSessionRead(repository, session -> session.findByNaturalId(TestEntity.class, "blubber").get());
    assertEquals("blubber", entity.getName());
    assertEquals("Steak", entity.getAttribute());
  }
}
