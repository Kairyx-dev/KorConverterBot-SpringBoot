package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.port.output.RecordMessageLogPort;
import org.specter.converter.domain.model.ConversionDomainService;

@ExtendWith(MockitoExtension.class)
class ConvertMessageServiceTest {

    @Mock
    private RecordMessageLogPort recordMessageLogPort;

    @Captor
    private ArgumentCaptor<RecordMessageLogCommand> logCommandCaptor;

    private ConvertMessageService sut;

    @BeforeEach
    void setUp() {
        // ConversionDomainService is a concrete domain service with no dependencies
        sut = new ConvertMessageService(new ConversionDomainService(), recordMessageLogPort);
    }

    @Test
    void execute_whenMessageIsConvertible_returnsConvertedResult() {
        // given — "gksrnr" is Korean keystroke for "한글"
        var command = new ConvertMessageCommand("gksrnr", 1L, 2L, "nick", "effective");

        // when
        var result = sut.execute(command);

        // then
        assertThat(result.converted()).isTrue();
        assertThat(result.originalMessage()).isEqualTo("gksrnr");
        assertThat(result.convertedMessage()).isNotEmpty();
        verify(recordMessageLogPort).record(any(RecordMessageLogCommand.class));
    }

    @Test
    void execute_whenMessageIsNotConvertible_returnsUnconvertedResult() {
        // given — URL is not convertible
        var command = new ConvertMessageCommand("https://example.com", 1L, 2L, "nick", "effective");

        // when
        var result = sut.execute(command);

        // then
        assertThat(result.converted()).isFalse();
        assertThat(result.convertedMessage()).isEmpty();
        verify(recordMessageLogPort).record(any(RecordMessageLogCommand.class));
    }

    @Test
    void execute_recordsMessageLogWithCorrectFields() {
        // given
        var command = new ConvertMessageCommand("gksrnr", 10L, 20L, "myNick", "myEffective");

        // when
        sut.execute(command);

        // then
        verify(recordMessageLogPort).record(logCommandCaptor.capture());
        var logCommand = logCommandCaptor.getValue();
        assertThat(logCommand.guildId()).isEqualTo(10L);
        assertThat(logCommand.channelId()).isEqualTo(20L);
        assertThat(logCommand.nickName()).isEqualTo("myNick");
        assertThat(logCommand.effectiveName()).isEqualTo("myEffective");
        assertThat(logCommand.message()).isEqualTo("gksrnr");
        assertThat(logCommand.converted()).isTrue();
        assertThat(logCommand.convertedMessage()).isNotEmpty();
    }

    @Test
    void constructor_rejectsNullArguments() {
        assertThatThrownBy(() -> new ConvertMessageService(null, recordMessageLogPort))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConvertMessageService(new ConversionDomainService(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
