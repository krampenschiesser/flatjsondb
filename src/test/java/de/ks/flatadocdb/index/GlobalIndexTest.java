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

package de.ks.flatadocdb.index;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class GlobalIndexTest {
  public static final int COUNT = 5;
  @Rule
  public TempRepository tempRepository = new TempRepository();
  private Repository repository;
  private MetaModel metaModel;
  private GlobalIndex index;

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metaModel = new MetaModel();
    metaModel.addEntity(TestEntity.class);
    index = new GlobalIndex(repository, metaModel);

    for (int i = 0; i < COUNT; i++) {
      TestEntity entity = new TestEntity("test" + (i + 1));
      Session session = new Session(metaModel, repository, index);
      session.persist(entity);
      session.prepare();
      session.commit();
    }
    index = new GlobalIndex(repository, metaModel);
  }

  @Test
  public void testRecreateIndex() throws Exception {
    index.recreate();

    Collection<IndexElement> elements = index.getAllOf(TestEntity.class);
    assertEquals(COUNT, elements.size());
  }
}