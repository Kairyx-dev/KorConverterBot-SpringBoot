package org.specter.converter.domain.model;

import lombok.Builder;
import lombok.With;
import org.jspecify.annotations.NullMarked;

@Builder
@With
@NullMarked
public record KrDataIndex(
    int chosung,
    int jungsung,
    int jongsung
) {

  public static final int NONE_INDEX = -1;

  public static KrDataIndex create() {
    return KrDataIndex.builder()
        .chosung(NONE_INDEX)
        .jungsung(NONE_INDEX)
        .jongsung(NONE_INDEX)
        .build();
  }

  public boolean chosungIndexed() {
    return chosung != NONE_INDEX;
  }

  public boolean jungsungIndexed() {
    return jungsung != NONE_INDEX;
  }

  public boolean jongsungIndexed() {
    return jongsung != NONE_INDEX;
  }

  public KrDataIndex clearChosung() {
    return this.withChosung(NONE_INDEX);
  }

  public KrDataIndex clearJungsung() {
    return this.withJungsung(NONE_INDEX);
  }

  public KrDataIndex clearJongsung() {
    return this.withJongsung(NONE_INDEX);
  }
}
