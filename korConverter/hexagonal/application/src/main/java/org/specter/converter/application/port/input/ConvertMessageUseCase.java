package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.result.ConvertMessageResult;

public interface ConvertMessageUseCase {
  ConvertMessageResult execute(ConvertMessageCommand command);
}
