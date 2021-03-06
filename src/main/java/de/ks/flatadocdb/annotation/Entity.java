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

package de.ks.flatadocdb.annotation;

import de.ks.flatadocdb.defaults.DefaultEntityPersister;
import de.ks.flatadocdb.defaults.DefaultFileGenerator;
import de.ks.flatadocdb.defaults.JoinedRootFolderGenerator;
import de.ks.flatadocdb.defaults.ReflectionLuceneDocumentExtractor;
import de.ks.flatadocdb.ifc.EntityPersister;
import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.ifc.FolderGenerator;
import de.ks.flatadocdb.ifc.LuceneDocumentExtractor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Main annotation to mark a class as entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
  /**
   * Used to load/save the entity
   * @return
   */
  Class<? extends EntityPersister> persister() default DefaultEntityPersister.class;

  /**
   * Used to determine and generate the target folder of the entity
   * @return
   */
  Class<? extends FolderGenerator> folderGenerator() default JoinedRootFolderGenerator.class;

  /**
   * Used to determine and generate the target file of the entity
   * @return
   */
  Class<? extends FileGenerator> fileGenerator() default DefaultFileGenerator.class;

  /**
   * Used to extract the indexable fields for lucene from the entity
   * @return
   */
  Class<? extends LuceneDocumentExtractor> luceneDocExtractor() default ReflectionLuceneDocumentExtractor.class;

}
