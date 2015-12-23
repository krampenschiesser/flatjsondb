package de.ks.flatadocdb.entity;

import java.nio.file.Path;

public class BaseEntityFriend {
  private final BaseEntity delegate;

  public BaseEntityFriend(BaseEntity delegate) {
    this.delegate = delegate;
  }

  public void setPathInRepo(Path pathInRepo) {
    delegate.pathInRepository = pathInRepo;
  }

}