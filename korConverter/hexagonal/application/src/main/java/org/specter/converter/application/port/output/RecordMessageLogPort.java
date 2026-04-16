package org.specter.converter.application.port.output;

import org.specter.converter.application.dto.command.RecordMessageLogCommand;

public interface RecordMessageLogPort {
  void record(RecordMessageLogCommand command);
}
