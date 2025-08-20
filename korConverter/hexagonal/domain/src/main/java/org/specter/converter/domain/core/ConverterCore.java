package org.specter.converter.domain.core;

public class ConverterCore {

  private final String ENG_KEY = "rRseEfaqQtTdwWczxvgkoiOjpuPhynbml"; // 키보드 영어 한글순
  private final String KOR_KEY = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ";
  private final String CHO_DATA = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"; // 초성
  private final String JUNG_DATA = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"; // 중성
  private final String JONG_DATA = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"; // 종성

  public String engToKor(String eng) {
    StringBuilder res = new StringBuilder();

    if (eng.isEmpty()) {
      return "";
    }

    int nCho = -1, nJung = -1, nJong = -1;

    for (int i = 0; i < eng.length(); i++) {
      char engCh = eng.charAt(i);
      int engPosition = ENG_KEY.indexOf(engCh);

      if (engPosition == -1) { // 영어키가 아닌경우 (특수문자 혹은 띄어쓰기)
        if (nCho != -1) { // 이전에 초성위치가 지정됨
          if (nJung != -1) { // 이전에 중성위치가 지정됨
            res.append(convertASCIIToString(nCho, nJung, nJong));
          } else { // 초성까지만 입력되고 특수기호가 입력됨
            res.append(CHO_DATA.charAt(nCho)); // 초성만 더해 줌
          }
          // 띄어쓰기
        } else { // 이전에 입력한 초성이 없음 (한글조합 불가)
          if (nJung != -1) { // 중성이 있음
            res.append(JUNG_DATA.charAt(nJung));
          } else if (nJong != -1) { // 종성이 있음
            res.append(JONG_DATA.charAt(nJong));
          }
        }
        // 특수문자 입력으로 인해 지정 초기화 및 입력된 특수문자 그대로 삽입
        nCho = -1;
        nJung = -1;
        nJong = -1;
        res.append(engCh);
      } else if (engPosition < 19) { // 자음
        final int currentKorIndex = KOR_KEY.charAt(engPosition);
        // 현재 입력된 자음(초성) 위치
        final int currentChoIndex = CHO_DATA.indexOf(currentKorIndex);

        if (nJung != -1) { // 이전에 입력한 중성이 있음
          if (nCho == -1) { // 초성이 없는경우 (한글 조합불가)
            // 이전에 입력했던 중성을 그대로 결과에 넣고 초성위치 지정
            res.append(JUNG_DATA.charAt(nJung));
            nJung = -1;
            nCho = currentChoIndex;
          } else {
            if (nJong == -1) { // 종성이 없는경우 입력된 값은 종성으로 사용
              nJong = JONG_DATA.indexOf(currentKorIndex);
              if (nJong == -1) {
                res.append(convertASCIIToString(nCho, nJung, nJong));
                nCho = currentChoIndex;
                nJung = -1;
              }
            } else if (nJong == 0 && engPosition == 9) {            // ㄳ
              nJong = 2;
            } else if (nJong == 3 && engPosition == 12) {            // ㄵ
              nJong = 4;
            } else if (nJong == 3 && engPosition == 18) {            // ㄶ
              nJong = 5;
            } else if (nJong == 7 && engPosition == 0) {            // ㄺ
              nJong = 8;
            } else if (nJong == 7 && engPosition == 6) {            // ㄻ
              nJong = 9;
            } else if (nJong == 7 && engPosition == 7) {            // ㄼ
              nJong = 10;
            } else if (nJong == 7 && engPosition == 9) {            // ㄽ
              nJong = 11;
            } else if (nJong == 7 && engPosition == 16) {            // ㄾ
              nJong = 12;
            } else if (nJong == 7 && engPosition == 17) {            // ㄿ
              nJong = 13;
            } else if (nJong == 7 && engPosition == 18) {            // ㅀ
              nJong = 14;
            } else if (nJong == 16 && engPosition == 9) {            // ㅄ
              nJong = 17;
            } else { // 이중 종성이 아닌 종성이 존재하는 경우 (이전글자 한글 조합완료 및 초성 위치지정)
              res.append(convertASCIIToString(nCho, nJung, nJong));
              nCho = currentChoIndex;
              nJung = -1;
              nJong = -1;
            }
          }
        } else { // 이전 중성입력이 없음
          if (nCho == -1) { // 기존 초성 없음 초성 지정
            if (nJong != -1) { // 기존 종성이 있음 종성처리
              res.append(JONG_DATA.charAt(nJong));
              nJong = -1;
            }
            nCho = currentChoIndex;
          } else if (nCho == 0 && engPosition == 9) {            // ㄳ
            nCho = -1;
            nJong = 2;
          } else if (nCho == 2 && engPosition == 12) {            // ㄵ
            nCho = -1;
            nJong = 4;
          } else if (nCho == 2 && engPosition == 18) {            // ㄶ
            nCho = -1;
            nJong = 5;
          } else if (nCho == 5 && engPosition == 0) {            // ㄺ
            nCho = -1;
            nJong = 8;
          } else if (nCho == 5 && engPosition == 6) {            // ㄻ
            nCho = -1;
            nJong = 9;
          } else if (nCho == 5 && engPosition == 7) {            // ㄼ
            nCho = -1;
            nJong = 10;
          } else if (nCho == 5 && engPosition == 9) {            // ㄽ
            nCho = -1;
            nJong = 11;
          } else if (nCho == 5 && engPosition == 16) {            // ㄾ
            nCho = -1;
            nJong = 12;
          } else if (nCho == 5 && engPosition == 17) {            // ㄿ
            nCho = -1;
            nJong = 13;
          } else if (nCho == 5 && engPosition == 18) {            // ㅀ
            nCho = -1;
            nJong = 14;
          } else if (nCho == 7 && engPosition == 9) {            // ㅄ
            nCho = -1;
            nJong = 17;
          } else { // 이중종성이 아닌 기존 초성이 있음 초성 처리 후 초성지정
            res.append(CHO_DATA.charAt(nCho));
            nCho = currentChoIndex;
          }
        }
      } else { // 모음
        if (nJong != -1) { //종성이 있음 앞의 종성을 초성으로 사용
          // 이중자음 다시 분해
          int tempCho;            // (임시용) 초성
          if (nJong == 2) {                    // ㄱ, ㅅ
            nJong = 0;
            tempCho = 9;
          } else if (nJong == 4) {            // ㄴ, ㅈ
            nJong = 3;
            tempCho = 12;
          } else if (nJong == 5) {            // ㄴ, ㅎ
            nJong = 3;
            tempCho = 18;
          } else if (nJong == 8) {            // ㄹ, ㄱ
            nJong = 7;
            tempCho = 0;
          } else if (nJong == 9) {            // ㄹ, ㅁ
            nJong = 7;
            tempCho = 6;
          } else if (nJong == 10) {            // ㄹ, ㅂ
            nJong = 7;
            tempCho = 7;
          } else if (nJong == 11) {            // ㄹ, ㅅ
            nJong = 7;
            tempCho = 9;
          } else if (nJong == 12) {            // ㄹ, ㅌ
            nJong = 7;
            tempCho = 16;
          } else if (nJong == 13) {            // ㄹ, ㅍ
            nJong = 7;
            tempCho = 17;
          } else if (nJong == 14) {            // ㄹ, ㅎ
            nJong = 7;
            tempCho = 18;
          } else if (nJong == 17) {            // ㅂ, ㅅ
            nJong = 16;
            tempCho = 9;
          } else { // 이중 자음이 아님
            tempCho = CHO_DATA.indexOf(JONG_DATA.charAt(nJong));
            nJong = -1;
          }

          if (nCho != -1) {    // 앞글자가 초성+중성+(종성)
            res.append(convertASCIIToString(nCho, nJung, nJong));
          } else {                    // 복자음만 있음
            res.append(JONG_DATA.charAt(nJong));
          }
          nCho = tempCho;
          nJung = -1;
          nJong = -1;
        }
        if (nJung == -1) { // 중성 입력 중
          nJung = JUNG_DATA.indexOf(KOR_KEY.charAt(engPosition));
        } else if (nJung == 8 && engPosition == 19) {            // ㅘ
          nJung = 9;
        } else if (nJung == 8 && engPosition == 20) {            // ㅙ
          nJung = 10;
        } else if (nJung == 8 && engPosition == 32) {            // ㅚ
          nJung = 11;
        } else if (nJung == 13 && engPosition == 23) {           // ㅝ
          nJung = 14;
        } else if (nJung == 13 && engPosition == 24) {           // ㅞ
          nJung = 15;
        } else if (nJung == 13 && engPosition == 32) {           // ㅟ
          nJung = 16;
        } else if (nJung == 18 && engPosition == 32) {           // ㅢ
          nJung = 19;
        } else {            // 조합 안되는 모음 입력
          if (nCho != -1) {            // 초성+중성 후 중성
            res.append(convertASCIIToString(nCho, nJung, nJong));
            nCho = -1;
          } else {                        // 중성 후 중성
            res.append(JUNG_DATA.charAt(nJung));
          }
          nJung = -1;
          res.append(KOR_KEY.charAt(engPosition));
        }
      }
    }

    // 마지막 한글이 있으면 처리
    if (nCho != -1) {
      if (nJung != -1) { // 한글 조합
        res.append(convertASCIIToString(nCho, nJung, nJong));
      } else {  // 초성만
        res.append(CHO_DATA.charAt(nCho));
      }
    } else {
      if (nJung != -1) {    // 중성만
        res.append(JUNG_DATA.charAt(nJung));
      } else if (nJong != -1) {
        res.append(JONG_DATA.charAt(nJong));
      }
    }

    return res.toString();
  }

  /**
   * 초성 중성 종성의 위치로 한글을 조합합니다.
   *
   * @param nCho  초성의 index
   * @param nJung 중성의 index
   * @param nJong 종성의 index
   * @return 조합된 한글 string
   */
  private String convertASCIIToString(int nCho, int nJung, int nJong) {
    char c = (char) (0xac00 + nCho * 21 * 28 + nJung * 28 + nJong + 1);
    return String.valueOf(c);
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
      int engPosition = ENG_KEY.indexOf(ch);
      boolean isSpace = ch == (char) 32;

      if (engPosition != -1) {
        isCorrectEng = true;
      } else if (!isSpace & !isSpecificCode(ch)) {
        return false;
      }
    }

    return isCorrectEng;
  }

  public static boolean checkUrlLink(String message) {
    return message.contains("http://") || message.contains("https://");
  }

  private static boolean isSpecificCode(char c) {
    return ((int) c > 32 && (int) c < 65) ||
        ((int) c > 90 && (int) c < 97) ||
        ((int) c > 122 && (int) c < 127);
  }
}
