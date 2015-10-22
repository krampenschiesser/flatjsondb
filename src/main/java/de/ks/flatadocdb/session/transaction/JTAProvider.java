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

package de.ks.flatadocdb.session.transaction;

import javax.transaction.Transaction;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;

public interface JTAProvider {
  Transaction getCurrentTransaction();

  TransactionSynchronizationRegistry getRegistry();

  static Optional<JTAProvider> lookup() {
    ServiceLoader<JTAProvider> loader = ServiceLoader.load(JTAProvider.class);
    HashSet<JTAProvider> providers = new HashSet<>();
    loader.forEach(l -> providers.add(l));
    if (providers.size() > 1) {
      throw new IllegalStateException("Found multiple providers for " + JTAProvider.class.getName());
    } else if (providers.size() == 1) {
      return Optional.of(providers.iterator().next());
    } else {
      return Optional.empty();
    }
  }
}
