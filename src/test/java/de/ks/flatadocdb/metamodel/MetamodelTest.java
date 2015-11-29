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

package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.session.Related;
import de.ks.flatadocdb.session.RelationOwner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetamodelTest {
  @Test
  public void testAddEntity() throws Exception {
    MetaModel metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);
    assertEquals(1, metamodel.getEntities().size());
  }

  @Test
  public void testScanClassPathForPackages() throws Exception {
    MetaModel metaModel = new MetaModel();
    int found = metaModel.scanClassPath(RelationOwner.class.getPackage().getName()).size();
    assertEquals(2, found);
    assertNotNull(metaModel.getEntityDescriptor(RelationOwner.class));
    assertNotNull(metaModel.getEntityDescriptor(Related.class));

    found = metaModel.scanClassPath(TestEntity.class.getPackage().getName()).size();
    assertEquals(1, found);
    assertNotNull(metaModel.getEntityDescriptor(TestEntity.class));
  }

  @Test
  public void testScanClassPath() throws Exception {
    MetaModel metaModel = new MetaModel();
    int found = metaModel.scanClassPath(RelationOwner.class.getPackage().getName(), TestEntity.class.getPackage().getName()).size();
    assertEquals(3, found);
    assertNotNull(metaModel.getEntityDescriptor(RelationOwner.class));
    assertNotNull(metaModel.getEntityDescriptor(Related.class));
    assertNotNull(metaModel.getEntityDescriptor(TestEntity.class));
  }
}