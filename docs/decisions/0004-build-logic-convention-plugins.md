# ADR-0004: build-recipe-plugin → build-logic convention plugin 전환

## Status
Accepted

## Context
이 프로젝트는 사내 linecorp build-recipe-plugin으로 모듈 공통 설정(java/spring/test/boot)을
구성해 왔다. 각 모듈은 `korConverter/**/gradle.properties`에 `label=java,spring,boot` 같은
**문자열**을 적어 참여를 선언하고, 실제 설정은 루트 `build.gradle.kts`의
`configureByLabel("java"|"spring"|"test"|"boot")` 블록 4개에 몰려 있었다.

이 간접참조 방식의 비용은 편의가 아니라 **가시성**이었다. `label=` 문자열에서 실제
`configureByLabel` 블록으로 IDE가 정의를 이동시켜 주지 않고, 자동완성도 타입 검사도
작동하지 않는다. 한 모듈에 어떤 설정이 적용되는지 파악하려면 그 모듈의
`gradle.properties`, 루트 `build.gradle.kts`의 라벨 블록, `libs.versions.toml`
세 파일을 오가야 했다.

전환 검토 중 별도 문제를 하나 더 확인했다. Gradle은 precompiled script plugin
**소스** 안에서 `libs.*` 타입세이프 접근자를 생성하지 않는다(gradle/gradle#15383,
2020년 개설 이후 여전히 OPEN, 마일스톤 없음). 네이티브 대안은 `VersionCatalogsExtension`을
통한 문자열 lookup뿐이며, 이 경로는 오타가 컴파일이 아니라 구성 시점에야 드러나고,
플러그인 버전이 카탈로그와 build-logic 양쪽에 이중 관리되어 Dependabot이 카탈로그만
갱신할 때 두 값이 소리 없이 어긋나는 위험을 남긴다.

전환 계약은 **무행위변경**이다. 검증 기준은 "빌드가 통과한다"가 아니라 전환 전후
의존성 보고서의 텍스트 diff 공백이며, 6개 모듈 × 7개 축(`compileClasspath`,
`runtimeClasspath`, `testCompileClasspath`, `testRuntimeClasspath`,
`annotationProcessor`, `testAnnotationProcessor`, `errorprone`) = 42건 비교에서
**diff 0건**으로 증명했다.

## Decision
`build-logic` composite build를 도입한다.

- 루트 `settings.gradle.kts`: `pluginManagement { includeBuild("build-logic") }`
- `build-logic`은 **multi-project 구조**를 취한다 — `build-logic/settings.gradle.kts`가
  `include("conventions")`로 서브프로젝트를 한 겹 판다.
- `build-logic/conventions`는 `dev.panuszewski.typesafe-conventions` 0.11.1을
  적용해 precompiled script plugin 소스 안에서도 `libs.*` 타입세이프 접근자를 쓴다.
- 설정 묶음 4개 — `java-conventions`, `spring-conventions`, `test-conventions`,
  `boot-conventions` — 를 현행 라벨 4종과 **1:1**로 대응시킨다. 이관의 추적
  가능성을 우선했고, 소비자가 하나뿐인 묶음의 통합은 이번 범위에서 하지 않는다.
- 각 모듈은 라벨 문자열 대신 `plugins { id("java-conventions") ... }`로 필요한
  설정 묶음을 **명시 선언**한다.
- 루트 `build.gradle.kts`에는 `alias(libs.plugins.spotless) apply false`
  (Spotless의 공유 `BuildService`가 클래스로더 하나를 공유하게 하기 위함)와
  `group`/`version`/`repositories`만 남긴다.
- **단일 프로젝트 `build-logic`**(파일 수가 적고 Isolated Projects와 호환됨)을
  대안으로 검토했으나 기각했다. 근거는 성능이나 정합성이 아니라 **참조 프로젝트
  (unity)와의 구조 일치** — 두 저장소를 오가는 유지보수자의 인지 부하를 낮추는
  쪽을 택했다.
- **수용한 제약 1** — Isolated Projects 사용 불가: typesafe-conventions #186에
  "Not compatible with isolated-projects enabled in Gradle 9.7 and above"로
  보고된 문제이며, 이 저장소의 multi-project `build-logic` 구성에서도 재현된다.
  `org.gradle.isolated-projects=true`를 켜지 않는다.
- **수용한 제약 2** — `build-logic` 단독 구동 금지: `gradle -p build-logic <task>`는
  `:compilePluginsBlocks`에서 실패한다. typesafe-conventions는 최상위 빌드를 통한
  구동을 전제하므로, CI·스크립트·문서 어디에도 이 형태의 호출을 넣지 않는다.
- **불변식** — toolchain 25 고정: `build-logic/conventions/build.gradle.kts`의
  Java toolchain을 25로 명시 고정한다. Gradle 9.7.1의 `kotlin-dsl`은 `jvmTarget`을
  데몬 JVM에 무경고로 맞추므로, 고정하지 않으면 더 낮은 JVM으로 빌드할 때 원인을
  알 수 없는 `UnsupportedClassVersionError`가 난다. 팀에 JDK 25 미만으로 빌드하는
  환경이 생기면, 이 값을 그 하한으로 내린다.

## Consequences
- Pro: 설정의 소재가 `label=` 문자열이 아니라 모듈의 `plugins {}` 블록에 명시되어,
  IDE 정의 이동·자동완성·타입 검사가 전부 복원된다.
- Pro: typesafe-conventions 도입으로 플러그인 버전의 이중 관리 위험이 사라진다 —
  `libs.versions.toml`이 유일한 소스가 된다.
- Pro: Spotless가 모듈별 설정으로 분리되어 `:domain:spotlessApply` 같은 부분
  실행과 증분 처리가 가능해진다(전환의 목적은 아니었으나 얻은 부수 효과).
- Pro: 전환 계약(무행위변경)을 6모듈×7축 42건 diff 0건으로 기계적으로 증명했다 —
  "정리됐다"는 주장이 아니라 검증된 사실이다.
- Con: typesafe-conventions는 개인 메인테이너가 유지하는 0.x 플러그인이다.
  선언된 호환성 하한은 `Gradle 8.8+, JDK 17+`뿐이고, Gradle 9 지원을 명시한
  벤더 문서는 없다(실측으로 9.7.1 동작을 확인했을 뿐이다).
- Con: Isolated Projects를 이 구조에서는 쓸 수 없다. 도입하려면 `build-logic`을
  단일 프로젝트로 재구성해야 한다.
- Con: 새 모듈을 추가하는 개발자는 필요한 설정 묶음을 `plugins {}`에 **직접
  선언**해야 한다. 라벨 시절처럼 파일 하나만 만들면 자동 적용되는 방식이 아니다.
- 후속 과제: configuration cache 도입을 검토할 때 `build-logic`을 단일
  프로젝트로 바꿀지(Isolated Projects 확보) 다시 평가한다. 또한 이번 전환에서
  그대로 이관한 알려진 하자 — `JacocoCoverageVerification` 80% 규칙이 `check`에
  연결되지 않아 실효가 없는 문제, NullAway `AnnotatedPackages`가 `"com.uber"`로
  고정되어 `org.specter.converter`를 검사하지 않는 문제, `test-conventions`와
  `:domain/build.gradle.kts`의 junit/assertj 중복 선언 — 은 무행위변경 계약을
  지키기 위해 손대지 않았다. 정리는 후속 이슈로 분리한다
  (`specs/001-build-logic-migration/data-model.md` J-6 / J-8 / INV-6).
