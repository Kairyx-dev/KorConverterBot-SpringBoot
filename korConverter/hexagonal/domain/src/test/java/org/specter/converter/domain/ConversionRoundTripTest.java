package org.specter.converter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.specter.converter.domain.service.ConversionDomainService;

/**
 * 왕복(round-trip) 검증.
 *
 * <p>기대값을 구현에서 얻지 않고 유니코드 한글 조합 규칙에서 역산한다. 완성형 한글을 초/중/종성으로 분해해 2벌식 키스트로크를 만들고, 그 키스트로크를 {@code engToKor}에 넣으면 원래
 * 한글이 그대로 나와야 한다. 아래 키 배열은 {@code KeyboardIndex}를 참조하지 않고 독립적으로 선언해 구현과 같은 실수를 공유하지 않도록 한다.
 */
class ConversionRoundTripTest {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_LAST = 0xD7A3;
    private static final int JUNG_COUNT = 21;
    private static final int JONG_COUNT = 28;

    /** 초성 19자의 키. */
    private static final String CHO_KEYS = "rRseEfaqQtTdwWczxvg";

    /** 중성 21자의 키. 이중모음은 두 키를 순서대로 누른다. */
    private static final String[] JUNG_KEYS = {
        "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk", "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml", "l"
    };

    /** 종성 28자(무받침 포함)의 키. 겹받침은 두 키를 순서대로 누른다. */
    private static final String[] JONG_KEYS = {
        "", "r", "R", "rt", "s", "sw", "sg", "e", "f", "fr", "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "qt", "t",
        "T", "d", "w", "c", "z", "x", "v", "g"
    };

    /** 한글키에 대응되지 않아 원본 그대로 통과되어야 하는 문자들. */
    private static final String PASSTHROUGH_CHARS = " !?.,-@#$%^&*()1234567890ABCDFGHIJKLMNSUVXYZ";

    private final ConversionDomainService sut = new ConversionDomainService();

    @Test
    @DisplayName("완성형 한글 11,172자가 모두 왕복 변환된다")
    void everyHangulSyllableRoundTrips() {
        for (var code = HANGUL_BASE; code <= HANGUL_LAST; code++) {
            var syllable = String.valueOf((char) code);

            assertThat(sut.engToKor(keystrokesOf(syllable)))
                    .as("syllable=%s", syllable)
                    .isEqualTo(syllable);
        }
    }

    @Property(tries = 500)
    @Label("임의의 한글 단어가 왕복 변환된다")
    void anyHangulWordRoundTrips(@ForAll("hangulWords") String word) {
        assertThat(sut.engToKor(keystrokesOf(word))).isEqualTo(word);
    }

    @Property(tries = 500)
    @Label("한글키에 대응되지 않는 문자가 섞여도 그 문자는 원본 그대로 통과된다")
    void passthroughCharsSurviveConversion(
            @ForAll("hangulWords") String first,
            @ForAll("passthroughChars") char passthrough,
            @ForAll("hangulWords") String second) {
        var expected = first + passthrough + second;

        assertThat(sut.engToKor(keystrokesOf(expected))).isEqualTo(expected);
    }

    @Provide
    Arbitrary<String> hangulWords() {
        return Arbitraries.integers()
                .between(HANGUL_BASE, HANGUL_LAST)
                .list()
                .ofMinSize(1)
                .ofMaxSize(8)
                .map(codes -> codes.stream()
                        .map(code -> String.valueOf((char) code.intValue()))
                        .collect(Collectors.joining()));
    }

    @Provide
    Arbitrary<Character> passthroughChars() {
        return Arbitraries.of(PASSTHROUGH_CHARS.chars().mapToObj(c -> (char) c).toArray(Character[]::new));
    }

    /** 한글 문자열을 2벌식 키스트로크로 되돌린다. 한글이 아닌 문자는 그대로 둔다. */
    private static String keystrokesOf(String text) {
        var keystrokes = new StringBuilder();

        for (var position = 0; position < text.length(); position++) {
            var ch = text.charAt(position);
            if (ch < HANGUL_BASE || ch > HANGUL_LAST) {
                keystrokes.append(ch);
                continue;
            }

            var offset = ch - HANGUL_BASE;
            keystrokes
                    .append(CHO_KEYS.charAt(offset / (JUNG_COUNT * JONG_COUNT)))
                    .append(JUNG_KEYS[(offset / JONG_COUNT) % JUNG_COUNT])
                    .append(JONG_KEYS[offset % JONG_COUNT]);
        }

        return keystrokes.toString();
    }
}
