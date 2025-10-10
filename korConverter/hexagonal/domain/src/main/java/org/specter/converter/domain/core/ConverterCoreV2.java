package org.specter.converter.domain.core;

import org.specter.converter.domain.model.KeyboardIndex;
import org.specter.converter.domain.model.KrDataIndex;

/**
 * Core v1 과의 차이점 <br/> - 최대한 하드코딩 되지 않도록 KrDataIndex 레코드 사용 <br/> - 키보드 인덱스 분리 및 raw 인덱스 사용 대신 Map 사용 <br/> - 메소드 크기
 * 조정
 *
 * @see KrDataIndex
 * @see KeyboardIndex
 */
public class ConverterCoreV2 {

  public String engToKor(String eng) {
    var res = new StringBuilder();

    if (eng.isEmpty()) {
      return "";
    }

    var indexInfo = KrDataIndex.create();

    for (char engCh : eng.toCharArray()) {
      var engPosition = KeyboardIndex.ENG_INDEX_MAP.get(engCh);

      if (engPosition == null) { // 영어키가 아닌경우 (특수문자 혹은 띄어쓰기)
        indexInfo = handleSpecialChar(engCh, res, indexInfo);
      } else if (engPosition < KeyboardIndex.KOR_INDEX_MAP.get('ㅏ')) { // 자음
        indexInfo = handleConsonant(indexInfo, res, engPosition);
      } else { // 모음
        indexInfo = handleVowels(indexInfo, res, engPosition);
      }
    }

    // 마지막 한글이 있으면 처리
    var end = makeEndOfIndex(indexInfo);
    if (end != null) {
      res.append(end);
    }

    return res.toString();
  }

  private KrDataIndex handleSpecialChar(char engCh, StringBuilder resultStringBuilder, KrDataIndex indexInfo) {
    // 특수문자 입력으로 인해 지정 초기화 및 입력된 특수문자 그대로 삽입
    var end = makeEndOfIndex(indexInfo);
    if (end != null) {
      resultStringBuilder.append(end);
    }

    resultStringBuilder.append(engCh);
    indexInfo = KrDataIndex.create();
    return indexInfo;
  }

  private KrDataIndex handleVowels(KrDataIndex indexInfo, StringBuilder resultStringBuilder, int engPosition) {
    var info = indexInfo;
    if (info.jongsungIndexed()) { //종성이 있는경우 앞의 종성을 초성 으로 사용
      info = useJongsungToChosungForJungsung(indexInfo, resultStringBuilder);
    }

    info = insertJungsung(info, engPosition, resultStringBuilder);
    return info;
  }

  private KrDataIndex handleConsonant(KrDataIndex indexInfo, StringBuilder resultStringBuilder, int engPosition) {
    final char currentKor = KeyboardIndex.KOR_KEY.charAt(engPosition);
    // 현재 입력된 자음(초성) 위치
    final int currentChoIndex = KeyboardIndex.CHOSUNG_INDEX_MAP.get(currentKor);

    if (indexInfo.jungsungIndexed() && !indexInfo.chosungIndexed()) { // 초성없이 중성만 있음
      // 이전에 입력했던 중성을 그대로 결과에 넣고 초성위치 지정
      resultStringBuilder.append(KeyboardIndex.JUNG_DATA.charAt(indexInfo.jungsung()));
      indexInfo = indexInfo.clearJungsung().withChosung(currentChoIndex);
    }

    if (indexInfo.jungsungIndexed()) {
      indexInfo = insertJongsung(indexInfo, resultStringBuilder, currentChoIndex);
    } else {// 이전 중성입력이 없음
      indexInfo = insertChosung(indexInfo, resultStringBuilder, currentChoIndex);
    }

    return indexInfo;
  }

