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

import de.ks.flatadocdb.index.StandardLuceneFields;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.RelationOwner;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ReflectionLuceneDocumentExtractorTest {
  @Test
  public void testGetFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<Field> fields = extractor.getFields(RelationOwner.class);
    assertEquals(fields.toString(), 5, fields.size());

    fields = extractor.getFields(TestEntity.class);
    assertEquals(fields.toString(), 6, fields.size());


    fields = extractor.getFields(ClassWithCollection.class);
    assertEquals(fields.toString(), 3, fields.size());
  }

  @Test
  public void testGetArrayFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<Field> fields = extractor.getFields(ClassWithArray.class);
    assertEquals(fields.toString(), 1, fields.size());
  }

  @Test
  public void testGetCollectionFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<Field> fields = extractor.getFields(ClassWithCollection.class);
    assertEquals(fields.toString(), 3, fields.size());
  }

  public static class ClassWithArray {
    private String[] booleans = new String[0];
    private Object[] objects = new Object[0];
  }

  public static class ClassWithCollection {
    private List<Boolean> booleans;
    private List<Object> objects;
    private List<LocalDateTime> times;
    private List<StandardLuceneFields> luceneField;
  }
}