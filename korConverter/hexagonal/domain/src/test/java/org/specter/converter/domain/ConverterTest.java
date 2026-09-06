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
                new TestCase("gh ZZ dhk", "호 ZZ 와"),
                new TestCase("frt", "ㄺㅅ"),
                new TestCase("rts", "ㄳㄴ"),
                new TestCase("frk", "ㄹ가"),
                new TestCase("gkfrk", "할가"),

                // 된소리(ㄸㅃㅉ)는 종성이 될 수 없어 앞글자를 확정하고 낱자로 남는다
                new TestCase("ekE", "다ㄸ"),
                new TestCase("ekQ", "다ㅃ"),
                new TestCase("ekW", "다ㅉ"),

                // 겹받침 뒤에 모음이 오면 뒷자음만 다음 글자의 초성으로 넘어간다
                new TestCase("dkfgdk", "앓아"),
                new TestCase("djqtdl", "없이"),
                new TestCase("dkswdk", "앉아"),
                new TestCase("rkqtdl", "값이"),

                // 이중모음 조합과, 조합 불가한 모음이 이어질 때의 분리
                new TestCase("ghks", "환"),
                new TestCase("godyd", "해용"),
                new TestCase("hlk", "ㅚㅏ"),
                new TestCase("mlk", "ㅢㅏ"),
                new TestCase("njl", "ㅝㅣ"),
                new TestCase("kk", "ㅏㅏ"),
                new TestCase("hkk", "ㅘㅏ"),

                // 초성 없이 이어지는 자음 나열 (겹자음 조합 후 다시 낱자)
                new TestCase("rrr", "ㄱㄱㄱ"),
                new TestCase("rtr", "ㄳㄱ"),
                new TestCase("fgfg", "ㅀㅀ"),
                new TestCase("TT", "ㅆㅆ"),

                // 대문자(된소리/이중모음) 키
                new TestCase("REQTWOP", "ㄲㄸㅃㅆ쨰ㅖ"),
                new TestCase("REkQk", "ㄲ따빠"),

                // 한글키 아닌 문자는 조합을 끊고 원본 그대로 통과된다
                new TestCase("gk!gk", "하!하"),
                new TestCase("gk1gk", "하1하"),
                new TestCase("gkGgk", "하G하"),
                new TestCase("gk gk", "하 하"),

                // 마지막 글자가 미완성인 경우
                new TestCase("dkssudgktp", "안녕하세"),
                new TestCase("gks", "한"),
                new TestCase("g", "ㅎ"),
                new TestCase("k", "ㅏ"),
                new TestCase("", ""),

                // 실제 문장
                new TestCase("dhsmf skfTlrk whgspdy", "오늘 날씨가 좋네요"),
                new TestCase("rkqtdmf clfjTek", "값을 치렀다"),
                new TestCase("dkswdktj dlfrsmsek", "앉아서 읽는다"),
                new TestCase("tlfgdjdy!", "싫어요!"),
                new TestCase("dhlrnrdj gkrtmq", "외국어 학습"),
                new TestCase("EmldjTmrl xmffuTek", "띄어쓰기 틀렸다"),
                new TestCase("dho rmfoTdj?", "왜 그랬어?"),

                // engToKor 은 통과 정책과 무관하게 한글키가 아닌 문자를 항상 원본으로 내보낸다
                new TestCase("gk~", "하~"),
                new TestCase("gk[gk", "하[하"),
                new TestCase("gk`gk", "하`하"));
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
                new AvailableCase("gk😀rk", false),
                // 한글키 문자가 하나도 없으면 변환 대상이 아니다
                new AvailableCase("", false),
                new AvailableCase("1234", false),
                new AvailableCase("!!!", false),
                new AvailableCase("   ", false),
                new AvailableCase("GG", false),
                // 한글키 문자가 하나라도 있으면 변환 대상이다
                new AvailableCase("a", true),
                new AvailableCase("gg", true),
                new AvailableCase("gk!gk", true),
                new AvailableCase("gk1gk", true),
                // ASCII 기호는 통과 대상이다 (경계값)
                new AvailableCase("gk~", true),
                new AvailableCase("gk[gk", true),
                new AvailableCase("gk{gk", true),
                // 백틱은 디스코드 코드 블록 구분자라 의도적으로 통과시키지 않는다
                new AvailableCase("gk`gk", false),
                // isSpecificCode 각 범위의 경계값
                new AvailableCase("gk!", true),
                new AvailableCase("gk/", true),
                new AvailableCase("gk:", true),
                new AvailableCase("gk@", true),
                new AvailableCase("gk[", true),
                new AvailableCase("gk_", true),
                new AvailableCase("gk{", true),
                // isNumber 경계값
                new AvailableCase("gk0", true),
                new AvailableCase("gk9", true),
                // isAlphabet 경계값
                new AvailableCase("gkA", true),
                new AvailableCase("gkZ", true),
                // 멘션 판정은 앞뒤가 모두 꺾쇠일 때만 적용된다
                new AvailableCase("<gk", true),
                new AvailableCase("gk>", true),
                new AvailableCase("http://example.com", false));
    }

    protected record TestCase(String eng, String kor) {}

    protected record AvailableCase(String message, boolean available) {}
}
