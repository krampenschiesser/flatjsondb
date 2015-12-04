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

import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;

public class LuceneIndexTest {

  private MetaModel metamodel;
  private GlobalIndex index;
  private LuceneIndex luceneIndex;
  private Repository repository;
  private Path path;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);

    repository = tempRepository.getRepository();
    path = tempRepository.getPath();
    index = new GlobalIndex(repository, metamodel);
    luceneIndex = new LuceneIndex(repository);
  }

  @Test
  public void testAddToIndex() throws Exception {
    TestEntity testEntity = new TestEntity("Schnitzel");
    Session session = new Session(metamodel, repository, index, luceneIndex);
    session.persist(testEntity);
    session.prepare();
    session.commit();


    session = new Session(metamodel, repository, index, luceneIndex);
    Document document = session.lucene(searcher -> {
      TermQuery termQuery = new TermQuery(new Term(StandardLuceneFields.NATURAL_ID.name(), "Schnitzel"));
      TopDocs search = searcher.search(termQuery, 1);
      ScoreDoc scoreDoc = search.scoreDocs[0];
      Document doc = searcher.doc(scoreDoc.doc);
      return doc;
    });
    assertNotNull(document);
  }
}
