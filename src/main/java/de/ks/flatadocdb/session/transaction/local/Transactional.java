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
package de.ks.flatadocdb.session.transaction.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class Transactional {
  private static final Logger log = LoggerFactory.getLogger(Transactional.class);
  private static final TransactionProvider provider = TransactionProvider.instance;
  private static final AtomicLong counter = new AtomicLong(0L);

  public static <T> T withNewTransaction(Supplier<T> supplier) {
    return withNewTransaction(String.format("tx%04d", counter.incrementAndGet()), supplier);
  }

  public static <T> T withNewTransaction(String txName, Supplier<T> supplier) {
    SimpleTransaction tx = provider.beginTransaction(txName);
    try {
      T retval = supplier.get();
      tx.prepare();
      tx.commit();
      log.trace("Successfully committed tx {}", txName);
      return retval;
    } catch (Throwable t) {
      log.error("Could not commit tx {}", txName, t);
      tx.rollback();
      throw t;
    } finally {
      provider.removeTransaction(txName);
    }
  }
}
