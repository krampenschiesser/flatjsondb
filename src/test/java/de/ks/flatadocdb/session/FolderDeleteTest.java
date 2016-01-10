/**
 * Copyright [2016] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ks.flatadocdb.session;

import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.TempRepository;
import de.ks.flatadocdb.annotation.Child;
import de.ks.flatadocdb.annotation.Entity;
import de.ks.flatadocdb.defaults.SingleFolderGenerator;
import de.ks.flatadocdb.entity.NamedEntity;
import de.ks.flatadocdb.metamodel.MetaModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertFalse;

public class FolderDeleteTest {

  private MetaModel metamodel;
  private Repository repository;

  @Rule
  public TempRepository tempRepository = new TempRepository();

  @Before
  public void setUp() throws Exception {
    repository = tempRepository.getRepository();
    metamodel = tempRepository.getMetaModel();
    metamodel.addEntity(FolderOwner.class);
    metamodel.addEntity(Related.class);

  }

  @Test
  public void testDeleteFolderWithChild() throws Exception {
    FolderOwner owner = new FolderOwner("owner");
    Related child = new Related("child");
    owner.setRelated(child);

    Session session = new Session(metamodel, repository);
    session.persist(owner);
    session.prepare();
    session.commit();

    session = new Session(metamodel, repository);
    session.remove(owner);
    session.prepare();
    session.commit();

    Path repoPath = tempRepository.getPath();
    assertFalse("Empty folders should have been removed", repoPath.resolve("FolderOwner").toFile().exists());
  }

  @Entity(folderGenerator = SingleFolderGenerator.class)
  public static class FolderOwner extends NamedEntity {
    @Child
    protected Related related;

    public FolderOwner(String owner) {
      super(owner);
    }

    protected FolderOwner() {
      super(null);
    }

    public Related getRelated() {
      return related;
    }

    public void setRelated(Related related) {
      this.related = related;
    }
  }
}
