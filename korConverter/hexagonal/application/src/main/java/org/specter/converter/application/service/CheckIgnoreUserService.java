package org.specter.converter.application.service;

import java.util.Objects;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

public class CheckIgnoreUserService implements CheckIgnoreUserUseCase {

  private final IgnoreUserQueryPort queryPort;

  public CheckIgnoreUserService(IgnoreUserQueryPort queryPort) {
    this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
  }

  @Override
  public boolean execute(CheckIgnoreUserQuery query) {
    Objects.requireNonNull(query, "query");
    return queryPort.existsByUserIdAndChannelId(query.userId(), query.channelId());
  }
}
