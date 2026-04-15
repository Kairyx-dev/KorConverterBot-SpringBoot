package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.result.IgnoreUserResult;

public interface AddIgnoreUserUseCase {
  IgnoreUserResult execute(AddIgnoreUserCommand command);
}