  private KrDataIndex insertChosung(final KrDataIndex indexInfo, StringBuilder resultStringBuilder,
      int currentChoIndex) {
    var resultInfo = indexInfo;

    if (!resultInfo.chosungIndexed() && resultInfo.jungsungIndexed()) { // 초성없이 종성있음, 종성 처리
      resultStringBuilder.append(KeyboardIndex.JONG_DATA.charAt(resultInfo.jongsung()));
      resultInfo = resultInfo.clearJongsung();
    }

    if (!resultInfo.chosungIndexed()) { // 초성 지정
      resultInfo = resultInfo.withChosung(currentChoIndex);
    } else { // 기존 초성 존재
      char existCho = KeyboardIndex.CHO_DATA.charAt(resultInfo.chosung());
      char newCho = KeyboardIndex.CHO_DATA.charAt(currentChoIndex);
      Character doubleChar = makeDoubleChar(existCho, newCho);

      if (doubleChar != null) { // 이중종성이 가능한경우 종성 지정
        resultInfo = resultInfo.clearChosung().withJongsung(KeyboardIndex.JONGSUNG_INDEX_MAP.get(doubleChar));
      } else {
        resultStringBuilder.append(existCho);
        resultInfo = resultInfo.withChosung(currentChoIndex);
      }
    }

    return resultInfo;
  }

  private KrDataIndex insertJongsung(final KrDataIndex indexInfo, StringBuilder resultStringBuilder,
      int currentChoIndex) {
    if (!indexInfo.jongsungIndexed()) { // 종성이 없는경우 입력된 값은 종성으로 사용
      char kor = KeyboardIndex.CHO_DATA.charAt(currentChoIndex);
      Integer jongsung = KeyboardIndex.JONGSUNG_INDEX_MAP.get(kor);

      if (jongsung == null) { // 종성 사용이 불가능한 초성이 입력 됨
        resultStringBuilder.append(composeSyllableFromIndex(indexInfo));
        return KrDataIndex.create().withChosung(currentChoIndex);
      } else {
        return indexInfo.withJongsung(jongsung);
      }
    } else {
      var existJongsung = KeyboardIndex.JONG_DATA.charAt(indexInfo.jongsung());
      var newJongsung = KeyboardIndex.CHO_DATA.charAt(currentChoIndex);

      Character doubleChar = makeDoubleChar(existJongsung, newJongsung);

      if (doubleChar != null) {
        return indexInfo.withJongsung(KeyboardIndex.JONGSUNG_INDEX_MAP.get(doubleChar));
      } else { // 이중 종성이 아닌 종성이 존재하는 경우 (이전글자 한글 조합완료 및 초성 위치지정)
        resultStringBuilder.append(composeSyllableFromIndex(indexInfo));
        return KrDataIndex.create().withChosung(currentChoIndex);
      }
    }
  }

  private KrDataIndex insertJungsung(final KrDataIndex indexInfo, int engPosition, StringBuilder res) {
    if (!indexInfo.jungsungIndexed()) { // 중성 입력 중
      return indexInfo.withJungsung(KeyboardIndex.JUNGSUNG_INDEX_MAP.get(KeyboardIndex.KOR_KEY.charAt(engPosition)));
    } else {
      var existJung = KeyboardIndex.JUNG_DATA.charAt(indexInfo.jungsung());
      var newJung = KeyboardIndex.KOR_KEY.charAt(engPosition);

      var doubleChar = makeDoubleChar(existJung, newJung);
      if (doubleChar != null) {
        return indexInfo.withJungsung(KeyboardIndex.JUNGSUNG_INDEX_MAP.get(doubleChar));
      } else { // 조합 안되는 모음 입력
        KrDataIndex returnIndex = indexInfo;
        if (indexInfo.chosungIndexed()) {            // 초성+중성 후 중성
          res.append(composeSyllableFromIndex(indexInfo));
          returnIndex = indexInfo.clearChosung();
        } else {                        // 중성 후 중성
          res.append(KeyboardIndex.JUNG_DATA.charAt(indexInfo.jungsung()));
        }
        returnIndex = returnIndex.clearJungsung();
        res.append(KeyboardIndex.KOR_KEY.charAt(engPosition));
        return returnIndex;
      }
    }
  }

