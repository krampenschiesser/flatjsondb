package de.ks.flatfiledb.metamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetamodelTest {
  @Test
  public void testAddEntity() throws Exception {
    MetaModel metamodel = new MetaModel();
    metamodel.addEntity(TestEntity.class);
    assertEquals(1, metamodel.getEntities().size());
  }
}