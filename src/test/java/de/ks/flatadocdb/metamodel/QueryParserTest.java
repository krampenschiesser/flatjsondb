/**
 * Copyright [2015] [Christian Loehnert]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ks.flatadocdb.metamodel;

import de.ks.flatadocdb.annotation.QueryProvider;
import de.ks.flatadocdb.query.Query;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {
  @Test
  public void testParseQueries() throws Exception {
    Set<Query<?, ?>> queries = new QueryParser().getQueries(TestEntity.class);
    assertEquals(3, queries.size());
  }

  @Test(expected = ParseException.class)
  public void testObjectQuery() throws Exception {
    new QueryParser().getQueries(ObjectQueryDeclaration.class);
  }

  @Test(expected = ParseException.class)
  public void testNonStaticQuery() throws Exception {
    new QueryParser().getQueries(NonstaticDeclaration.class);
  }

  @Test(expected = ParseException.class)
  public void testNonPublicQuery() throws Exception {
    new QueryParser().getQueries(NonPublicDeclaration.class);
  }

  @Test(expected = ParseException.class)
  public void testNonAnnotatedDeclaration() throws Exception {
    new QueryParser().getQueries(NonAnnotatedDeclaration.class);
  }

  static class ObjectQueryDeclaration {
    @QueryProvider
    public static Object getQuery() {
      return null;
    }
  }

  static class NonstaticDeclaration {
    @QueryProvider
    public Query<?, ?> getQuery() {
      return null;
    }
  }

  static class NonPublicDeclaration {
    @QueryProvider
    protected static Query<?, ?> getQuery() {
      return null;
    }
  }

  static class NonAnnotatedDeclaration {
    public static Query<?, ?> getQuery() {
      return null;
    }
  }
}
