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

package de.ks.flatadocdb.metamodel.relation;

import de.ks.flatadocdb.session.Related;
import de.ks.flatadocdb.session.RelationOwner;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class RelationParserTest {

  @Test
  public void testParseToOneRelations() throws Exception {
    Set<ToOneRelation> relations = new RelationParser().parseToOneRelations(Related.class);
    assertEquals(1, relations.size());
    ToOneRelation relation = relations.iterator().next();
    assertEquals(RelationOwner.class, relation.getRelationType());
    assertNotNull(relation.getRelationField());
    assertNotNull(relation.setterHandle);
    assertNotNull(relation.getterHandle);
    assertTrue(relation.isLazy());
  }

  @Test
  public void testParseToManyRelations() throws Exception {
    Set<ToManyRelation> relations = new RelationParser().parseToManyRelations(RelationOwner.class);
    assertEquals(2, relations.size());
    ToManyRelation relation = relations.stream().filter(r -> r.getRelationField().getName().equals("relatedSet")).findFirst().get();

    assertEquals(Related.class, relation.getRelationType());
    assertNotNull(relation.getRelationField());
    assertEquals(Set.class, relation.getCollectionType());
    assertNotNull(relation.setterHandle);
    assertNotNull(relation.getterHandle);
    assertFalse(relation.isLazy());
  }

  @Test
  public void testParseToOneChildRelations() throws Exception {
    Set<ToOneChildRelation> relations = new RelationParser().parseToOneChildRelations(RelationOwner.class);
    assertEquals(1, relations.size());
    ToOneChildRelation relation = relations.iterator().next();
    assertEquals(Related.class, relation.getRelationType());
    assertNotNull(relation.getRelationField());
    assertNotNull(relation.setterHandle);
    assertNotNull(relation.getterHandle);
    assertNotNull(relation.getFileGenerator());
    assertNotNull(relation.getFolderGenerator());
    assertTrue(relation.isLazy());
  }

  @Test
  public void testParseToManyChildRelations() throws Exception {
    Set<ToManyChildRelation> relations = new RelationParser().parseToManyChildRelations(RelationOwner.class);
    assertEquals(1, relations.size());
    ToManyChildRelation relation = relations.iterator().next();

    assertEquals(Related.class, relation.getRelationType());
    assertNotNull(relation.getRelationField());
    assertEquals(List.class, relation.getCollectionType());
    assertNotNull(relation.setterHandle);
    assertNotNull(relation.getterHandle);
    assertNotNull(relation.getFileGenerator());
    assertNotNull(relation.getFolderGenerator());
    assertTrue(relation.isLazy());
  }
}
