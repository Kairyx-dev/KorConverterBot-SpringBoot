package org.specter.converter.application.dto.command;

public record AddIgnoreUserCommand(long userId, long channelId, String name) {
  public AddIgnoreUserCommand {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
  }
}
