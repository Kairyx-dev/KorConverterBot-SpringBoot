package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserAlreadyExistsException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

@ExtendWith(MockitoExtension.class)
class AddIgnoreUserServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private LoadIgnoreUserPort loadPort;

    @Mock
    private SaveIgnoreUserPort savePort;

    private AddIgnoreUserService sut;

    @BeforeEach
    void setUp() {
        sut = new AddIgnoreUserService(loadPort, savePort, FIXED_CLOCK);
    }

    @Test
    void execute_whenUserDoesNotExist_createsAndSaves() {
        // given
        var command = new AddIgnoreUserCommand(100L, 200L, "testUser");
        given(loadPort.loadByUserIdAndChannelId(new UserId(100L), new ChannelId(200L)))
                .willReturn(Optional.empty());

        // when
        var result = sut.execute(command);

        // then
        assertThat(result.userId()).isEqualTo(100L);
        assertThat(result.channelId()).isEqualTo(200L);
        assertThat(result.name()).isEqualTo("testUser");
        verify(savePort).save(any(IgnoreUser.class));
    }

    @Test
    void execute_whenUserAlreadyExists_throwsException() {
        // given
        var command = new AddIgnoreUserCommand(100L, 200L, "testUser");
        var existing = IgnoreUser.create(new UserId(100L), new ChannelId(200L), "testUser", FIXED_NOW);
        given(loadPort.loadByUserIdAndChannelId(new UserId(100L), new ChannelId(200L)))
                .willReturn(Optional.of(existing));

        // when / then
        assertThatThrownBy(() -> sut.execute(command))
                .isInstanceOf(IgnoreUserAlreadyExistsException.class);
        verify(savePort, never()).save(any());
    }

    @Test
    void constructor_rejectsNullArguments() {
        assertThatThrownBy(() -> new AddIgnoreUserService(null, savePort, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AddIgnoreUserService(loadPort, null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AddIgnoreUserService(loadPort, savePort, null))
                .isInstanceOf(NullPointerException.class);
    }
}
