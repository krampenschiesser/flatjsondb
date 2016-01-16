/**
 * Copyright [2016] [Christian Loehnert]
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
package de.ks.flatadocdb.session;

import java.io.Serializable;

public class NaturalId implements Serializable {
  private static final long serialVersionUID = 1L;

  private Serializable key;
  private Class<?> clazz;

  protected NaturalId() {
  }

  public NaturalId(Class<?> clazz, Serializable key) {
    this.key = key;
    this.clazz = clazz;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public Serializable getKey() {
    return key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NaturalId)) {
      return false;
    }

    NaturalId naturalId1 = (NaturalId) o;

    if (!key.equals(naturalId1.key)) {
      return false;
    }
    return clazz.equals(naturalId1.clazz);

  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + clazz.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("NaturalId{");
    sb.append("naturalId=").append(key);
    sb.append(", clazz=").append(clazz);
    sb.append('}');
    return sb.toString();
  }
}
