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
package de.ks.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LuceneTaggingTest {
  private static final Logger log = LoggerFactory.getLogger(LuceneTaggingTest.class);
  private Analyzer analyzer;
  private Directory directory;

  @Before
  public void setUp() throws Exception {
    analyzer = new StandardAnalyzer();
    directory = new RAMDirectory();

  }

  @Test
  public void testTags() throws Exception {
    IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));

    List<String> allTags = Arrays.asList("bla blubb", "blubb", "blubber huhu", "bla huhu", "haha");
    for (String tag : allTags) {
      Document doc = new Document();
      doc.add(new TextField("tags", tag, Field.Store.YES));
      writer.addDocument(doc);
    }
    writer.close();


    DirectoryReader directoryReader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(directoryReader);
    String term = "BLA";
    TermQuery termQuery = new TermQuery(new Term("tags", term));
    TopDocs search = searcher.search(termQuery, 50);
    log("TermQuery", searcher, search);

    FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("tags", term));
    search = searcher.search(fuzzyQuery, 50);
    log("FuzzyQuery", searcher, search);

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new TermQuery(new Term("tags", "blubb")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("tags", "bla")), BooleanClause.Occur.SHOULD);
    BooleanQuery query = builder.build();
    search = searcher.search(query, 50);
    log("BooleanQuery", searcher, search);
  }

  public void log(String queryType, IndexSearcher searcher, TopDocs search) throws IOException {
    log.info("{}: Found total={}, maxScore={}", queryType, search.totalHits, search.getMaxScore());
    for (ScoreDoc scoreDoc : search.scoreDocs) {
      Document doc = searcher.doc(scoreDoc.doc);
      String tags = doc.get("tags");
      log.info("Doc {}, score={}, tags={}", scoreDoc.doc, scoreDoc.score, tags);
    }
  }
}
