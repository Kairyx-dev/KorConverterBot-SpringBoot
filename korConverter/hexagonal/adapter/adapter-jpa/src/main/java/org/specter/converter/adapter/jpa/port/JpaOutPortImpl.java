package org.specter.converter.adapter.jpa.port;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.specter.converter.adapter.jpa.service.IgnoreUserService;
import org.specter.converter.adapter.jpa.service.MessageLogService;
import org.specter.converter.aplication.outport.JpaOutPort;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.MessageLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@NullMarked
public class JpaOutPortImpl implements JpaOutPort {

  private final IgnoreUserService ignoreUserService;
  private final MessageLogService messageLogService;

  public JpaOutPortImpl(IgnoreUserService ignoreUserService, MessageLogService messageLogService) {
    this.ignoreUserService = ignoreUserService;
    this.messageLogService = messageLogService;
  }

  @Override
  public List<IgnoreUser> findAllIgnoreUsers() {
    return ignoreUserService.findAll();
  }

  @Override
  public Optional<IgnoreUser> findIgnoreUser(long userId, long channelId) {
    return ignoreUserService.findByUserId(userId, channelId);
  }

  @Override
  public IgnoreUser saveIgnoreUser(IgnoreUser ignoreUser) {
    return ignoreUserService.save(ignoreUser);
  }

  @Override
  public MessageLog saveMessageLog(MessageLog messageLog) {
    return messageLogService.save(messageLog);
  }

  @Override
  @Transactional
  public void deleteIgnoreUser(long userId, long channelId) {
    ignoreUserService.deleteByUserId(userId, channelId);
  }
}
