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
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.Related;
import de.ks.flatadocdb.session.RelationOwner;
import de.ks.flatadocdb.session.Session;
import de.ks.flatadocdb.session.SessionFriend;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class RelationTest {

  private MetaModel metamodel;
  private GlobalIndex index;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    metamodel = new MetaModel();
    metamodel.addEntity(RelationOwner.class);
    metamodel.addEntity(Related.class);

    repository = tempRepository.getRepository();
    index = new GlobalIndex(repository, metamodel);
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


    Session session = new Session(metamodel, repository, index);
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

    Session session = new Session(metamodel, repository, index);
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


    Session session = new Session(metamodel, repository, index);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository, index);
    owner = session.findById(RelationOwner.class, owner.getId()).get();
    assertEquals(1, new SessionFriend(session).getEntries().size());

    owner.getRelatedList().toString();//lazy load
    assertEquals(2, new SessionFriend(session).getEntries().size());

    owner.getChild().toString();//lazy load
    assertEquals(3, new SessionFriend(session).getEntries().size());
  }
}
