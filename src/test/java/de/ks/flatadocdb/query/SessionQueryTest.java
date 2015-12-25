/**
 * Copyright [2015] [Christian Loehnert]
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
package de.ks.flatadocdb.query;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.entity.BaseEntity;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.index.IndexElement;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.Session;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SessionQueryTest {

  public static final int AMOUNT = 11;
  @Rule
  public TempRepository tempRepository = new TempRepository();
  private MetaModel metamodel;
  private Repository repository;
  private GlobalIndex index;

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(TestEntity.class);
    index = repository.getIndex();
    Session session = new Session(metamodel, repository);
    for (int i = 0; i < AMOUNT; i++) {
      TestEntity testEntity = new TestEntity("Schnitzel" + i);
      testEntity.setAttribute("Att" + (i + 1));
      session.persist(testEntity);
    }
    session.prepare();
    session.commit();
  }

  @Test
  public void testIndexFilled() throws Exception {
    Map<IndexElement, Optional<String>> elements = index.getQueryElements(TestEntity.attributeQuery());
    assertNotNull(elements);
    assertEquals(AMOUNT, elements.size());
  }

  @Test
  public void testFindFromIndex() throws Exception {
    Session session = new Session(metamodel, repository);
    Collection<TestEntity> entities = session.query(TestEntity.attributeQuery(), (String str) -> str.contains("1"));
    assertEquals(3, entities.size());

    for (TestEntity entity : entities) {
      assertThat(entity.getAttribute(), Matchers.containsString("1"));
    }
  }

  @Test
  public void testFindFromSession() throws Exception {
    Session session = new Session(metamodel, repository);
    Set<TestEntity> all = index.getAllOf(TestEntity.class).stream().map(IndexElement::getId).map(id -> session.findById(TestEntity.class, id)).collect(Collectors.toSet());
    all.forEach(e -> e.setAttribute("bla"));

    Collection<TestEntity> entities = session.query(TestEntity.attributeQuery(), (String str) -> str.contains("1"));
    assertEquals(0, entities.size());
    entities = session.query(TestEntity.attributeQuery(), (String str) -> str.contains("bla"));
    assertEquals(AMOUNT, entities.size());
  }

  @Test
  public void testFindFromSessionSubset() throws Exception {
    Session session = new Session(metamodel, repository);

    TestEntity entity = session.findByNaturalId(TestEntity.class, "Schnitzel10");
    entity.setAttribute("blubb");

    Collection<TestEntity> entities = session.query(TestEntity.attributeQuery(), (String str) -> str.contains("1"));
    assertEquals(2, entities.size());
    for (TestEntity testEntity : entities) {
      assertNotEquals("Schnitzel10", testEntity.getName());
    }
  }

  @Test
  public void testQueryValues() throws Exception {
    Session session = new Session(metamodel, repository);

    TestEntity entity = session.findByNaturalId(TestEntity.class, "Schnitzel10");
    entity.setAttribute("blubb");

    Set<String> values = session.queryValues(TestEntity.attributeQuery(), s -> true);
    assertEquals(AMOUNT, values.size());
  }

  @Test
  public void testMultiQuery() throws Exception {
    Session session = new Session(metamodel, repository);

    Session.MultiQueyBuilder<TestEntity> query = session.<TestEntity>multiQuery();
    query.query(TestEntity.attributeQuery(), (String str) -> str.contains("1"));
    query.query((Query<? extends TestEntity, LocalDateTime>) BaseEntity.getCreationTimeQuery(), (LocalDateTime time) -> time.isAfter(LocalDateTime.now().minusYears(1)));

    Set<TestEntity> entities = query.find();
    assertEquals(3, entities.size());

    for (TestEntity entity : entities) {
      assertThat(entity.getAttribute(), Matchers.containsString("1"));
    }
  }
}
