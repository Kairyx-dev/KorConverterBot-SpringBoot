package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IgnoreUserIdTest {

    @Test
    @DisplayName("zero is allowed for UNSAVED sentinel")
    void zero_allowed_for_unsaved() {
        assertThat(IgnoreUserId.UNSAVED.value()).isEqualTo(0L);
    }

    @Test
    @DisplayName("positive value is accepted")
    void positive_value_accepted() {
        var id = new IgnoreUserId(7L);
        assertThat(id.value()).isEqualTo(7L);
    }

    @Test
    @DisplayName("negative value is rejected")
    void negative_rejected() {
        assertThatThrownBy(() -> new IgnoreUserId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Property
    void any_non_negative_long_is_accepted(@ForAll @LongRange(min = 0) long value) {
        var id = new IgnoreUserId(value);
        assertThat(id.value()).isEqualTo(value);
    }

    @Property
    void any_negative_long_is_rejected(@ForAll @LongRange(min = Long.MIN_VALUE, max = -1) long value) {
        assertThatThrownBy(() -> new IgnoreUserId(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
