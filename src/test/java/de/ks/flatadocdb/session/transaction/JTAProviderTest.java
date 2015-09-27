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

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.util.Optional;

import static org.junit.Assert.*;

public class JTAProviderTest {
  @Before
  public void setUp() throws Exception {
    DelegatingJTATransactionProvider.setProvider(new TestProvider());
  }

  @After
  public void tearDown() throws Exception {
    DelegatingJTATransactionProvider.setProvider(null);
  }

  @Test
  public void testGetTXByLoader() throws Exception {
    Optional<JTAProvider> lookup = JTAProvider.lookup();
    assertTrue(lookup.isPresent());
    JTAProvider provider = lookup.get();
    assertNotNull(provider.getCurrentTransaction());
    assertThat(provider.getCurrentTransaction(), Matchers.instanceOf(TestTransaction.class));
    assertThat(provider, Matchers.instanceOf(DelegatingJTATransactionProvider.class));
  }

  @Test
  public void testNone() throws Exception {
    DelegatingJTATransactionProvider.setProvider(null);

    Optional<JTAProvider> lookup = JTAProvider.lookup();
    assertNull(lookup.get().getCurrentTransaction());
  }

  static class TestTransaction implements Transaction {
    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
      return false;
    }

    @Override
    public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
      return false;
    }

    @Override
    public int getStatus() throws SystemException {
      return 42;
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {

    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {

    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }
  }

  static class TestProvider implements JTAProvider {
    @Override
    public Transaction getCurrentTransaction() {
      return new TestTransaction();
    }

    @Override
    public TransactionSynchronizationRegistry getRegistry() {
      return null;
    }
  }
}