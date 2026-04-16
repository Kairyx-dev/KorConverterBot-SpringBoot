package org.specter.converter.application.dto.command;

public record RecordMessageLogCommand(
    long guildId,
    String channel,
    String nickName,
    String effectiveName,
    String message,
    boolean converted,
    String convertedMessage,
    long channelId) {}
