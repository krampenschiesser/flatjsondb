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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TimeProfilerTest {
  @Test
  public void testParseDurationFull() throws Exception {
    TimeProfiler profiler = new TimeProfiler("bla");
    profiler.fullTimeString = true;
    String parsed = profiler.parseDuration(TimeUnit.SECONDS.toNanos(1) + TimeUnit.MILLISECONDS.toNanos(22) + TimeUnit.MICROSECONDS.toNanos(333) + 444);
    assertEquals("1.022,333,444 s", parsed);

    parsed = profiler.parseDuration(TimeUnit.MILLISECONDS.toNanos(1) + TimeUnit.MICROSECONDS.toNanos(22) + 333);
    assertEquals("1.022,333 ms", parsed);

    parsed = profiler.parseDuration(TimeUnit.MILLISECONDS.toNanos(1) + TimeUnit.NANOSECONDS.toNanos(100));
    assertEquals("1.000,100 ms", parsed);

    parsed = profiler.parseDuration(TimeUnit.MICROSECONDS.toNanos(22) + 333);
    assertEquals("22.333 µs", parsed);


    parsed = profiler.parseDuration(42);
    assertEquals("42 ns", parsed);
  }
}