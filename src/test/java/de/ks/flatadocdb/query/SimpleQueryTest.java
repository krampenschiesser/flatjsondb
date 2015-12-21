package de.ks.flatadocdb.query;

import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleQueryTest {
  @Test
  public void testSimpleEquals() throws Exception {
    Query<TestEntity, String> first = Query.of(TestEntity.class, TestEntity::getAttribute);
    Query<TestEntity, String> second = Query.of(TestEntity.class, TestEntity::getAttribute);

    assertEquals(first, second);
  }

  @Test
  public void testValueExtraction() throws Exception {
    Query<TestEntity, String> query = Query.of(TestEntity.class, TestEntity::getAttribute);

    TestEntity testEntity = new TestEntity("bla").setAttribute("blubb");
    String value = query.getValue(testEntity);
    assertEquals("blubb", value);
  }

  @Test
  public void testNullPath() throws Exception {
    Query<WithEmbedded, String> query = Query.of(WithEmbedded.class, e -> e.getEmbedded().getValue());

    WithEmbedded withEmbedded = new WithEmbedded();
    String value = query.getValue(withEmbedded);
    assertNull(value);
  }

  static class WithEmbedded {
    Embedded embedded;

    public Embedded getEmbedded() {
      return embedded;
    }

    public void setEmbedded(Embedded embedded) {
      this.embedded = embedded;
    }
  }

  static class Embedded {
    protected String value;

    public String getValue() {
      return value;
    }

    public Embedded setValue(String value) {
      this.value = value;
      return this;
    }
  }
}