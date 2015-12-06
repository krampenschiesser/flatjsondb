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
import de.ks.flatadocdb.index.StandardLuceneFields;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LuceneIndexTest {

  private MetaModel metamodel;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(TestEntity.class);
  }

  @Test
  public void testAddToIndex() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository);
    session.persist(testEntity);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository);
    Document document = session.lucene(searcher -> {
      TermQuery termQuery = new TermQuery(new Term(StandardLuceneFields.NATURAL_ID.name(), "Schnitzel"));
      TopDocs search = searcher.search(termQuery, 1);
      ScoreDoc scoreDoc = search.scoreDocs[0];
      Document doc = searcher.doc(scoreDoc.doc);
      return doc;
    });
    assertNotNull(document);
  }

  @Test
  public void testUpdate() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel").setAttribute("Saftig");
    Session session = new Session(metamodel, repository);
    session.persist(testEntity);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository);
    TestEntity read = session.findById(TestEntity.class, testEntity.getId()).get();
    read.setAttribute("Zaeh");
    session.prepare();
    session.commit();

    Document document = session.lucene(searcher -> {
      TermQuery termQuery = new TermQuery(new Term(StandardLuceneFields.NATURAL_ID.name(), "Schnitzel"));
      TopDocs search = searcher.search(termQuery, 1);
      ScoreDoc scoreDoc = search.scoreDocs[0];
      Document doc = searcher.doc(scoreDoc.doc);
      return doc;
    });
    assertNotNull(document);
    assertEquals("Zaeh", document.get("attribute"));
  }

  @Test
  public void testDelete() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel").setAttribute("Saftig");
    Session session = new Session(metamodel, repository);
    session.persist(testEntity);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository);
    TestEntity read = session.findById(TestEntity.class, testEntity.getId()).get();
    session.remove(read);
    session.prepare();
    session.commit();

    int documentAmount = session.lucene(searcher -> searcher.getIndexReader().maxDoc());
    assertEquals(0, documentAmount);
  }
}
