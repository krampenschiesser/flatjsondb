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

import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

public class LocalTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry {
  @Override
  public Object getTransactionKey() {
    return null;
  }

  @Override
  public void putResource(Object key, Object value) {

  }

  @Override
  public Object getResource(Object key) {
    return null;
  }

  @Override
  public void registerInterposedSynchronization(Synchronization sync) {

  }

  @Override
  public int getTransactionStatus() {
    return 0;
  }

  @Override
  public void setRollbackOnly() {

  }

  @Override
  public boolean getRollbackOnly() {
    return false;
  }
}
