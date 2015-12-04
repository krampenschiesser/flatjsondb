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
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LuceneIndex implements Index {
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

  @Override
  public void addEntry(SessionEntry sessionEntry) {
    new LuceneWrite(writer -> writeEntry(sessionEntry, writer));
  }

  @Override
  public void updateEntry(SessionEntry sessionEntry) {
    new LuceneWrite(writer -> {
      deleteEntry(sessionEntry, writer);
      writeEntry(sessionEntry, writer);
    });
  }

  @Override
  public void removeEntry(SessionEntry sessionEntry) {
    new LuceneWrite(writer -> deleteEntry(sessionEntry, writer));
  }

  protected void deleteEntry(SessionEntry sessionEntry, IndexWriter writer) throws IOException {
    writer.deleteDocuments(new Term(StandardLuceneFields.ID.name(), sessionEntry.getId()));
  }

  protected void writeEntry(SessionEntry sessionEntry, IndexWriter writer) throws IOException {
    LuceneDocumentExtractor luceneExtractor = sessionEntry.getEntityDescriptor().getLuceneExtractor();
    @SuppressWarnings("unchecked")
    Document document = luceneExtractor.createDocument(sessionEntry.getObject());
    if (document == null) {
      document = new Document();
    }
    for (StandardLuceneFields luceneField : StandardLuceneFields.values()) {
      String key = luceneField.name();
      if (document.getField(key) != null) {
        document.removeField(key);
      }
    }
    document.add(StandardLuceneFields.ID.create(sessionEntry.getId()));
    document.add(StandardLuceneFields.FILENAME.create(sessionEntry.getFileName()));
    document.add(StandardLuceneFields.NATURAL_ID.create(String.valueOf(sessionEntry.getNaturalId())));

    writer.addDocument(document);
  }

  public Directory getDirectory() {
    return directory;
  }

  @FunctionalInterface
  interface LuceneWriteConsumer {
    void apply(IndexWriter writer) throws IOException;
  }

  class LuceneWrite {
    private final LuceneWriteConsumer consumer;

    public LuceneWrite(LuceneWriteConsumer consumer) {
      this.consumer = consumer;
      execute();
    }

    private void execute() {
      try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
          consumer.apply(indexWriter);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
