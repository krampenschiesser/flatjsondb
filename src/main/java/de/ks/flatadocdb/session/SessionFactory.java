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
package de.ks.flatadocdb.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import de.ks.flatadocdb.session.transaction.local.LocalJTAProvider;

public class SessionFactory {
  private static final Logger log = LoggerFactory.getLogger(SessionFactory.class);
//
//  private final MetaModel metaModel = new MetaModel();
//  private final JTAProvider jtaProvider;
//
//  public SessionFactory() {
//    Optional<JTAProvider> lookup = JTAProvider.lookup();
//    if (lookup.isPresent()) {
//      log.info("Found custom implementation of JTAProvider {}", lookup.get());
//      jtaProvider = lookup.get();
//    } else {
//      log.info("Found no custom implementation of JTAProvider. Will fallback to use local transaction management.");
//      jtaProvider = new LocalJTAProvider();
//    }
//  }
//
//  public Session openSession() {
//    return new Session();
//  }
//
//  public MetaModel getMetaModel() {
//    return metaModel;
//  }
//
//  public boolean isLocallyManaged() {
//    return jtaProvider instanceof LocalJTAProvider;
//  }
}
