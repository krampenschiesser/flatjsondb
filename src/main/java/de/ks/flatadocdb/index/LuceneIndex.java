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
import de.ks.flatadocdb.util.TimeProfiler;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Index managing a lucene directory.
 */
public class LuceneIndex implements Index {
  private static final Logger log = LoggerFactory.getLogger(LuceneIndex.class);

  public static final String LUCENE_INDEX_FOLDER = ".lucene";
  private final Directory directory;

  private final StandardAnalyzer analyzer;
  private final IndexWriter indexWriter;
  private volatile IndexReader indexReader;
  private final AtomicBoolean dirty = new AtomicBoolean();

  public LuceneIndex(Repository repository) throws RuntimeException {
    try {
      Path resolve = repository.getPath().resolve(LUCENE_INDEX_FOLDER);
      Files.createDirectories(resolve);

      TimeProfiler profiler = new TimeProfiler("Lucene loading").start();
      try {
        this.directory = FSDirectory.open(resolve);
        analyzer = new StandardAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriter = new IndexWriter(directory, cfg);
        reopenIndexReader();
      } finally {
        profiler.stop().logDebug(log);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected IndexReader reopenIndexReader() throws IOException {
    indexReader = DirectoryReader.open(indexWriter, true);
    return indexReader;
  }

  @Override
  public void addEntry(SessionEntry sessionEntry) {
    try {
      writeEntry(sessionEntry, indexWriter);
      makeDirty();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void updateEntry(SessionEntry sessionEntry) {
    try {
      deleteEntry(sessionEntry, indexWriter);
      writeEntry(sessionEntry, indexWriter);
      makeDirty();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void makeDirty() {
    dirty.set(true);
  }

  @Override
  public void recreate() {
    //FIXME
  }

  @Override
  public void removeEntry(SessionEntry sessionEntry) {
    try {
      deleteEntry(sessionEntry, indexWriter);
      makeDirty();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

    if (log.isTraceEnabled()) {
      document.getFields().forEach(f -> log.trace("Extracted field {} from {}({}). Vaue={}",//
        f.name(), sessionEntry.getObject(), sessionEntry.getFileName(), //
        f.stringValue().length() > 70 ? f.stringValue().substring(0, 70) : f.stringValue()));
    }
    writer.addDocument(document);
  }

  public Directory getDirectory() {
    return directory;
  }

  public IndexReader getIndexReader() {
    if (dirty.get()) {
      try {
        reopenIndexReader();
        dirty.set(true);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return indexReader;
  }

  @Override
  public void close() {
    try {
      indexWriter.close();
      indexReader.close();
      analyzer.close();
    } catch (IOException e) {
      log.error("Could not close lucene", e);
    }
  }

}
