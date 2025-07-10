package org.specter.converter.adapter.jpa.port;

import java.util.List;
import java.util.Optional;
import org.specter.converter.adapter.jpa.service.IgnoreUserService;
import org.specter.converter.aplication.outport.JpaOutPort;
import org.specter.converter.domain.model.IgnoreUser;
import org.springframework.stereotype.Component;

@Component
public class JpaOutPortImpl implements JpaOutPort {

  private final IgnoreUserService ignoreUserService;

  public JpaOutPortImpl(IgnoreUserService ignoreUserService) {
    this.ignoreUserService = ignoreUserService;
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
  public void deleteIgnoreUser(long userId, long channelId) {
    ignoreUserService.deleteByUserId(userId, channelId);
  }
}
