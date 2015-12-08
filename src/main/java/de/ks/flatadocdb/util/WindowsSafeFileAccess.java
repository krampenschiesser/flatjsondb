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
package de.ks.flatadocdb.util;

import com.google.common.base.StandardSystemProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Locale;

/**
 * windows needs  special handling(of course) because stuff like on access scanners
 * can open one of the two files and parse them -> no write access allowed
 *
 * @param <E> file or path or string....
 */
public class WindowsSafeFileAccess<E> {

  public static void exec(FileSystemCall call) {
    new WindowsSafeFileAccess(call).run();
  }

  static final boolean isWindows = StandardSystemProperty.OS_NAME.value().toLowerCase(Locale.ROOT).contains("win");
  public static final int LINUX_IS_THE_BEST = 0;
  public static final int WINDOWS_SUCKS = 5;

  private final FileSystemCall call;

  public WindowsSafeFileAccess(FileSystemCall call) {
    this.call = call;
  }

  void run() {
    int retries = isWindows ? WINDOWS_SUCKS : LINUX_IS_THE_BEST;
    int count = 0;
    do {
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
      try {
        call.run();
        return;
      } catch (AccessDeniedException | FileNotFoundException e) {
        if (isWindows) {
          expSleep(count);
        } else {
          throw new RuntimeException(e);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      count++;
    } while (count < retries);
  }

  private void expSleep(int count) {
    try {
      long sleepTime = new Double((10 * Math.pow((count - 1), 2))).longValue();
      Thread.sleep(sleepTime);
    } catch (InterruptedException e1) {
      Thread.currentThread().interrupt();
    }
  }

  public static interface FileSystemCall {
    void run() throws IOException;
  }
}
