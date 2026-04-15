package org.specter.converter.domain.exception;

public final class IgnoreUserNotFoundException extends IgnoreUserException {
    public IgnoreUserNotFoundException(long userId, long channelId) {
        super("IgnoreUser not found for userId=%d, channelId=%d".formatted(userId, channelId));
    }
}
