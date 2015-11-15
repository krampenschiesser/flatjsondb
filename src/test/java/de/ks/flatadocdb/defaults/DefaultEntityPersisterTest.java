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
import de.ks.flatadocdb.session.Related;
import de.ks.flatadocdb.session.RelationOwner;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class DefaultEntityPersisterTest {
  @Rule
  public TempRepository tempRepository = new TempRepository();
  private MetaModel metaModel;
  private EntityDescriptor testEntityDescriptor;
  private EntityDescriptor relatedDescriptor;
  private EntityDescriptor ownerDescriptor;

  @Before
  public void setUp() throws Exception {
    metaModel = new MetaModel();
    metaModel.addEntity(TestEntity.class);
    metaModel.addEntity(RelationOwner.class);
    metaModel.addEntity(Related.class);
    testEntityDescriptor = metaModel.getEntityDescriptor(TestEntity.class);
    ownerDescriptor = metaModel.getEntityDescriptor(RelationOwner.class);
    relatedDescriptor = metaModel.getEntityDescriptor(Related.class);
  }

  @Test
  public void testStore() throws Exception {
    DefaultEntityPersister persister = new DefaultEntityPersister();
    TestEntity testEntity = new TestEntity("Hallo welt");

    String fileName = testEntityDescriptor.getFileGenerator().getFileName(tempRepository.getRepository(), testEntityDescriptor, testEntity);
    Path folder = testEntityDescriptor.getFolderGenerator().getFolder(tempRepository.getRepository(), null, testEntity);

    Path target = folder.resolve(fileName);
    byte[] contents = persister.createFileContents(tempRepository.getRepository(), testEntityDescriptor, testEntity);
    Files.write(target, contents);

    assertTrue(target.toFile().exists());

    assertTrue(Files.readAllLines(target).stream().filter(line -> line.contains("Hallo welt")).findFirst().isPresent());

    Object load = persister.load(tempRepository.getRepository(), testEntityDescriptor, target);
    assertEquals(testEntity, load);
  }

  @Test
  public void testCanHandleFile() throws Exception {
    DefaultEntityPersister persister = new DefaultEntityPersister();
    TestEntity testEntity = new TestEntity("Hallo welt");
    byte[] contents = persister.createFileContents(tempRepository.getRepository(), testEntityDescriptor, testEntity);
    Path write = Files.write(tempRepository.getPath().resolve(TestEntity.class.getSimpleName()), contents);
    assertTrue(persister.canParse(write, testEntityDescriptor));
  }

  private static final Logger log = LoggerFactory.getLogger(DefaultEntityPersisterTest.class);

  @Test
  public void testPersistRelation() throws Exception {
    DefaultEntityPersister persister = new DefaultEntityPersister();
    persister.initialize(tempRepository.getRepository(), metaModel);
    RelationOwner owner = new RelationOwner("owner");
    Related related = new Related("related").setId("relatedId");
    Related child = new Related("child").setId("childId");

    owner.setChild(child);
    owner.getRelatedSet().add(related);
    owner.getRelatedList().add(related);
    owner.getRelatedList().add(related);

    byte[] fileContents = persister.createFileContents(tempRepository.getRepository(), ownerDescriptor, owner);
    String json = new String(fileContents);
    log.info(json);
    assertThat(json, Matchers.not(Matchers.containsString(Related.class.getName())));
    assertThat(json, Matchers.containsString("\"child\" : \"childId\""));
    assertThat(json, Matchers.containsString("\"relatedList\" : {\n" + "      \"java.util.ArrayList\" : [ \"relatedId\", \"relatedId\" ]"));
    assertThat(json, Matchers.containsString("\"relatedSet\" : {\n" + "      \"java.util.HashSet\" : [ \"relatedId\" ]"));
  }
}