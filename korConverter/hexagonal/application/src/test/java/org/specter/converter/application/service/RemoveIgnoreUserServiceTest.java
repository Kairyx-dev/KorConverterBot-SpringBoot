package org.specter.converter.application.service;

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
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserNotFoundException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.IgnoreUserId;
import org.specter.converter.domain.model.UserId;

@ExtendWith(MockitoExtension.class)
class RemoveIgnoreUserServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private LoadIgnoreUserPort loadPort;

    @Mock
    private SaveIgnoreUserPort savePort;

    private RemoveIgnoreUserService sut;

    @BeforeEach
    void setUp() {
        sut = new RemoveIgnoreUserService(loadPort, savePort, FIXED_CLOCK);
    }

    @Test
    void execute_whenUserExists_marksForRemovalAndDeletes() {
        // given
        var command = new RemoveIgnoreUserCommand(100L, 200L);
        var existing = IgnoreUser.reconstitute(
                new IgnoreUserId(1L), new UserId(100L), new ChannelId(200L), "testUser", FIXED_NOW, FIXED_NOW, 0L);
        given(loadPort.loadByUserIdAndChannelId(new UserId(100L), new ChannelId(200L)))
                .willReturn(Optional.of(existing));

        // when
        sut.execute(command);

        // then
        verify(savePort).delete(existing);
    }

    @Test
    void execute_whenUserNotFound_throwsException() {
        // given
        var command = new RemoveIgnoreUserCommand(100L, 200L);
        given(loadPort.loadByUserIdAndChannelId(new UserId(100L), new ChannelId(200L)))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.execute(command)).isInstanceOf(IgnoreUserNotFoundException.class);
        verify(savePort, never()).delete(any());
    }

    @Test
    void constructor_rejectsNullArguments() {
        assertThatThrownBy(() -> new RemoveIgnoreUserService(null, savePort, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveIgnoreUserService(loadPort, null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveIgnoreUserService(loadPort, savePort, null))
                .isInstanceOf(NullPointerException.class);
    }
}
