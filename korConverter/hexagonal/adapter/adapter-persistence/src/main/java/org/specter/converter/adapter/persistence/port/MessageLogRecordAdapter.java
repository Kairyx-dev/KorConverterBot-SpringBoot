package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.MESSAGE_LOG;

import java.util.Objects;
import org.jooq.DSLContext;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.port.output.RecordMessageLogPort;

public class MessageLogRecordAdapter implements RecordMessageLogPort {

  private final DSLContext dsl;

  public MessageLogRecordAdapter(DSLContext dsl) {
    this.dsl = Objects.requireNonNull(dsl, "dsl");
  }

  @Override
  public void record(RecordMessageLogCommand command) {
    Objects.requireNonNull(command, "command");
    dsl.insertInto(MESSAGE_LOG)
        .set(MESSAGE_LOG.GUILD, String.valueOf(command.guildId()))
        .set(MESSAGE_LOG.CHANNEL, command.channel())
        .set(MESSAGE_LOG.NICK_NAME, command.nickName())
        .set(MESSAGE_LOG.EFFECTIVE_NAME, command.effectiveName())
        .set(MESSAGE_LOG.MESSAGE, command.message())
        .set(MESSAGE_LOG.IS_CONVERTED, command.converted())
        .set(MESSAGE_LOG.CONVERTED_MESSAGE, command.convertedMessage())
        .set(MESSAGE_LOG.CHANNEL_ID, command.channelId())
        .execute();
  }
}
