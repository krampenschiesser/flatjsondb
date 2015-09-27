package de.ks.flatfiledb.metamodel;

import de.ks.flatfiledb.annotation.Entity;
import de.ks.flatfiledb.annotation.Id;
import de.ks.flatfiledb.annotation.Version;
import de.ks.flatfiledb.entity.NamedEntity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParserTest {
  private Parser parser;

  @Before
  public void setUp() throws Exception {
    parser = new Parser();
  }

  @Test(expected = Parser.ParseException.class)
  public void testNoEntity() throws Exception {
    parser.parse(NoEntity.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testNoId() throws Exception {
    parser.parse(NoId.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testMultipleId() throws Exception {
    parser.parse(MultipleId.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testWrongIdType() throws Exception {
    parser.parse(WrongIdType.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testNoVersion() throws Exception {
    parser.parse(NoVersion.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testMultipleVersion() throws Exception {
    parser.parse(MultipleVersion.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testWrongVersionType() throws Exception {
    parser.parse(WrongVersionType.class);
  }

  @Test
  public void testCorrectEntity() throws Throwable {
    EntityDescriptor result = parser.parse(CorrectEntity.class);
    assertNotNull(result);
    assertEquals(CorrectEntity.class, result.entityClass);
    assertNotNull(result.naturalIdFieldAccess);
    assertNotNull(result.versionAccess);
    assertNotNull(result.idAccess);

    CorrectEntity entity = new CorrectEntity("test").setId(42).setVersion(3);

    assertEquals("test", result.naturalIdFieldAccess.invoke(entity));
    assertEquals(42, (long) result.idAccess.invoke(entity));
    assertEquals(3, (long) result.versionAccess.invoke(entity));
  }

  @Entity
  static class CorrectEntity extends NamedEntity {
    public CorrectEntity(String name) {
      super(name);
    }

    public CorrectEntity setVersion(long version) {
      this.version = version;
      return this;
    }

    public CorrectEntity setId(long id) {
      this.id = id;
      return this;
    }

  }

  static class NoEntity {
    protected String name;
  }

  @Entity
  static class NoId {
    protected String name;
  }

  @Entity
  static class MultipleId {
    @Id
    private long id1;
    @Id
    private long id2;
    protected String name;
  }

  @Entity
  static class WrongIdType {
    @Id
    private short id1;
    protected String name;
  }

  @Entity
  static class NoVersion {
    @Id
    private long id;
  }

  @Entity
  static class MultipleVersion {
    @Id
    private long id;
    @Version
    private long version1;
    @Version
    private long version2;
  }

  @Entity
  static class WrongVersionType {
    @Id
    private long id;
    @Version
    private short version;
  }
}