package org.specter.converter.adapter.persistence.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.specter.converter.adapter.persistence.generated.Tables.MESSAGE_LOG;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.specter.converter.adapter.persistence.AdapterTestBase;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdapterTestBase.AdapterTestConfig.class)
class MessageLogRecordAdapterTest extends AdapterTestBase {

    @Autowired
    DSLContext dsl;

    @Autowired
    MessageLogRecordAdapter adapter;

    @BeforeEach
    void cleanup() {
        dsl.deleteFrom(MESSAGE_LOG).execute();
    }

    @Test
    void record_inserts_a_row() {
        var command = new RecordMessageLogCommand(
                12345L, "general", "testNick", "testEffective", "dkssud", true, "안녕", 67890L);

        adapter.record(command);

        var rows = dsl.selectFrom(MESSAGE_LOG).fetch();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getGuild()).isEqualTo("12345");
        assertThat(rows.getFirst().getChannel()).isEqualTo("general");
        assertThat(rows.getFirst().getNickName()).isEqualTo("testNick");
        assertThat(rows.getFirst().getEffectiveName()).isEqualTo("testEffective");
        assertThat(rows.getFirst().getMessage()).isEqualTo("dkssud");
        assertThat(rows.getFirst().getIsConverted()).isTrue();
        assertThat(rows.getFirst().getConvertedMessage()).isEqualTo("안녕");
        assertThat(rows.getFirst().getChannelId()).isEqualTo(67890L);
    }

    @Test
    void record_inserts_unconverted_message() {
        var command = new RecordMessageLogCommand(11111L, "random", "nick", "effective", "hello", false, null, 22222L);

        adapter.record(command);

        var rows = dsl.selectFrom(MESSAGE_LOG).fetch();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getIsConverted()).isFalse();
        assertThat(rows.getFirst().getConvertedMessage()).isNull();
    }
}
