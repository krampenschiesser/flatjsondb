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

package de.ks.flatadocdb.defaults;

import de.ks.flatadocdb.metamodel.EntityDescriptor;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultFileGeneratorTest {
  private static final Logger log = LoggerFactory.getLogger(DefaultFileGeneratorTest.class);

  @Test
  public void testHashCode() throws Exception {
    Object o = new Object();
    DefaultFileGenerator generator = new DefaultFileGenerator();
    String s = generator.parseHashCode(o);
    assertNotNull(s);
    log.info(s);

    EntityDescriptor desc = Mockito.mock(EntityDescriptor.class);
    Mockito.when(desc.getNaturalId(o)).thenReturn(null);
    String fileName = generator.getFileName(null, desc, o);
    assertEquals(s + "." + DefaultFileGenerator.EXTENSION, fileName);
  }

  @Test
  public void testNaturalId() throws Exception {
    DefaultFileGenerator generator = new DefaultFileGenerator();
    String s = generator.parseNaturalId("#: öäana89?!!``  l");
    assertNotNull(s);
    assertEquals("_öäana89__l", s);
  }

}