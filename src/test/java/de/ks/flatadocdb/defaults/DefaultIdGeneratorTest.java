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

package de.ks.flatadocdb.defaults;

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.util.DeleteDir;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultIdGeneratorTest {
  private static final Logger log = LoggerFactory.getLogger(DefaultIdGeneratorTest.class);
  private String tmpDir;
  private File file;

  @Before
  public void setUp() throws Exception {
    tmpDir = StandardSystemProperty.JAVA_IO_TMPDIR.value();
    File testDir = new File(tmpDir, "sha1test");
    new DeleteDir(testDir).delete();

    testDir.mkdir();
    file = new File(testDir, "bla.txt");
    file.createNewFile();
  }

  @Test
  public void testRelativePath() throws Exception {
    String relativePath = new DefaultIdGenerator().getRelativePath(Paths.get(tmpDir), file.toPath());
    assertEquals("sha1test/bla.txt", relativePath);
  }

  @Test
  public void testSha1() throws Exception {
    String sha1 = new DefaultIdGenerator().getSha1Hash(Paths.get(tmpDir), file.toPath());
    assertNotNull(sha1);
    assertEquals("11c2d1d6d97e09f45a626de23f37e9d8d4817138", sha1);
  }
}