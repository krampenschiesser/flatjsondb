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
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.index.LuceneIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;

public class LifeCycleTest {
  @Rule
  public TempRepository tempRepository = new TempRepository();
  private MetaModel metamodel;

  private GlobalIndex index;
  private Repository repository;
  private Path path;
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
  public void testPreUpdate() throws Exception {
    TestEntity entity = new TestEntity("Steak");

    Session session = new Session(metamodel, repository, index, luceneIndex);
    session.persist(entity);
    session.prepare();

    assertNotNull(entity.getUpdateTime());
  }
}
