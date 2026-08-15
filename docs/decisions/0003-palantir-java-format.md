# ADR-0003: palantir-java-format 채택

## Status
Accepted

## Context
이 프로젝트는 Spotless 를 통해 google-java-format 1.35.0 을 java 포맷터로 사용해 왔다
(2-space 들여쓰기, 100 column). 포맷터 버전은 `build.gradle.kts` 에 문자열로 하드코딩되어
있어 나머지 모든 의존성과 달리 `gradle/libs.versions.toml` 및 Dependabot 추적 밖에 있었다.

전환 동기는 **조직 내 다른 프로젝트 및 사내 Java 코드 스타일 표준이
palantir-java-format 이며, 이 저장소만 google-java-format 으로 남아 있었다는 점**이다.
저장소를 오가는 사람이 들여쓰기 폭이 다른 두 스타일 사이를 전환해야 했고,
같은 코드를 다른 저장소로 옮길 때 무의미한 재포맷 diff 가 발생했다.

전환에 앞서 실제 적용 후 되돌리는 방식으로 다음을 사전 검증했다:

- JDK 25 / Gradle 9.6.1 / Spotless 8.8.0 에서 `--add-exports` JVM 인자 없이 동작한다.
  (google-java-format 계열이 JDK 16+ 에서 흔히 요구하는 `gradle.properties` 설정이 불필요)
- CI Gate 1 (`spotlessCheck checkstyleMain compileJava`) 통과.
- unused import 자동 제거가 유지된다. Spotless 의 palantir 스텝도 import fixer 를
  실행하므로 google-java-format 대비 기능 회귀가 없다.
- 재포맷 범위는 66개 중 62개 파일, +1977/-2099.
- jOOQ generated 코드는 기존 `targetExclude("**/generated/**")` 로 이미 제외된다.

부수 효과로, palantir 의 wrapping 규칙이 이 코드베이스에 깔린 fluent chain
(ArchUnit, jOOQ DSL, AssertJ)에서 더 짧은 결과를 낸다. google-java-format 은
`= noClasses()` 를 다음 줄로 밀어내지만 palantir 는 첫 호출을 `=` 뒤에 붙이고
체인만 들여쓴다. 전환 결정의 근거는 아니지만 순변화가 -122 줄인 이유다.

## Decision
Spotless 의 java 포맷터를 **palantir-java-format 2.97.0** 으로 교체한다.

- `build.gradle.kts`: `palantirJavaFormat(libs.versions.palantir.java.format.get())`
- 스타일 옵션은 기본값을 쓴다 — `style = PALANTIR`, `formatJavadoc = false`.
  Javadoc 정규화는 이번 전환 목적과 무관하므로 켜지 않는다.
- 버전은 `libs.versions.toml` 의 `[versions]` 로 옮기고, Dependabot 이 bump 대상
  artifact 를 인식하도록 `[libraries]` 항목도 함께 둔다. 이 라이브러리는 어느 모듈에도
  의존성으로 추가되지 않는다. Dependabot `etc` 그룹의 `"*"` 패턴이 자동 포함한다.
- Checkstyle 에는 `Indentation` / `LineLength` 모듈을 **추가하지 않는다.** 포맷터가
  결정론적으로 보장하는 것을 Checkstyle 로 중복 검사하면, 어느 한쪽 버전이 올라갈 때
  두 도구가 어긋나 CI 가 교착된다. Checkstyle 은 포맷터가 다루지 않는 영역
  (naming, imports, braces, coding 관례)만 담당한다.
- `.editorconfig` 를 추가해 IDE 타이핑을 포맷터 결과에 근접시킨다. 최종 판정은 항상
  `./gradlew spotlessApply` 이며 lefthook pre-commit 이 `spotlessCheck` 로 강제한다.
- 재포맷 커밋은 `.git-blame-ignore-revs` 에 등록한다.

## Consequences
- Pro: 조직 내 다른 저장소와 스타일이 일치한다. 저장소 간 코드 이동 시 재포맷 diff 없음.
- Pro: 포맷터 버전이 Dependabot 추적 대상이 되어, 그동안 수동이던 업그레이드가 자동화된다.
- Pro: fluent chain 이 많은 이 코드베이스에서 줄 수가 소폭 감소(-122).
- Con: 62개 파일 재포맷으로 `git blame` 이 오염된다. `.git-blame-ignore-revs` 로 완화하되,
  개발자가 `git config blame.ignoreRevsFile .git-blame-ignore-revs` 를 로컬에서 1회
  실행해야 한다 (README 에 기재). GitHub 웹 blame 은 이 파일을 자동 인식한다.
- Con: 이 전환 PR 은 **squash 가 아닌 merge commit 으로 머지해야 한다.** squash 하면
  재포맷 커밋 SHA 가 사라져 `.git-blame-ignore-revs` 항목이 조용히 무효화된다.
  이 저장소의 기본 squash 관례에 대한 1회 예외다.
- Con: 전환 시점에 열려 있던 브랜치/PR 은 전면 충돌한다. 전환 시점에 열린 PR 이 없음을
  확인하고 진행했다. 이후 유사 전환 시 동일 확인이 필요하다.
- Con: IDE 에서 자동 포맷(Cmd+Opt+L)을 쓰려면 개발자가 Palantir Java Format
  IntelliJ 플러그인을 설치해야 한다. 미설치 시 IDE 포맷과 Spotless 결과가 어긋나
  커밋 훅에서 되돌려진다.
