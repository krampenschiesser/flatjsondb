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

import de.ks.flatadocdb.exception.AggregateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class BaseTransaction implements SimpleTransaction {
  private static final Logger log = LoggerFactory.getLogger(BaseTransaction.class);
  private final String name;
  private final List<TransactionResource> resources = new ArrayList<>();

  public BaseTransaction(String name) {
    this.name = name;
  }

  @Override
  public void prepare() {
    resources.forEach(TransactionResource::prepare);
  }

  @Override
  public void commit() {
    resources.forEach(TransactionResource::commit);
  }

  @Override
  public void rollback() {
    ArrayList<Throwable> exceptions = new ArrayList<>();
    resources.forEach((transactionResource) -> {
      try {
        transactionResource.rollback();

      } catch (Throwable t) {
        exceptions.add(t);
        log.error("Exception occured during rollback", t);
      }
    });
    if (!exceptions.isEmpty()) {
      throw new AggregateException(exceptions);
    }
  }

  public void registerResource(TransactionResource resource) {
    this.resources.add(resource);
  }

  public String getName() {
    return name;
  }

  public List<TransactionResource> getResources() {
    return resources;
  }

  @Override
  public String toString() {
    return "BaseTransaction{" +
      "name='" + name + '\'' +
      '}';
  }
}
