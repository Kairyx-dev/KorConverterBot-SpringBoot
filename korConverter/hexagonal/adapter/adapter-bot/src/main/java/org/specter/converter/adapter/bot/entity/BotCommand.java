package org.specter.converter.adapter.bot.entity;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BotCommand {
  ECO_VERSION("version", "봇의 현재 버전이 몇인지 물어봅니다"),
  ECO_TEST("eco", "지정한 대사를 봇이 입력하도록 명령합니다. (테스트용)"),
  IGNORE_ME("ignore", "봇이 사용자의 대화를 무시하도록 합니다."),
  UN_IGNORE_ME("unignore", "봇이 사용자의 대화를 다시 들을수 있도록 합니다."),
  UNKNOWN("unknown", "dump unknown description");

  private final String command;
  private final String description;

  public static BotCommand fromCommand(String command) {
    return Arrays.stream(BotCommand.values())
        .filter(botCommand -> botCommand.getCommand().equals(command))
        .findAny()
        .orElse(BotCommand.UNKNOWN);
  }
}
