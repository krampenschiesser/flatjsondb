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
package de.ks.flatadocdb.session.relation;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.Related;
import de.ks.flatadocdb.session.RelationOwner;
import de.ks.flatadocdb.session.Session;
import de.ks.flatadocdb.session.SessionFriend;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;

import static org.junit.Assert.*;

public class RelationTest {

  private MetaModel metamodel;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(RelationOwner.class);
    metamodel.addEntity(Related.class);

  }

  @Test
  public void testPersistRelation() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("related");
    Related child = new Related("child");
    owner.setChild(child);
    owner.getRelatedList().add(related);
    owner.getRelatedList().add(related);
    owner.getRelatedSet().add(related);


    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    assertNotNull(owner.getId());
    assertNotNull(related.getId());
    assertNotNull(child.getId());

    Path relatedPath = repository.getPath().resolve(Related.class.getSimpleName()).resolve("related.json");
    assertTrue(relatedPath.toFile().exists());

    Path childPath = repository.getPath().resolve(RelationOwner.class.getSimpleName()).resolve(Related.class.getSimpleName()).resolve("child.json");
    assertTrue(childPath.toFile().exists());
  }

  /**
   * if same entity added as child and normal relation we only persist it as normal entity, not as child
   *
   * @throws Exception
   */
  @Test
  public void testPersistNoChild() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related child = new Related("child");
    owner.setChild(child);
    owner.getRelatedList().add(child);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    assertNotNull(owner.getId());
    assertNotNull(child.getId());

    Path relatedPath = repository.getPath().resolve(Related.class.getSimpleName()).resolve("child.json");
    assertTrue(relatedPath.toFile().exists());

    Path childPath = repository.getPath().resolve(RelationOwner.class.getSimpleName()).resolve(Related.class.getSimpleName()).resolve("child.json");
    assertFalse(childPath.toFile().exists());
  }

  @Test
  public void testLoadLazyRelation() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("related");
    Related child = new Related("child");
    owner.setChild(child);
    owner.getRelatedList().add(related);
    owner.getRelatedList().add(related);


    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    owner = session.findById(RelationOwner.class, owner.getId());
    assertEquals(1, new SessionFriend(session).getEntries().size());

    assertEquals(2, owner.getRelatedList().size());
    assertEquals(2, new SessionFriend(session).getEntries().size());

    owner.getChild().toString();//lazy load
    assertEquals(3, new SessionFriend(session).getEntries().size());
    assertNotNull(owner.getChild());
    assertEquals("child", owner.getChild().getName());
  }

  @Test
  public void testLoadEagerRelation() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("related");
    owner.getRelatedSet().add(related);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    owner = session.findById(RelationOwner.class, owner.getId());
    assertEquals(2, new SessionFriend(session).getEntries().size());
  }

  @Test
  public void testDeleteRelation() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("related");
    owner.getRelatedList().add(related);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    session.remove(session.findById(related.getId()));
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    owner = (RelationOwner) session.findById(owner.getId());
    assertTrue(owner.getRelatedList().isEmpty());
    session.prepare();
    session.commit();
  }

  @Test
  public void testDeleteChildWithParent() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("child");
    owner.setChild(related);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository);
    session.remove(session.findById(owner.getId()));
    session.prepare();
    session.commit();

    Collection<IndexElement> owners = repository.getIndex().getAllOf(RelationOwner.class);
    Collection<IndexElement> children = repository.getIndex().getAllOf(Related.class);
    assertThat(owners, Matchers.empty());
    assertThat(children, Matchers.empty());
  }

  @Test
  public void testDeleteChildrenWithParent() throws Exception {
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("child");
    owner.getRelatedChildren().add(related);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository);
    session.remove(session.findById(owner.getId()));
    session.prepare();
    session.commit();

    Collection<IndexElement> owners = repository.getIndex().getAllOf(RelationOwner.class);
    Collection<IndexElement> children = repository.getIndex().getAllOf(Related.class);
    assertThat(owners, Matchers.empty());
    assertThat(children, Matchers.empty());
  }
}
