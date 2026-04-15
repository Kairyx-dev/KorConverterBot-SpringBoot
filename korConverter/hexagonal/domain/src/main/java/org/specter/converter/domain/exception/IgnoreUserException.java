package org.specter.converter.domain.exception;

public sealed class IgnoreUserException extends RuntimeException
        permits IgnoreUserNotFoundException, IgnoreUserAlreadyExistsException {
    protected IgnoreUserException(String message) {
        super(message);
    }
}
