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

package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.annotation.lifecycle.LifeCycle;
import de.ks.flatadocdb.annotation.lifecycle.PostLoad;
import de.ks.flatadocdb.annotation.lifecycle.PostRemove;
import de.ks.flatadocdb.annotation.lifecycle.PrePersist;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LifeCycleParserTest {
  @Test
  public void testParseLifecycleMethodsParent() throws Exception {
    Map<LifeCycle, Set<MethodHandle>> lifeCycleSetMap = new LifeCycleParser().parseMethods(LifeCycleEntityParent.class);
    assertEquals(2, lifeCycleSetMap.size());
  }

  @Test
  public void testParseLifecycleMethodsChild() throws Exception {
    Map<LifeCycle, Set<MethodHandle>> lifeCycleSetMap = new LifeCycleParser().parseMethods(LifeCycleEntityChild.class);
    assertEquals(3, lifeCycleSetMap.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadParsing() throws Exception {
    new LifeCycleParser().parseMethods(BadLifeCycleDeclaration.class);
  }

  public static class BadLifeCycleDeclaration {
    @PrePersist
    private void prePersist(String bla) {
      //
    }
  }

  public static class LifeCycleEntityChild extends LifeCycleEntityParent {
    @Override
    @PostLoad
    public void postLoad() {
      super.postLoad();
      String bla = "".toLowerCase(Locale.CANADA);
      bla.trim();
    }

    @PostRemove
    protected void postRemove() {
    }
  }

  public static class LifeCycleEntityParent {
    @PostLoad
    public void postLoad() {
    }

    @PrePersist
    private void prePersist() {
    }
  }

}