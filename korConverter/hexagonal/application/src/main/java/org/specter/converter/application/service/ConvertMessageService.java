package org.specter.converter.application.service;

import java.util.Objects;
import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.dto.result.ConvertMessageResult;
import org.specter.converter.application.port.input.ConvertMessageUseCase;
import org.specter.converter.application.port.output.RecordMessageLogPort;
import org.specter.converter.domain.model.ConversionDomainService;

public class ConvertMessageService implements ConvertMessageUseCase {

  private final ConversionDomainService conversionDomainService;
  private final RecordMessageLogPort recordMessageLogPort;

  public ConvertMessageService(
      ConversionDomainService conversionDomainService, RecordMessageLogPort recordMessageLogPort) {
    this.conversionDomainService =
        Objects.requireNonNull(conversionDomainService, "conversionDomainService");
    this.recordMessageLogPort =
        Objects.requireNonNull(recordMessageLogPort, "recordMessageLogPort");
  }

  @Override
  public ConvertMessageResult execute(ConvertMessageCommand command) {
    Objects.requireNonNull(command, "command");

    var message = command.message();
    var available = conversionDomainService.checkAvailableStr(message);

    String convertedMessage;
    if (available) {
      convertedMessage = conversionDomainService.engToKor(message);
    } else {
      convertedMessage = "";
    }

    recordMessageLogPort.record(
        new RecordMessageLogCommand(
            command.guildId(),
            String.valueOf(command.channelId()),
            command.nickName(),
            command.effectiveName(),
            message,
            available,
            convertedMessage,
            command.channelId()));

    return new ConvertMessageResult(message, convertedMessage, available);
  }
}
