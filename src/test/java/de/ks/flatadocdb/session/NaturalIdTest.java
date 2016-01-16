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
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class NaturalIdTest {

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
  public void testNaturalIdTwice() throws Exception {
    TestEntity test = new TestEntity("test");
    RelationOwner owner = new RelationOwner("test");

    Session session = new Session(metamodel, repository);
    session.persist(test);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    RelationOwner ownerReloaded = session.findByNaturalId(RelationOwner.class, "test");
    TestEntity testReloaded = session.findByNaturalId(TestEntity.class, "test");
    session.prepare();
    session.commit();

    assertEquals(test, testReloaded);
    assertEquals(owner, ownerReloaded);
    assertNotEquals(ownerReloaded.getClass(), testReloaded.getClass());
  }
}
