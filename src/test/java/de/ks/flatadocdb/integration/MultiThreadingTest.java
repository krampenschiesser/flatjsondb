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
package de.ks.flatadocdb.integration;

import com.google.common.base.StandardSystemProperty;
import de.ks.flatadocdb.Repository;
import de.ks.flatadocdb.exception.StaleObjectStateException;
import de.ks.flatadocdb.metamodel.TestEntity;
import de.ks.flatadocdb.session.SessionFactory;
import de.ks.flatadocdb.util.DeleteDir;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiThreadingTest {
  private static final Logger log = LoggerFactory.getLogger(MultiThreadingTest.class);
  private SessionFactory sessionFactory;
  private Repository repository;

  @Before
  public void setUp() throws Exception {
    Path repoPath = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "tempRepo");
    new DeleteDir(repoPath).delete();
    Files.createDirectories(repoPath);

    repository = new Repository(repoPath);
    sessionFactory = new SessionFactory(repository, TestEntity.class.getPackage().getName());
  }

  @After
  public void tearDown() throws Exception {
    sessionFactory.close();
  }

  @Ignore
  @Test
  public void testMassiveMultiThreading() throws Exception {
    int threads = Runtime.getRuntime().availableProcessors();
//    int threads = 1;
    int maxItems = 10000;
    int iterations = maxItems * 3;
    int batchsize = 10;

    testMultithreaded(threads, maxItems, iterations, batchsize);
  }

  @Test
  public void testReducedMultithreading() throws Exception {
    int maxItems = 500;
    int threads = Runtime.getRuntime().availableProcessors();
    testMultithreaded(threads, maxItems, maxItems / threads + 50, 10);
  }

  protected void testMultithreaded(int threads, int maxItems, int iterations, int batchsize) throws Exception {
    ExecutorService service = Executors.newFixedThreadPool(threads);
    List<TestEntity> items = IntStream.range(0, maxItems).mapToObj(i -> new TestEntity("entity" + i)).collect(Collectors.toList());
    ConcurrentLinkedQueue<TestEntity> insertionQueue = new ConcurrentLinkedQueue<>(items);
    ConcurrentLinkedQueue<String> workQueue = new ConcurrentLinkedQueue<>();

    CountDownLatch start = new CountDownLatch(threads);
    ArrayList<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      final int thread = i;
      Runnable runner = () -> {
        waitForBarrier(start);
        for (int j = 0; j < iterations; j++) {
          final String suffix = "T" + thread + "" + j;
          try {
            sessionFactory.transactedSession(session -> {
              TestEntity itemToInsert = insertionQueue.poll();
              if (itemToInsert != null) {
                session.persist(itemToInsert);
              } else {
                ArrayList<String> ids = new ArrayList<>(session.getRepository().getIndex().getAllIds());
                Collections.shuffle(ids);
                for (int k = 0; k < batchsize; k++) {
                  workQueue.add(ids.get(k));
                  String id = workQueue.poll();
                  if (id != null) {
                    TestEntity testEntity = session.findById(TestEntity.class, id);
                    testEntity.setAttribute(testEntity.getName() + suffix);
                  }
                }
              }
            });
          } catch (StaleObjectStateException e) {
            log.info("Got statel object state exception", e);
            //ok
          }
        }

      };
      futures.add(service.submit(runner));
    }

    ArrayList<Throwable> exceptions = new ArrayList<>();

    while (!futures.isEmpty()) {
      for (Iterator<Future<?>> iterator = futures.iterator(); iterator.hasNext(); ) {
        Future<?> next = iterator.next();
        try {
          next.get(1, TimeUnit.SECONDS);
          iterator.remove();
        } catch (TimeoutException e) {
          //ok next one
        }
      }
    }
  }

  protected void waitForBarrier(CountDownLatch latch) {
    try {
      latch.countDown();
      latch.await();
    } catch (Exception e) {
      //urks
    }
  }
}
