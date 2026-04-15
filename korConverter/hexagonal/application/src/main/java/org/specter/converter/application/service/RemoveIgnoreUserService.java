package org.specter.converter.application.service;

import java.time.Clock;
import java.util.Objects;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserNotFoundException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.UserId;

public class RemoveIgnoreUserService implements RemoveIgnoreUserUseCase {

    private final LoadIgnoreUserPort loadPort;
    private final SaveIgnoreUserPort savePort;
    private final Clock clock;

    public RemoveIgnoreUserService(LoadIgnoreUserPort loadPort,
                                   SaveIgnoreUserPort savePort,
                                   Clock clock) {
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort");
        this.savePort = Objects.requireNonNull(savePort, "savePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void execute(RemoveIgnoreUserCommand command) {
        Objects.requireNonNull(command, "command");

        var userId = new UserId(command.userId());
        var channelId = new ChannelId(command.channelId());

        var ignoreUser = loadPort.loadByUserIdAndChannelId(userId, channelId)
                .orElseThrow(() -> new IgnoreUserNotFoundException(
                        userId.value(), channelId.value()));

        ignoreUser.markForRemoval(clock.instant());
        savePort.delete(ignoreUser);
    }
}
