/**
 * Copyright [2016] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RenamingTest {

  private MetaModel metamodel;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(TestEntity.class);
    metamodel.addEntity(RelationOwner.class);
    metamodel.addEntity(Related.class);

  }

  @Test
  public void testRenameEntity() throws Exception {
    TestEntity test = new TestEntity("test");

    Session session = new Session(metamodel, repository);
    session.persist(test);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    TestEntity reloaded = session.findById(test.getId());
    reloaded.setName("huhu");
    session.prepare();
    session.commit();

    Collection<IndexElement> indexElements = repository.getIndex().getAllOf(TestEntity.class);
    assertEquals(1, indexElements.size());
    IndexElement element = indexElements.iterator().next();
    assertEquals("huhu", element.getNaturalId());

    Path repoPath = tempRepository.getPath();
    List<Path> folders = Files.list(repoPath.resolve(TestEntity.class.getSimpleName())).collect(Collectors.toList());
    assertEquals(1, folders.size());
    assertEquals("huhu.json", folders.iterator().next().getFileName().toString());
  }

  @Test
  public void testRenameEntityWithChild() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related child = new Related("child");
    owner.setChild(child);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    RelationOwner reloaded = session.findById(owner.getId());
    reloaded.setName("huhu");
    session.prepare();
    session.commit();

    Collection<IndexElement> indexElements = repository.getIndex().getAllOf(RelationOwner.class);
    assertEquals(1, indexElements.size());
    IndexElement element = indexElements.iterator().next();
    assertEquals("huhu", element.getNaturalId());

    indexElements = repository.getIndex().getAllOf(Related.class);
    assertEquals(1, indexElements.size());
    element = indexElements.iterator().next();
    assertEquals("child", element.getNaturalId());

    Path repoPath = tempRepository.getPath();
    List<Path> folders = Files.list(repoPath.resolve(RelationOwner.class.getSimpleName())).collect(Collectors.toList());
    folders.sort(Comparator.comparing(p -> p.getFileName().toString()));
    assertEquals(2, folders.size());
    assertEquals("huhu.json", folders.get(1).getFileName().toString());
  }
}
