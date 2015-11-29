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

package de.ks.flatadocdb.session;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.index.GlobalIndex;
import de.ks.flatadocdb.metamodel.MetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//import de.ks.flatadocdb.session.transaction.local.LocalJTAProvider;

public class SessionFactory {
  private static final Logger log = LoggerFactory.getLogger(SessionFactory.class);
  private final LinkedHashMap<Repository, GlobalIndex> repositories = new LinkedHashMap<>();
  private final MetaModel metaModel = new MetaModel();
  private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),//
    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("SessionFactoryPooled-%d").build());

  public List<Repository> getRepositories() {
    return repositories.keySet().stream().collect(Collectors.toList());
  }

  public void addRepository(Repository repository) {
    GlobalIndex index = new GlobalIndex(repository, metaModel, executorService);
    repositories.put(repository, index);
    index.recreate();
  }

  public Session openSession(Repository repository) {
    if (!repositories.containsKey(repository)) {
      addRepository(repository);
    }
    return new Session(metaModel, repository, repositories.get(repository));
  }

  public Session openSession() {
    if (repositories.size() == 1) {
      return openSession(repositories.keySet().iterator().next());
    } else {
      throw new IllegalStateException("Requested to open a session without a specified repository.");
    }
  }

  public MetaModel getMetaModel() {
    return metaModel;
  }
}
