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
import de.ks.flatadocdb.metamodel.MetaModel;
import de.ks.flatadocdb.session.transaction.local.TransactionProvider;
import de.ks.flatadocdb.session.transaction.local.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

//import de.ks.flatadocdb.session.transaction.local.LocalJTAProvider;

public class SessionFactory implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(SessionFactory.class);
  private final Map<String, Repository> repositoryByName = new HashMap<>();
  private final MetaModel metaModel = new MetaModel();
  private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),//
    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("SessionFactoryPooled-%d").build());

  public SessionFactory(Repository repository, String entityPackage) {
    this(Collections.singleton(repository), Collections.singleton(entityPackage));
  }

  public SessionFactory(Collection<Repository> repositories, Collection<String> packages) {
    checkSize(repositories, 1);
    checkSize(packages, 1);

    repositories.forEach(this::addRepository);
    metaModel.scanClassPath(packages);
  }

  private void checkSize(Collection<?> collection, int expectedSize) {
    if (collection.size() != expectedSize) {
      throw new IllegalArgumentException("Expected a collection with " + expectedSize + " elements, but got " + collection.size());
    }
  }


  public void addRepository(Repository repository) {
    Objects.requireNonNull(repository, "Repository is required");
    log.info("Added repository {}", repository.getPath());
    repositoryByName.put(repository.getName(), repository);
    repository.initialize();
  }

  public Repository getRepository(String name) {
    return repositoryByName.get(name);
  }

  public Session openSession(Repository repository) {
    Objects.requireNonNull(repository, "Repository is required");
    if (!repositoryByName.containsKey(repository.getName())) {
      addRepository(repository);
    }
    return new Session(metaModel, repository);
  }

  public Session openSession() {
    if (repositoryByName.size() == 1) {
      return openSession(repositoryByName.values().iterator().next());
    } else {
      throw new IllegalStateException("Requested to open a session without a specified repository.");
    }
  }

  public MetaModel getMetaModel() {
    return metaModel;
  }

  public void transactedSession(Repository repository, Consumer<Session> sessionConsumer) {
    Transactional.withNewTransaction(() -> {
      Session session = openSession(repository);
      TransactionProvider.instance.registerResource(session);
      sessionConsumer.accept(session);
    });
  }

  public <T> T transactedSessionRead(Repository repository, Function<Session, T> sessionFunction) {
    return Transactional.withNewTransaction(() -> {
      Session session = openSession(repository);
      TransactionProvider.instance.registerResource(session);
      return sessionFunction.apply(session);
    });
  }

  @Override
  public void close() {
    repositoryByName.values().forEach(Repository::close);
  }
}
