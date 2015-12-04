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
import de.ks.flatadocdb.index.LuceneIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class RelationCollectionTest {

  private MetaModel metamodel;
  private GlobalIndex index;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();
  private LuceneIndex luceneIndex;

  @Before
  public void setUp() throws Exception {
    metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);

    repository = tempRepository.getRepository();
    index = new GlobalIndex(repository, metamodel);
    luceneIndex = new LuceneIndex(repository);
  }

  @Test
  public void testRelationList() throws Exception {
    Session session = new Session(metamodel, repository, index, luceneIndex);
    ArrayList<String> ids = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      TestEntity testEntity = new TestEntity("Schnitzel " + (i + 1));
      session.persist(testEntity);
      ids.add(testEntity.getId());
    }

    RelationList<TestEntity> list = new RelationList<>(ids, session);
    assertEquals(0, list.delegate.size());
    assertEquals(5, list.size());//load
    assertEquals(5, list.delegate.size());

    session.prepare();
    session.commit();
  }

  @Test
  public void testRelationSet() throws Exception {
    Session session = new Session(metamodel, repository, index, luceneIndex);
    HashSet<String> ids = new HashSet<>();

    for (int i = 0; i < 5; i++) {
      TestEntity testEntity = new TestEntity("Schnitzel " + (i - (i % 2) + 1));
      session.persist(testEntity);
      if (testEntity.getId() != null) {
        ids.add(testEntity.getId());
      }
    }

    RelationSet<TestEntity> list = new RelationSet<>(ids, session);
    assertEquals(0, list.delegate.size());
    assertEquals(3, list.size());//load
    assertEquals(3, list.delegate.size());

    session.prepare();
    session.commit();
  }
}