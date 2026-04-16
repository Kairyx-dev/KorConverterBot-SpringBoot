package org.specter.converter.application.service;

import java.time.Clock;
import java.util.Objects;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.result.IgnoreUserResult;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserAlreadyExistsException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

public class AddIgnoreUserService implements AddIgnoreUserUseCase {

  private final LoadIgnoreUserPort loadPort;
  private final SaveIgnoreUserPort savePort;
  private final Clock clock;

  public AddIgnoreUserService(
      LoadIgnoreUserPort loadPort, SaveIgnoreUserPort savePort, Clock clock) {
    this.loadPort = Objects.requireNonNull(loadPort, "loadPort");
    this.savePort = Objects.requireNonNull(savePort, "savePort");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public IgnoreUserResult execute(AddIgnoreUserCommand command) {
    Objects.requireNonNull(command, "command");

    var userId = new UserId(command.userId());
    var channelId = new ChannelId(command.channelId());

    loadPort
        .loadByUserIdAndChannelId(userId, channelId)
        .ifPresent(
            existing -> {
              throw new IgnoreUserAlreadyExistsException(userId.value(), channelId.value());
            });

    var ignoreUser = IgnoreUser.create(userId, channelId, command.name(), clock.instant());
    savePort.save(ignoreUser);

    return new IgnoreUserResult(
        ignoreUser.id().value(), command.userId(), command.channelId(), command.name());
  }
}
