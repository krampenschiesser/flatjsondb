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
import javassist.util.proxy.ProxyObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class LazyEntityTest {

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

  @After
  public void tearDown() throws Exception {
    if (luceneIndex != null) {
      luceneIndex.close();
    }
    if (repository != null) {
      repository.close();
    }
  }
  @Test
  public void testLazyEntity() throws Exception {
    Session session = new Session(metamodel, repository, index, luceneIndex);

    TestEntity testEntity = new TestEntity("Schnitzel ");
    session.persist(testEntity);

    TestEntity proxy = LazyEntity.proxyFor(TestEntity.class, testEntity.getId(), session);
    assertTrue(proxy instanceof TestEntity);
    assertTrue(proxy instanceof ProxyObject);

    ProxyObject proxyObject = (ProxyObject) proxy;
    LazyEntity handler = (LazyEntity) proxyObject.getHandler();
    assertNull(handler.delegate.get());

    assertNotSame(proxy, testEntity);

    assertEquals(proxy, testEntity);//trigger lazy loading
    assertNotNull(handler.delegate.get());
  }

  @Test
  public void testSetOwnerReference() throws Exception {
    Session session = new Session(metamodel, repository, index, luceneIndex);

    TestEntity testEntity = new TestEntity("Schnitzel ");
    session.persist(testEntity);

    TestOwner testOwner = new TestOwner();
    TestEntity proxy = LazyEntity.proxyFor(TestEntity.class, testEntity.getId(), session, testOwner, TestOwner.class.getDeclaredField("child"));
    testOwner.child = proxy;

    assertNotSame(testOwner.child, testEntity);
    proxy.getName();//lazy loading

    assertSame(testOwner.child, testEntity);
  }

  static class TestOwner {
    TestEntity child;
  }
}
