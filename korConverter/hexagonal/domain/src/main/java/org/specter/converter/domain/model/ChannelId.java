package org.specter.converter.domain.model;

public record ChannelId(long value) {
  public ChannelId {
    if (value <= 0) {
      throw new IllegalArgumentException("ChannelId must be positive, got: " + value);
    }
  }
}