  /**
   * 중성 입력을 위해 이전에 존재 하던 종성을 분리하여 초성으로 사용하도록 합니다.
   *
   */
  private KrDataIndex useJongsungToChosungForJungsung(final KrDataIndex indexInfo, StringBuilder res) {
    // 이중자음 다시 분해
    int tempCho;            // (임시용) 초성
    KrDataIndex returnIndex;

    String seperated = KeyboardIndex.SEPARATED_CONSONANT_MAP.get(KeyboardIndex.JONG_DATA.charAt(indexInfo.jongsung()));

    if (seperated != null) {
      returnIndex = indexInfo.withJongsung(KeyboardIndex.JONGSUNG_INDEX_MAP.get(seperated.charAt(0)));
      tempCho = KeyboardIndex.CHOSUNG_INDEX_MAP.get(seperated.charAt(1));
    } else {
      tempCho = KeyboardIndex.CHO_DATA.indexOf(KeyboardIndex.JONG_DATA.charAt(indexInfo.jongsung()));
      returnIndex = indexInfo.clearJongsung();
    }

    if (returnIndex.chosungIndexed()) { // 앞글자가 초성+중성+(종성)
      res.append(composeSyllableFromIndex(returnIndex));
    } else { // 복자음만 있던 경우
      res.append(KeyboardIndex.JONG_DATA.charAt(indexInfo.jongsung()));
    }

    returnIndex = KrDataIndex.create().withChosung(tempCho);
    return returnIndex;
  }

  private Character makeEndOfIndex(KrDataIndex indexInfo) {
    if (indexInfo.chosungIndexed() && indexInfo.jungsungIndexed()) { // 초성 + 중성으로 조합이 가능 함
      return composeSyllableFromIndex(indexInfo);
    }

    if (indexInfo.chosungIndexed()) { // 중성 없이 초성만 있음
      return KeyboardIndex.CHO_DATA.charAt(indexInfo.chosung()); // 초성만 더해 줌
    }

    if (indexInfo.jungsungIndexed()) { // 이전에 입력한 초성이 없이 중성만 있음 (한글조합 불가)
      return KeyboardIndex.JUNG_DATA.charAt(indexInfo.jungsung());
    }

    if (indexInfo.jongsungIndexed()) { // 종성만 있음
      return KeyboardIndex.JONG_DATA.charAt(indexInfo.jongsung());
    }

    return null;
  }

  /**
   * 초성 중성 종성의 위치로 한글을 조합합니다.
   *
   * @return 조합된 한글 string
   */
  private char composeSyllableFromIndex(KrDataIndex indexInfo) {
    return (char) (0xac00 + indexInfo.chosung() * 21 * 28 + indexInfo.jungsung() * 28 + indexInfo.jongsung() + 1);
  }

  /**
   * message 가 변환 가능한 char 로 이루어져 있는지 검사합니다.
   *
   * @param message 체크할 message
   * @return 사용가능한지에 대한 boolean 값입니다.
   */
  public boolean checkAvailableStr(String message) {
    if (checkUrlLink(message)) {
      return false;
    }

    if (message.startsWith("<") && message.endsWith(">")) {
      return false;
    }

    boolean isCorrectEng = false;
    for (char ch : message.toCharArray()) {
      if (KeyboardIndex.ENG_INDEX_MAP.containsKey(ch)) {
        isCorrectEng = true;
      } else if (!isSpace(ch) && !isSpecificCode(ch) && !isNumber(ch)) {
        return false;
      }
    }

    return isCorrectEng;
  }

  private boolean checkUrlLink(String message) {
    return message.contains("http://") || message.contains("https://");
  }

  private boolean isSpace(char c) {
    return c == ' ';
  }

  private boolean isSpecificCode(char c) {
    return ('!' <= c && c <= '/') || (':' <= c && c <= '@') || ('[' < c && c < '`') || ('{' < c && c < '~');
  }

  private boolean isNumber(char c) {
    return ('0' <= c && c <= '9');
  }

  private Character makeDoubleChar(char first, char second) {
    return KeyboardIndex.COMBINATION_MAP.get(String.valueOf(first) + second);
  }

}
