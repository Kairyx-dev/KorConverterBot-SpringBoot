package org.specter.converter.application.dto.command;

public record ConvertMessageCommand(
        String message,
        long guildId,
        long channelId,
        String nickName,
        String effectiveName) {

    public ConvertMessageCommand {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
    }
}
