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

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultFolderGeneratorTest {
  @Rule
  public final TempRepository tempRepository = new TempRepository();

  @Test
  public void testDefaultFolderGeneration() throws Exception {
    Repository repo = tempRepository.getRepository();
    JoinedRootFolderGenerator defaultFolderGenerator = new JoinedRootFolderGenerator();
    Path path = defaultFolderGenerator.getFolder(repo, new TestEntity("test"));
    assertEquals(tempRepository.getPath().resolve(TestEntity.class.getSimpleName()), path);
    assertTrue(path.toFile().exists());
  }

  @Test(expected = IllegalStateException.class)
  public void testRepoExists() throws Exception {
    tempRepository.getPath().resolve(TestEntity.class.getSimpleName()).toFile().createNewFile();

    Repository repo = tempRepository.getRepository();
    JoinedRootFolderGenerator defaultFolderGenerator = new JoinedRootFolderGenerator();
    defaultFolderGenerator.getFolder(repo, new TestEntity("test"));
  }
}