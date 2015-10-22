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
package de.ks.flatadocdb;

import com.google.common.base.StandardSystemProperty;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TempRepository extends ExternalResource {
  private Path path;
  private Repository repository;

  @Override
  protected void before() throws Throwable {
    path = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "testRepository");
    new DeleteDir(path).delete();
    repository = new Repository(path);
  }

  public Path getPath() {
    return path;
  }

  public Repository getRepository() {
    return repository;
  }
}
