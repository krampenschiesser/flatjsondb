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

import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class TimeProfiler {
  @FunctionalInterface
  public static interface RunnableWithException<E extends Throwable> {
    void run() throws E;
  }

  public static <E extends Throwable> TimeProfiler run(String name, RunnableWithException<E> r) throws E {
    TimeProfiler profiler = new TimeProfiler(name).start();
    try {
      r.run();
    } finally {
      profiler.stop();
    }
    return profiler;
  }

  public static TimeProfiler run(String name, Runnable r) {
    TimeProfiler profiler = new TimeProfiler(name).start();
    try {
      r.run();
    } finally {
      profiler.stop();
    }
    return profiler;
  }

  long startNs = -1;
  long elapsedNs = 0;
  boolean fullTimeString;
  private final String name;

  public TimeProfiler(String name) {
    this.name = name;
  }

  public boolean isFullTimeString() {
    return fullTimeString;
  }

  public void setFullTimeString(boolean fullTimeString) {
    this.fullTimeString = fullTimeString;
  }

  public TimeProfiler start() {
    startNs = System.nanoTime();
    return this;
  }

  public TimeProfiler stop() {
    if (startNs == -1) {
      throw new IllegalStateException("Profiler was never started");
    }
    elapsedNs += System.nanoTime() - startNs;
    startNs = -1;
    return this;
  }

  public String getName() {
    return name;
  }

  public void logInfo(Logger logger) {
    logger.info("{} took {}", name, parseDuration(getDurationNs()));
  }

  public void logDebug(Logger logger) {
    logger.debug("{} took {}", name, parseDuration(getDurationNs()));
  }

  String parseDuration(long duration) {
    long seconds = TimeUnit.NANOSECONDS.toSeconds(duration);
    long millis = TimeUnit.NANOSECONDS.toMillis(duration);
    long micros = TimeUnit.NANOSECONDS.toMicros(duration);
    long nanos = duration;


    if (seconds > 0) {
      long remainingNanos = nanos - TimeUnit.SECONDS.toNanos(seconds);
      long remainingMillis = remainingNanos / 1000 / 1000;
      long remainingMiros = remainingNanos / 1000 % 1000;
      remainingNanos %= 1000;

      if (fullTimeString) {
        return seconds + "." + String.format("%03d", remainingMillis) + "," + String.format("%03d", remainingMiros) + "," + String.format("%03d", remainingNanos) + " s";
      } else {
        return seconds + "." + String.format("%03d", remainingMillis) + " s";
      }
    } else if (millis > 0) {
      long remainingNanos = nanos - TimeUnit.MILLISECONDS.toNanos(millis);
      long remainingMiros = remainingNanos / 1000 % 1000;
      remainingNanos %= 1000;
      if (fullTimeString) {
        return millis + "." + String.format("%03d", remainingMiros) + "," + String.format("%03d", remainingNanos) + " ms";
      } else {
        return millis + "." + String.format("%03d", remainingMiros) + " ms";
      }
    } else if (micros > 0) {
      long remainingNanos = nanos - TimeUnit.MICROSECONDS.toNanos(micros);
      return micros + "." + String.format("%03d", remainingNanos) + " µs";
    } else {
      return nanos + " ns";
    }
  }

  protected long getDurationNs() {
    if (startNs != -1) {
      throw new IllegalStateException("Stop profiler first");
    }
    return elapsedNs;
  }

}
