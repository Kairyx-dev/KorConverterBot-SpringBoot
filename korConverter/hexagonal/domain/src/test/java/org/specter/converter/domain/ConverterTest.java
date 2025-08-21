package org.specter.converter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.specter.converter.domain.core.ConverterCoreV2;

class ConverterTest {

  private ConverterCoreV2 converterCore;

  @BeforeEach
  void setUp() {
    converterCore = new ConverterCoreV2();
  }


  @ParameterizedTest
  @DisplayName("convert 테스트")
  @MethodSource("provideParams")
  void t1(TestCase param) {
    var converted = converterCore.engToKor(param.eng());

    assertThat(converted).isEqualTo(param.kor());
  }

  protected static Stream<TestCase> provideParams() {
    return Stream.of(
        TestCase.builder()
            .eng("dkssudgktpdy dlttjqdlqslek!")
            .kor("안녕하세요 잇섭입니다!")
            .build(),
        TestCase.builder()
            .eng("rkskekfkakqktkdkwkckzkxkvkgk")
            .kor("가나다라마바사아자차카타파하")
            .build(),
        TestCase.builder()
            .eng("kijuhynmlop")
            .kor("ㅏㅑㅓㅕㅗㅛㅜㅡㅣㅐㅔ")
            .build(),
        TestCase.builder()
            .eng("dlwndwkdma rkqtwkfggkrt")
            .kor("이중자음 값잟핛")
            .build(),
        TestCase.builder()
            .eng("dlwndahdma dhkwlsWhk dhodnpdml")
            .kor("이중모음 와진쫘 왜웨의")
            .build(),
        TestCase.builder()
            .eng("wjscp zlqhem q w e r t y u i o p a s d f g h j k l z x c v b n m")
            .kor("전체 키보드 ㅂ ㅈ ㄷ ㄱ ㅅ ㅛ ㅕ ㅑ ㅐ ㅔ ㅁ ㄴ ㅇ ㄹ ㅎ ㅗ ㅓ ㅏ ㅣ ㅋ ㅌ ㅊ ㅍ ㅠ ㅜ ㅡ")
            .build(),
        TestCase.builder()
            .eng("rt sw sg fr fa fq ft fx fv fg qt ghj")
            .kor("ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ 호ㅓ")
            .build(),
        TestCase.builder()
            .eng("whdqt")
            .kor("종ㅄ")
            .build(),
        TestCase.builder()
            .eng("djEja")
            .kor("어떰")
            .build()
    );
  }

  @Builder
  protected record TestCase(
      String eng,
      String kor
  ) {

  }
}
