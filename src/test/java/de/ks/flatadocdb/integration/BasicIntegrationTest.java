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
package de.ks.flatadocdb.integration;

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.session.Session;
import de.ks.flatadocdb.session.SessionFactory;
import de.ks.flatadocdb.util.DeleteDir;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BasicIntegrationTest {

  @Before
  public void setUp() throws Exception {
    Path repoPath = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "tempRepo");
    new DeleteDir(repoPath).delete();
    Files.createDirectories(repoPath);

    Repository repository = new Repository(repoPath);
    Session session = new SessionFactory().openSession(repository);
  }

  @Test
  public void testBasicIntegration() throws Exception {

  }
}
