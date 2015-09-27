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

package de.ks.log;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.File;
import java.util.Optional;

public class LogbackConfigurator extends ContextAwareBase implements Configurator {
  @Override
  public void configure(LoggerContext loggerContext) {
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);

    Optional<File> cfgFile = getCfgFile();
    if (cfgFile.isPresent()) {
      System.out.println("Found cfg file " + cfgFile.get());

      try {
        configurator.doConfigure(cfgFile.get());
      } catch (JoranException je) {
        // StatusPrinter will handle this
      }
      StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    } else {
      System.err.println("Could not find any cfg/logback.xml");
      BasicConfigurator.configure((LoggerContext) context);
    }
  }

  protected static Optional<File> getCfgFile() {
    String classFilePath = LogbackConfigurator.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    File file = new File(classFilePath);
    return discoverLogbackCfg(file);
  }

  private static Optional<File> discoverLogbackCfg(File file) {
    for (File parent = file; parent != null; parent = parent.getParentFile()) {
      File cfgDir = new File(parent, "cfg");
      File cfgFile = new File(cfgDir, "logback.xml");
      if (cfgFile.exists()) {
        return Optional.of(cfgFile);
      }
    }
    return Optional.empty();
  }
}
