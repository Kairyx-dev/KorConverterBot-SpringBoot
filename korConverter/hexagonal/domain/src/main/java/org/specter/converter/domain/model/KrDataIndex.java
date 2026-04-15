package org.specter.converter.domain.model;

public record KrDataIndex(
    int chosung,
    int jungsung,
    int jongsung
) {

  public static final int NONE_INDEX = -1;

  public static KrDataIndex create() {
    return new KrDataIndex(NONE_INDEX, NONE_INDEX, NONE_INDEX);
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

  public KrDataIndex withChosung(int chosung) {
    return new KrDataIndex(chosung, this.jungsung, this.jongsung);
  }

  public KrDataIndex withJungsung(int jungsung) {
    return new KrDataIndex(this.chosung, jungsung, this.jongsung);
  }

  public KrDataIndex withJongsung(int jongsung) {
    return new KrDataIndex(this.chosung, this.jungsung, jongsung);
  }

  public KrDataIndex clearChosung() {
    return withChosung(NONE_INDEX);
  }

  public KrDataIndex clearJungsung() {
    return withJungsung(NONE_INDEX);
  }

  public KrDataIndex clearJongsung() {
    return withJongsung(NONE_INDEX);
  }
}
