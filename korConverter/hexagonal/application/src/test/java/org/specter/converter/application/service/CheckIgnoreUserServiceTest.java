package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

@ExtendWith(MockitoExtension.class)
class CheckIgnoreUserServiceTest {

    @Mock
    private IgnoreUserQueryPort queryPort;

    private CheckIgnoreUserService sut;

    @BeforeEach
    void setUp() {
        sut = new CheckIgnoreUserService(queryPort);
    }

    @Test
    void execute_whenUserExists_returnsTrue() {
        // given
        var query = new CheckIgnoreUserQuery(100L, 200L);
        given(queryPort.existsByUserIdAndChannelId(100L, 200L)).willReturn(true);

        // when
        var result = sut.execute(query);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void execute_whenUserDoesNotExist_returnsFalse() {
        // given
        var query = new CheckIgnoreUserQuery(100L, 200L);
        given(queryPort.existsByUserIdAndChannelId(100L, 200L)).willReturn(false);

        // when
        var result = sut.execute(query);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void constructor_rejectsNullQueryPort() {
        assertThatThrownBy(() -> new CheckIgnoreUserService(null))
                .isInstanceOf(NullPointerException.class);
    }
}
