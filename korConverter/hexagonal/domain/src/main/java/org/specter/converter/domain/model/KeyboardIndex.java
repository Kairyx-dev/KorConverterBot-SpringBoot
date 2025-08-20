package org.specter.converter.domain.model;

import java.util.HashMap;
import java.util.Map;

public class KeyboardIndex {

  private KeyboardIndex() {
  }

  private static final String ENG_KEY = "rRseEfaqQtTdwWczxvgkoiOjpuPhynbml"; // 키보드 영어 한글순
  public static final String KOR_KEY = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ";
  public static final String CHO_DATA = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"; // 초성
  public static final String JUNG_DATA = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"; // 중성
  public static final String JONG_DATA = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"; // 종성

  public static final Map<Character, Integer> ENG_INDEX_MAP;
  public static final Map<Character, Integer> KOR_INDEX_MAP;
  public static final Map<Character, Integer> CHOSUNG_INDEX_MAP;
  public static final Map<Character, Integer> JUNGSUNG_INDEX_MAP;
  public static final Map<Character, Integer> JONGSUNG_INDEX_MAP;
  public static final Map<String, Character> COMBINATION_MAP;
  public static final Map<Character, String> SEPARATED_CONSONANT_MAP;

  static {
    ENG_INDEX_MAP = Map.copyOf(makeIndexMap(ENG_KEY));
    KOR_INDEX_MAP = Map.copyOf(makeIndexMap(KOR_KEY));
    CHOSUNG_INDEX_MAP = Map.copyOf(makeIndexMap(CHO_DATA));
    JUNGSUNG_INDEX_MAP = Map.copyOf(makeIndexMap(JUNG_DATA));
    JONGSUNG_INDEX_MAP = Map.copyOf(makeIndexMap(JONG_DATA));
    COMBINATION_MAP = Map.copyOf(createDoubleCharMap());
    SEPARATED_CONSONANT_MAP = Map.copyOf(createSepratedConsonantMap());
  }

  private static Map<Character, Integer> makeIndexMap(String original) {
    Map<Character, Integer> indexMap = new HashMap<>();

    for (int i = 0; i < original.length(); i++) {
      indexMap.put(original.charAt(i), i);
    }

    return indexMap;
  }

  private static Map<String, Character> createDoubleCharMap() {
    Map<String, Character> map = new HashMap<>();
    map.put("ㄱㅅ", 'ㄳ');
    map.put("ㄴㅈ", 'ㄵ');
    map.put("ㄴㅎ", 'ㄶ');
    map.put("ㄹㄱ", 'ㄺ');
    map.put("ㄹㅁ", 'ㄻ');
    map.put("ㄹㅂ", 'ㄼ');
    map.put("ㄹㅅ", 'ㄽ');
    map.put("ㄹㅌ", 'ㄾ');
    map.put("ㄹㅍ", 'ㄿ');
    map.put("ㄹㅎ", 'ㅀ');
    map.put("ㅂㅅ", 'ㅄ');
    map.put("ㅗㅏ", 'ㅘ');
    map.put("ㅗㅐ", 'ㅙ');
    map.put("ㅗㅣ", 'ㅚ');
    map.put("ㅜㅓ", 'ㅝ');
    map.put("ㅜㅔ", 'ㅞ');
    map.put("ㅜㅣ", 'ㅟ');
    map.put("ㅡㅣ", 'ㅢ');
    return map;
  }

  private static Map<Character, String> createSepratedConsonantMap() {
    Map<Character, String> map = new HashMap<>();
    map.put('ㄳ', "ㄱㅅ");
    map.put('ㄵ', "ㄴㅈ");
    map.put('ㄶ', "ㄴㅎ");
    map.put('ㄺ', "ㄹㄱ");
    map.put('ㄻ', "ㄹㅁ");
    map.put('ㄼ', "ㄹㅂ");
    map.put('ㄽ', "ㄹㅅ");
    map.put('ㄾ', "ㄹㅌ");
    map.put('ㄿ', "ㄹㅍ");
    map.put('ㅀ', "ㄹㅎ");
    map.put('ㅄ', "ㅂㅅ");
    return map;
  }
}
