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

import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.metamodel.EntityDescriptor;
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultEntityPersisterTest {
  @Rule
  public TempRepository tempRepository = new TempRepository();
  private MetaModel metaModel;

  @Before
  public void setUp() throws Exception {
    metaModel = new MetaModel();
    metaModel.addEntity(TestEntity.class);
  }

  @Test
  public void testStore() throws Exception {
    DefaultEntityPersister persister = new DefaultEntityPersister();
    TestEntity testEntity = new TestEntity("Hallo welt");
    EntityDescriptor entityDescriptor = metaModel.getEntityDescriptor(TestEntity.class);

    String fileName = entityDescriptor.getFileGenerator().getFileName(tempRepository.getRepository(), entityDescriptor, testEntity);
    Path folder = entityDescriptor.getFolderGenerator().getFolder(tempRepository.getRepository(), testEntity);

    Path target = folder.resolve(fileName);
    byte[] contents = persister.createFileContents(tempRepository.getRepository(), entityDescriptor, testEntity);
    Files.write(target, contents);

    assertTrue(target.toFile().exists());

    assertTrue(Files.readAllLines(target).stream().filter(line -> line.contains("Hallo welt")).findFirst().isPresent());

    Object load = persister.load(tempRepository.getRepository(), entityDescriptor, target);
    assertEquals(testEntity, load);
  }
}