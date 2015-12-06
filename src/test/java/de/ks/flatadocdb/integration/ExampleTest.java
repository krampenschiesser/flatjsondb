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

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.session.SessionFactory;
import de.ks.flatadocdb.util.DeleteDir;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExampleTest {
  private Path myRepoPath;

  @Before
  public void setUp() throws Exception {
    String tmpDir = System.getProperty("java.io.tmpdir");
    myRepoPath = Paths.get(tmpDir, "myRepoPath");
    new DeleteDir(myRepoPath).delete();
    Files.createDirectories(myRepoPath);
  }

  @Test
  public void testExample() throws Exception {
    //open a repository with a java.nio.Path
    Repository repository = new Repository(myRepoPath);

    //open a new session factory
    //the factory needs at least one repository and a package to scan or 1-n entity classes
    //a factory needs to be closed again to release resources(mainly lucene index)
    try (SessionFactory factory = new SessionFactory(repository, ExampleEntity.class)) {
      factory.transactedSession(session -> {
        ExampleEntity entity = new ExampleEntity("Hello world!");
        assert entity.getId() == null;
        session.persist(entity);
        assert entity.getId() != null;//the unique id is written into the entity with the persist call
      });

      //modify entity
      factory.transactedSession(session -> {
        ExampleEntity entity = session.findByNaturalId(ExampleEntity.class, "Hello world!");
        entity.setAttribute("Coffee");

        ExampleEntity entity2 = session.findByNaturalId(ExampleEntity.class, "Hello world!");
        assert entity == entity2;//same instance always returned
      });

      //read entity
      ExampleEntity entity = factory.transactedSessionRead(session -> session.findByNaturalId(ExampleEntity.class, "Hello world!"));
      assert "Coffee".equals(entity.getAttribute());//entity is now detached but can still be used.

      //delete enity again
      factory.transactedSession(session -> {
        ExampleEntity reloaded = session.findByNaturalId(ExampleEntity.class, "Hello world!");
        session.remove(reloaded);
      });
    }
//    factory.close(); if we don't use a try-with-resource block we need to close the factory manually
  }
}
