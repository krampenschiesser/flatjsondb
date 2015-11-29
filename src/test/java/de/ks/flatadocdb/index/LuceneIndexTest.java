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

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.util.DeleteDir;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LuceneIndexTest {

  private LuceneIndex luceneIndex;

  @Before
  public void setUp() throws Exception {
    Path path = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "luceneDir");
    new DeleteDir(path).delete();
    Files.createDirectories(path);

    FSDirectory directory = FSDirectory.open(path);
    luceneIndex = new LuceneIndex(directory);
  }

  @Test
  public void testAddDoc() throws Exception {
    Document document = new Document();
    document.add(new StringField("id", "blubber", Field.Store.YES));
  }
}