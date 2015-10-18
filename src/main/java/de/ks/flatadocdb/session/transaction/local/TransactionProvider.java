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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

public class TransactionProvider {
  public static final TransactionProvider instance = new TransactionProvider();

  protected final ThreadLocal<Deque<BaseTransaction>> transactions = ThreadLocal.withInitial(LinkedList::new);

  protected TransactionProvider() {
    //
  }

  public SimpleTransaction beginTransaction(String name) {
    BaseTransaction baseTransaction = new BaseTransaction(name);
    transactions.get().add(baseTransaction);
    return baseTransaction;
  }

  public void removeTransaction(String txName) {
    BaseTransaction tx = transactions.get().stream().filter(t -> t.getName().equals(txName)).findFirst().get();
    tx.getResources().forEach(TransactionResource::close);
    transactions.get().remove(tx);
  }

  public Optional<SimpleTransaction> getCurrentTransaction() {
    return Optional.ofNullable(transactions.get().peekLast());
  }

  public void registerResource(TransactionResource resource) {
    BaseTransaction tx = transactions.get().peekLast();
    if (tx != null) {
      tx.registerResource(resource);
    }
  }
}
