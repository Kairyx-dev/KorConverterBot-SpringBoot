package org.specter.converter.adapter.bot.entity;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BotCommand {
  ECO_VERSION("version", "봇의 현재 버전이 몇인지 물어봅니다"),
  ECO_TEST("eco", "dump unknown description"),
  UNKNOWN("unknown", "지정한 대사를 봇이 입력하도록 명령합니다. (테스트용)");

  private final String command;
  private final String description;

  public static BotCommand fromCommand(String command) {
    return Arrays.stream(BotCommand.values())
        .filter(botCommand -> botCommand.getCommand().equals(command))
        .findAny()
        .orElse(BotCommand.UNKNOWN);
  }
}
