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
import de.ks.flatadocdb.ifc.LuceneDocumentExtractor;
import de.ks.flatadocdb.session.SessionEntry;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LuceneIndex {
  public static final String LUCENE_INDEX_FOLDER = ".lucene";
  private final Directory directory;

  public LuceneIndex(Repository repository) throws RuntimeException {
    try {
      Path resolve = repository.getPath().resolve(LUCENE_INDEX_FOLDER);
      Files.createDirectories(resolve);

      this.directory = FSDirectory.open(resolve);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void addEntry(SessionEntry sessionEntry) {
    LuceneDocumentExtractor luceneExtractor = sessionEntry.getEntityDescriptor().getLuceneExtractor();

    try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
        @SuppressWarnings("unchecked")
        Document document = luceneExtractor.createDocument(sessionEntry);
        if (document == null) {
          document = new Document();
        }
        document.add(StandardLuceneFields.ID.create(sessionEntry.getId()));
        document.add(StandardLuceneFields.FILENAME.create(sessionEntry.getFileName()));

        indexWriter.addDocument(document);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
