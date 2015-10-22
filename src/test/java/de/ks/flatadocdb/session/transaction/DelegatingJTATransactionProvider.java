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

public class DelegatingJTATransactionProvider implements JTAProvider {
//  public static final AtomicReference<JTAProvider> provider = new AtomicReference<>(new LocalJTAProvider());
//
//  public static void setProvider(JTAProvider other) {
//    provider.set(other);
//  }
//
//  public static JTAProvider getProvider() {
//    return provider.get();
//  }

  @Override
  public Transaction getCurrentTransaction() {
//    return provider.get() == null ? null : provider.get().getCurrentTransaction();
    return null;
  }

  @Override
  public TransactionSynchronizationRegistry getRegistry() {
//    return provider.get() == null ? null : provider.get().getRegistry();
    return null;
  }
}
