package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    @DisplayName("positive value is accepted")
    void positive_value_accepted() {
        var userId = new UserId(42L);
        assertThat(userId.value()).isEqualTo(42L);
    }

    @Test
    @DisplayName("zero is rejected")
    void zero_rejected() {
        assertThatThrownBy(() -> new UserId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("negative value is rejected")
    void negative_rejected() {
        assertThatThrownBy(() -> new UserId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Property
    void any_positive_long_is_accepted(@ForAll @LongRange(min = 1) long value) {
        var userId = new UserId(value);
        assertThat(userId.value()).isEqualTo(value);
    }

    @Property
    void any_non_positive_long_is_rejected(@ForAll @LongRange(min = Long.MIN_VALUE, max = 0) long value) {
        assertThatThrownBy(() -> new UserId(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
