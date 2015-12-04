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

import de.ks.flatadocdb.defaults.DefaultFileGenerator;
import de.ks.flatadocdb.defaults.JoinedSubFolderGenerator;
import de.ks.flatadocdb.ifc.FileGenerator;
import de.ks.flatadocdb.ifc.FolderGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a child of a given entity.
 * This will cause the annotated field to be persisted in the same or a subfolder of the parent entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Child {
  boolean lazy() default true;

  Class<? extends FolderGenerator> folderGenerator() default JoinedSubFolderGenerator.class;

  Class<? extends FileGenerator> fileGenerator() default DefaultFileGenerator.class;
}
