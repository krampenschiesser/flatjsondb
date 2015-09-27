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
package de.ks.flatfiledb.session;

import de.ks.flatfiledb.metamodel.MetaModel;
import de.ks.flatfiledb.session.transaction.JTATransactionProvider;
import de.ks.flatfiledb.session.transaction.JTATransactionSynchronizationRegistryProvider;
import de.ks.flatfiledb.session.transaction.local.LocalTransactionProvider;
import de.ks.flatfiledb.session.transaction.local.LocalTransactionSynchronizationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.TransactionSynchronizationRegistry;
import java.util.Optional;

public class SessionFactory {
  private static final Logger log = LoggerFactory.getLogger(SessionFactory.class);

  private final MetaModel metaModel = new MetaModel();
  private final JTATransactionSynchronizationRegistryProvider registryProvider = new JTATransactionSynchronizationRegistryProvider();
  private final JTATransactionProvider transactionProvider;

  public SessionFactory() {
    TransactionSynchronizationRegistry registry = registryProvider.get();
    if (registry == null) {
      log.info("Found no TransactionSynchronizationRegistry via jndi lookup. Will use custom implementation.");
      registryProvider.set(new LocalTransactionSynchronizationRegistry());
      transactionProvider = new LocalTransactionProvider(registryProvider.get());
    } else {
      Optional<JTATransactionProvider> txProvider = JTATransactionProvider.lookup();
      if (txProvider.isPresent()) {
        this.transactionProvider = txProvider.get();
      } else {
        throw new IllegalStateException("We run in a JTA environment but do not have any JTATransactionProvider. Please provide an implementation.");
      }
    }

  }

  public MetaModel getMetaModel() {
    return metaModel;
  }

}
