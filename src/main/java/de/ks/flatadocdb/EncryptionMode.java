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

package de.ks.flatadocdb;

public enum EncryptionMode {
  /**
   * No encryption at all
   */
  NONE,
  /**
   * When transferring the git repository to sync with other devices, it is encrypted
   */
  REPOSITORY_ONLY,
  /**
   * File contents are encrypted
   * currently not supported, doesn't make much sense with asciidoctor...
   */
  CONTENT,
  /**
   * File contents, file names, and folder names are encrypted
   * currently not supported, doesn't make much sense with asciidoctor...
   */
  PATHS;
}
