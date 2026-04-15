package org.specter.converter.domain.exception;

public final class IgnoreUserAlreadyExistsException extends IgnoreUserException {
  public IgnoreUserAlreadyExistsException(long userId, long channelId) {
    super("IgnoreUser already exists for userId=%d, channelId=%d".formatted(userId, channelId));
  }
}
