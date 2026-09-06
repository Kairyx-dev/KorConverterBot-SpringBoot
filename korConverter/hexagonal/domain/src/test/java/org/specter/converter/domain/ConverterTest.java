package org.specter.converter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.specter.converter.domain.service.ConversionDomainService;

class ConverterTest {

    private ConversionDomainService converterCore;

    @BeforeEach
    void setUp() {
        converterCore = new ConversionDomainService();
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
                new TestCase("dkssudgktpdy dlttjqdlqslek!", "안녕하세요 잇섭입니다!"),
                new TestCase("rkskekfkakqktkdkwkckzkxkvkgk", "가나다라마바사아자차카타파하"),
                new TestCase("kijuhynmlop", "ㅏㅑㅓㅕㅗㅛㅜㅡㅣㅐㅔ"),
                new TestCase("dlwndwkdma rkqtwkfggkrt", "이중자음 값잟핛"),
                new TestCase("dlwndahdma dhkwlsWhk dhodnpdml", "이중모음 와진쫘 왜웨의"),
                new TestCase(
                        "wjscp zlqhem q w e r t y u i o p a s d f g h j k l z x c v b n m",
                        "전체 키보드 ㅂ ㅈ ㄷ ㄱ ㅅ ㅛ ㅕ ㅑ ㅐ ㅔ ㅁ ㄴ ㅇ ㄹ ㅎ ㅗ ㅓ ㅏ ㅣ ㅋ ㅌ ㅊ ㅍ ㅠ ㅜ ㅡ"),
                new TestCase("rt sw sg fr fa fq ft fx fv fg qt ghj", "ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ 호ㅓ"),
                new TestCase("whdqt", "종ㅄ"),
                new TestCase("djEja", "어떰"),
                new TestCase("gufak", "혈마"),
                new TestCase("gufak?", "혈마?"),
                new TestCase("gkausgka", "하면함"),
                new TestCase("kt", "ㅏㅅ"),
                new TestCase("dkssudGktpdy", "안녕Gㅏ세요"),
                new TestCase("gkArk", "하A가"),
                new TestCase("gh ZZ dhk", "호 ZZ 와"));
    }

    @ParameterizedTest
    @DisplayName("checkAvailableStr 테스트")
    @MethodSource("provideAvailableParams")
    void t2(AvailableCase param) {
        var available = converterCore.checkAvailableStr(param.message());

        assertThat(available).isEqualTo(param.available());
    }

    protected static Stream<AvailableCase> provideAvailableParams() {
        return Stream.of(
                new AvailableCase("dkssudGktpdy", true),
                new AvailableCase("gkArk", true),
                // 한글키에 대응되는 영문이 하나도 없으면 변환 대상이 아니다
                new AvailableCase("ABC", false),
                // 기존 가드 유지
                new AvailableCase("https://example.com", false),
                new AvailableCase("<@1234567890>", false),
                new AvailableCase("안녕 dk", false),
                new AvailableCase("gk😀rk", false));
    }

    protected record TestCase(String eng, String kor) {}

    protected record AvailableCase(String message, boolean available) {}
}
