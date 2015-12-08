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

import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.ifc.FolderGenerator;

import java.lang.reflect.Field;

public class ToOneChildRelation extends ToOneRelation implements ChildRelation {
  protected final FolderGenerator folderGenerator;
  protected final FileGenerator fileGenerator;

  public ToOneChildRelation(Class<?> relationType, Field relationField, boolean lazy, FolderGenerator folderGenerator, FileGenerator fileGenerator) {
    super(relationType, relationField, lazy);
    this.folderGenerator = folderGenerator;
    this.fileGenerator = fileGenerator;
  }

  @Override
  public FolderGenerator getFolderGenerator() {
    return folderGenerator;
  }

  @Override
  public FileGenerator getFileGenerator() {
    return fileGenerator;
  }
}
