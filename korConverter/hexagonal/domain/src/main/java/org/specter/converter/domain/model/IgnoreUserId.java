package org.specter.converter.domain.model;

public record IgnoreUserId(long value) {
  public static final IgnoreUserId UNSAVED = new IgnoreUserId(0L);

  public IgnoreUserId {
    if (value < 0) {
      throw new IllegalArgumentException("IgnoreUserId must not be negative, got: " + value);
    }
  }
}
