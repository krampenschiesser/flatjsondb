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
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Main class to use for using transactions.
 * It also sets a MDC for the underlying logging framework displaying the transaction name.
 */
public class Transactional {
  private static final Logger log = LoggerFactory.getLogger(Transactional.class);
  private static final TransactionProvider provider = TransactionProvider.instance;
  private static final AtomicLong counter = new AtomicLong(0L);
  public static final String TRANSACTION_NAME_PATTERN = "tx-%06d";
  public static final String TRANSACTION_MDC_KEY = "TX_MDC";//used for mdc logging

  /**
   * Read in a transactional way
   * Automatic(incremented) transaction id.
   *
   * @param supplier the read function
   * @param <T>      return value type
   * @return anything
   */
  public static <T> T withNewTransaction(Supplier<T> supplier) {
    return withNewTransaction(String.format(TRANSACTION_NAME_PATTERN, counter.incrementAndGet()), supplier);
  }

  /**
   * Run in a transactionaly way.
   * Automatic(incremented) transaction id.
   *
   * @param runnable code to be executed
   */
  public static void withNewTransaction(Runnable runnable) {
    Supplier<Object> supplier = () -> {
      runnable.run();
      return null;
    };
    withNewTransaction(String.format(TRANSACTION_NAME_PATTERN, counter.incrementAndGet()), supplier);
  }

  /**
   * Read in a transactional way
   * Automatic(incremented) transaction id.
   *
   * @param txName   give a specific name for the transaction
   * @param supplier the read function
   * @param <T>      return value type
   * @return anything
   */
  public static <T> T withNewTransaction(String txName, Supplier<T> supplier) {
    SimpleTransaction tx = provider.beginTransaction(txName);
    MDC.put(TRANSACTION_MDC_KEY, txName.substring(3));
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
      MDC.remove(TRANSACTION_MDC_KEY);
      provider.removeTransaction(txName);
    }
  }
}
