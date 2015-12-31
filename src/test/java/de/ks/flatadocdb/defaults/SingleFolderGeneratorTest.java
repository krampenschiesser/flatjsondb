package de.ks.flatadocdb.defaults;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.metamodel.TestEntity;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class SingleFolderGeneratorTest {
  @Rule
  public final TempRepository tempRepository = new TempRepository();

  @Test
  public void testDefaultFolderGeneration() throws Exception {
    tempRepository.getMetaModel().addEntity(TestEntity.class);
    Repository repo = tempRepository.getRepository();
    JoinedRootFolderGenerator defaultFolderGenerator = new SingleFolderGenerator();
    Path path = defaultFolderGenerator.getFolder(repo, null, new TestEntity("test"));
    assertEquals(tempRepository.getPath().resolve(TestEntity.class.getSimpleName()).resolve("test"), path);
    assertTrue(path.toFile().exists());
  }

}