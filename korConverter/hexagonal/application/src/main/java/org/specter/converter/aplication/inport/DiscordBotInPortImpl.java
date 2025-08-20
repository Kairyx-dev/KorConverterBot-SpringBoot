package org.specter.converter.aplication.inport;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.specter.converter.aplication.outport.JpaOutPort;
import org.specter.converter.domain.core.ConverterCoreV2;
import org.specter.converter.domain.model.IgnoreUser;

@AllArgsConstructor
public class DiscordBotInPortImpl implements DiscordBotInPort {

  private final ConverterCoreV2 core;
  private final JpaOutPort jpaOutPort;

  @Override
  public boolean checkAvailableStr(String message) {
    return core.checkAvailableStr(message);
  }

  @Override
  public String engToKor(String message) {
    return core.engToKor(message);
  }

  @Override
  public boolean checkIgnoredUser(long userId, long channelId) {
    Optional<IgnoreUser> findUser = jpaOutPort.findIgnoreUser(userId, channelId);

    return findUser.isPresent();
  }

  @Override
  public IgnoreUser addIgnoreUser(IgnoreUser user) {
    return jpaOutPort.saveIgnoreUser(user);
  }

  @Override
  public void removeIgnoreUser(long userId, long channelId) {
    jpaOutPort.deleteIgnoreUser(userId, channelId);
  }
}
