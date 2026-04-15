package org.specter.converter.application.dto.command;

public record RemoveIgnoreUserCommand(long userId, long channelId) {
}
