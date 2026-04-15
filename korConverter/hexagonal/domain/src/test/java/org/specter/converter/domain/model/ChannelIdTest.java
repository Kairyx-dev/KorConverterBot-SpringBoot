package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChannelIdTest {

  @Test
  @DisplayName("positive value is accepted")
  void positive_value_accepted() {
    var channelId = new ChannelId(99L);
    assertThat(channelId.value()).isEqualTo(99L);
  }

  @Test
  @DisplayName("zero is rejected")
  void zero_rejected() {
    assertThatThrownBy(() -> new ChannelId(0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("positive");
  }

  @Test
  @DisplayName("negative value is rejected")
  void negative_rejected() {
    assertThatThrownBy(() -> new ChannelId(-5L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("positive");
  }

  @Property
  void any_positive_long_is_accepted(@ForAll @LongRange(min = 1) long value) {
    var channelId = new ChannelId(value);
    assertThat(channelId.value()).isEqualTo(value);
  }

  @Property
  void any_non_positive_long_is_rejected(
      @ForAll @LongRange(min = Long.MIN_VALUE, max = 0) long value) {
    assertThatThrownBy(() -> new ChannelId(value)).isInstanceOf(IllegalArgumentException.class);
  }
}
