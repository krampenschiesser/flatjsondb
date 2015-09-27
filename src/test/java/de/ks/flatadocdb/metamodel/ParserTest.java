package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.annotation.Id;
import de.ks.flatadocdb.annotation.Property;
import de.ks.flatadocdb.annotation.Version;
import de.ks.flatadocdb.entity.BaseEntity;
import de.ks.flatadocdb.entity.NamedEntity;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.ifc.PropertyPersister;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

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

  @Test(expected = Parser.ParseException.class)
  public void testBadPersister() throws Exception {
    parser.parse(BadPersisterEntity.class);
  }

  @Test(expected = Parser.ParseException.class)
  public void testPrivatePersister() throws Exception {
    parser.parse(PrivatePersisterEntity.class);
  }

  @Test
  public void testCorrectEntity() throws Throwable {
    EntityDescriptor result = parser.parse(CorrectEntity.class);
    assertNotNull(result);
    assertEquals(CorrectEntity.class, result.entityClass);
    assertNotNull(result.naturalIdFieldAccess);
    assertNotNull(result.versionAccess);
    assertNotNull(result.idAccess);
    assertNotNull(result.persister);

    CorrectEntity entity = new CorrectEntity("test").setId(42).setVersion(3);

    assertEquals("test", result.naturalIdFieldAccess.invoke(entity));
    assertEquals(42, (long) result.idAccess.invoke(entity));
    assertEquals(3, (long) result.versionAccess.invoke(entity));
  }

  @Test
  public void testEntityWithPropertyPersisters() throws Exception {
    EntityDescriptor result = parser.parse(EntityWithPropertyPersisters.class);
    assertEquals(2, result.getPropertyPersisters().size());

    assertTrue(result.getPropertyPersister("bla").isPresent());
    assertTrue(result.getPropertyPersister("other").isPresent());
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

  @Entity(persister = BadPersister.class)
  static class BadPersisterEntity extends BaseEntity {

  }

  @Entity(persister = PrivatePersister.class)
  static class PrivatePersisterEntity extends BaseEntity {

  }

  static class BadPersister implements EntityPersister {
    public BadPersister(String bla) {

    }

    @Override
    public Object load(EntityDescriptor descriptor) {
      return null;
    }

    @Override
    public void save(EntityDescriptor descriptor, File path, Object object) {

    }
  }

  static class PrivatePersister implements EntityPersister {
    private PrivatePersister() {

    }

    @Override
    public Object load(EntityDescriptor descriptor) {
      return null;
    }

    @Override
    public void save(EntityDescriptor descriptor, File path, Object object) {

    }
  }

  @Entity
  static class EntityWithPropertyPersisters extends BaseEntity {
    @Property(TestStringPropertyPersister.class)
    protected String bla;

    @Property(TestStringPropertyPersister.class)
    protected String other;
  }

  static class TestStringPropertyPersister implements PropertyPersister<EntityWithPropertyPersisters, String> {
    @Override
    public String load(EntityWithPropertyPersisters entityWithPropertyPersisters) {
      return null;
    }

    @Override
    public void save(String s, EntityWithPropertyPersisters entityWithPropertyPersisters) {

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