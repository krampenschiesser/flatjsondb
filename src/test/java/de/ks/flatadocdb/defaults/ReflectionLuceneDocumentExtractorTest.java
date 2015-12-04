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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ReflectionLuceneDocumentExtractorTest {
  private static final Logger log = LoggerFactory.getLogger(ReflectionLuceneDocumentExtractorTest.class);

  @Test
  public void testGetFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<ReflectionLuceneDocumentExtractor.DocField> fields = extractor.getFields(RelationOwner.class);
    assertEquals(fields.toString(), 5, fields.size());

    fields = extractor.getFields(TestEntity.class);
    assertEquals(fields.toString(), 6, fields.size());


    fields = extractor.getFields(ClassWithCollection.class);
    assertEquals(fields.toString(), 3, fields.size());
  }

  @Test
  public void testGetArrayFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<ReflectionLuceneDocumentExtractor.DocField> fields = extractor.getFields(ClassWithArray.class);
    assertEquals(fields.toString(), 1, fields.size());
  }

  @Test
  public void testGetCollectionFields() throws Exception {
    ReflectionLuceneDocumentExtractor extractor = new ReflectionLuceneDocumentExtractor();
    Set<ReflectionLuceneDocumentExtractor.DocField> fields = extractor.getFields(ClassWithCollection.class);
    assertEquals(fields.toString(), 3, fields.size());
  }

  @Test
  public void testDocFieldArray() throws Exception {
    ClassWithArray classWithArray = new ClassWithArray();
    classWithArray.strings = new String[]{"bla", "blubb"};

    ReflectionLuceneDocumentExtractor.DocField docField = new ReflectionLuceneDocumentExtractor().getFields(ClassWithArray.class).iterator().next();
    IndexableField indexableField = docField.apply(classWithArray);
    assertEquals("strings", indexableField.name());
    assertEquals("[bla, blubb]", indexableField.stringValue());
  }

  @Test
  public void testDocFieldCollection() throws Exception {
    ClassWithCollection classWithCollection = new ClassWithCollection();
    classWithCollection.booleans = Arrays.asList(true, false, true);
    classWithCollection.luceneField = Arrays.asList(StandardLuceneFields.ID);
    classWithCollection.objects = Arrays.asList(new Object(), new Object());
    classWithCollection.times = Arrays.asList(LocalDateTime.now(), LocalDateTime.now().minusDays(1));

    Set<ReflectionLuceneDocumentExtractor.DocField> fields = new ReflectionLuceneDocumentExtractor().getFields(ClassWithCollection.class);
    for (ReflectionLuceneDocumentExtractor.DocField field : fields) {
      IndexableField indexableField = field.apply(classWithCollection);
      if (field.getField().getName().equals("booleans")) {
        assertEquals("booleans", indexableField.name());
        assertEquals("true, false, true", indexableField.stringValue());
      } else if (field.getField().getName().equals("times")) {
        assertEquals("times", indexableField.name());
        assertEquals(classWithCollection.times.get(0).toString() + ", " + classWithCollection.times.get(1).toString(), indexableField.stringValue());
      } else if (field.getField().getName().equals("luceneField")) {
        assertEquals("luceneField", indexableField.name());
        assertEquals("ID", indexableField.stringValue());
      }
    }
  }

  @Test
  public void testCreateDocument() throws Exception {

    TestEntity testEntity = new TestEntity("huhu").setAttribute("bla");
    Document document = new ReflectionLuceneDocumentExtractor().createDocument(testEntity);
    assertThat(document.getFields(), Matchers.hasSize(6));
    assertEquals("null", document.getField("id").stringValue());
    assertEquals("huhu", document.getField("name").stringValue());
    assertEquals("bla", document.getField("attribute").stringValue());
  }

  @Test
  public void testDocFieldEntity() throws Exception {
    TestEntity testEntity = new TestEntity("huhu").setAttribute("bla");

    Set<ReflectionLuceneDocumentExtractor.DocField> fields = new ReflectionLuceneDocumentExtractor().getFields(TestEntity.class);
    fields.forEach(f -> log.info(f.apply(testEntity).toString()));
  }

  public static class ClassWithArray {
    String[] strings;
    Object[] objects;
  }

  public static class ClassWithCollection {
    List<Boolean> booleans;
    List<Object> objects;
    List<LocalDateTime> times;
    List<StandardLuceneFields> luceneField;
  }
}