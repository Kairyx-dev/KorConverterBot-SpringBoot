package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;

public interface RemoveIgnoreUserUseCase {
    void execute(RemoveIgnoreUserCommand command);
}
