package org.specter.converter.aplication.inport;

import org.specter.converter.domain.model.IgnoreUser;

public interface DiscordBotInPort {

  boolean checkAvailableStr(String message);

  String engToKor(String message);

  boolean checkIgnoredUser(long userId, long channelId);

  IgnoreUser addIgnoreUser(IgnoreUser user);

  void removeIgnoreUser(long userId, long channelId);
}
