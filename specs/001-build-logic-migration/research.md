# Phase 0: Research — build-logic 전환

**Date**: 2026-09-08 | **Plan**: [plan.md](./plan.md)

이 문서의 R-1 ~ R-7은 **추측이 아니라 실측**이다. 스펙 작성 전 단계에서 이 저장소의 실제 도구 조합(Gradle 9.7.1 / Temurin JDK 25.0.3 / Kotlin 2.4.0 embedded)으로 최소 스파이크를 세워 확인했다. "확인 방법" 항목에 재현 경로를 남긴다.

Technical Context에 남은 NEEDS CLARIFICATION은 **0건**이다.

---

## R-1. precompiled script plugin에서 버전 카탈로그를 참조하는 방법

**Decision**: `dev.panuszewski.typesafe-conventions` 0.11.1을 채택한다.

**Rationale**:
- Gradle은 precompiled script plugin **소스** 안에서 `libs.*` 접근자를 생성하지 않는다. [gradle/gradle#15383](https://github.com/gradle/gradle/issues/15383)은 2020-12-01 개설 이후 **여전히 OPEN, 마일스톤 없음**이며, 스레드에서 "이런 것이 Gradle에 들어올 가능성은 낮다"는 방향으로 정리되고 typesafe-conventions를 대안으로 지목한다.
- 네이티브 대안은 `build-logic/settings.gradle.kts`에서 카탈로그를 수동 등록한 뒤 **문자열 lookup**(`extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("...").get()`)을 쓰는 것뿐이다. 오타가 컴파일에서 잡히지 않고 런타임(구성 시점)에야 드러난다.
- (b) 미채택 시의 실제 비용은 편의성이 아니라 **정합성**이다: 플러그인 버전이 `libs.versions.toml`과 `build-logic` 양쪽에 이중 관리되고, Dependabot은 카탈로그만 갱신하므로 두 값이 **소리 없이 어긋난다**. FR-005 위반.

**Alternatives considered**:
| 대안 | 기각 사유 |
|---|---|
| Gradle 네이티브 (`VersionCatalogsExtension` 문자열 lookup) | 타입 안전성 0. 버전 이중 관리로 Dependabot과 정합성 붕괴 |
| plugin marker 좌표 직접 기술 (`<id>:<id>.gradle.plugin:<ver>`) | 동일한 이중 관리 문제. 단, R-2에 따라 `build-logic/conventions/build.gradle.kts` **자체**에서는 타입세이프 `libs.*`가 typesafe-conventions 없이도 동작하므로 부분적 대안은 되나, precompiled script 소스는 여전히 문자열 lookup |
| `buildSrc` | Gradle 문서가 명시하듯 카탈로그를 자동 상속하지 않아 동일 문제. 게다가 빌드 캐시 무효화 범위가 넓다 |

**Risk**: 개인 메인테이너의 0.x 플러그인이다. 선언된 호환성은 하한만 있고(`Gradle 8.8+, JDK 17+`) **Gradle 9 지원을 명시한 벤더 문서는 없다**. 다만 R-2로 실동작을 확인했고, 해당 저장소 자신의 wrapper가 9.7.1을 추적 중이며 Gradle 테스트 매트릭스를 운용한다.

---

## R-2. Gradle 9.7.1 + JDK 25에서 typesafe-conventions / kotlin-dsl 동작 여부

**Decision**: 동작한다. 채택 가능.

**확인 방법**: 스크래치패드에 `build-logic/conventions` multi-project 스파이크를 세우고 `java-conventions.gradle.kts`에서 `alias(libs.plugins.*)`, `libs.versions.palantir.java.format.get()`, `libs.projectlombok.lombok`을 모두 사용한 뒤 `:sample:compileJava` 실행 → **BUILD SUCCESSFUL (45s)**. 생성 태스크 `generateLibrariesForLibs`, `generateEntrypointForLibs`, `generatePrecompiledScriptPluginAccessors` 정상 수행 확인.

**함께 확인된 것**: 다음 구성이 전부 convention plugin 안에서 Gradle 9.7.1에 동작한다 — `java.toolchain.languageVersion = 25`, `checkstyle { toolVersion }`, `jacoco` + `tasks.withType<JacocoCoverageVerification>`, `options.errorprone { option(...) }`, `spotless { java { palantirJavaFormat(...) } }`, `jib { }`, `springBoot { buildInfo() }`, `tasks.withType<BootJar> { enabled = ... }`.

---

## R-3. precompiled script plugin의 타입 접근자 생성 범위 — **실제로 밟은 함정**

**Decision**: 확장 블록(`springBoot {}`, `jib {}` 등)에 접근하려면, 그 블록을 제공하는 플러그인을 **해당 스크립트 자신의 `plugins {}` 블록**에 선언해야 한다. 다른 설정 묶음이 같은 플러그인을 적용해도 접근자는 생성되지 않는다.

**Rationale / 확인 방법**: `spring-conventions`가 `alias(libs.plugins.springframework.boot)`을 적용한 상태에서 `boot-conventions`에 `springBoot { buildInfo() }`만 쓰자 컴파일 실패:

```
e: boot-conventions.gradle.kts:7:1  Unresolved reference 'springBoot'.
e: boot-conventions.gradle.kts:7:14 Unresolved reference 'buildInfo'.
```

`boot-conventions`의 `plugins {}`에 `alias(libs.plugins.springframework.boot)`을 **중복 선언**하자 통과. 플러그인 적용은 멱등이므로 런타임 중복 적용은 no-op이다.

**설계 반영**: `boot-conventions`는 Spring Boot 플러그인을 재선언한다. 이 중복은 실수가 아니므로 **주석으로 이유를 남긴다** — 정리 대상으로 오인해 지우면 빌드가 깨진다.

---

## R-4. `bootJar` 활성/비활성 분리 패턴

**Decision**: `spring-conventions`에서 `tasks.withType<BootJar>().configureEach { enabled = false }`, `boot-conventions`에서 `enabled = true`.

**Rationale**: 현행 루트 스크립트가 `allprojects { BootJar { enabled = false } }` + `configureByLabel("boot") { BootJar { enabled = true } }`로 하던 것과 동일한 의미다. `spring-conventions`가 Spring Boot 플러그인을 적용하므로 그 4개 모듈에 `bootJar` 태스크가 생기고, 그중 `:boot`만 되켠다.

**중요**: `spring-conventions`는 Spring Boot 플러그인을 **반드시** 적용해야 한다. `libs.versions.toml`의 `springframework-boot-starter-*` 항목들이 **버전 없이 선언**되어 있고, 그 버전은 Spring Boot 플러그인이 공급하는 BOM에서 온다. 플러그인을 빼면 버전 해석이 실패한다.

**확인 방법**: 스파이크에서 `:svc`(spring만) / `:app`(spring+boot) 두 모듈로 검증 → `:svc:bootJar **SKIPPED**`, `:svc/build/libs` 부재, `:app:bootJar` 실행 + `app-2.2.1.jar` 생성 + `:app:bootBuildInfo` 수행 확인.

---

## R-5. `kotlin-dsl` jvmTarget이 데몬 JVM을 추종하는 문제

**Decision**: `build-logic/conventions`에 Java toolchain 25를 명시 고정한다.

**Rationale**: Gradle 9.7.1의 `kotlin-dsl`은 `jvmTarget`을 **데몬 JVM에 맞춰 무경고로 설정**한다. JDK 25 데몬에서 컴파일하면 `jvmTarget = JVM_25`, 바이트코드 major **69**가 산출된다. 그렇게 만들어진 설정 묶음은 더 낮은 JVM에서 `UnsupportedClassVersionError`로 죽으며, 그 메시지는 원인을 설명하지 않는다. 빌드 캐시를 공유하는 이기종 환경에서 특히 위험하다.

**고정 값 선택**: **25**. 이 저장소는 toolchain 25 / CI `setup-java 25` / 배포 이미지 `amazoncorretto:25`로 전부 25다. 25로 고정하면 현재 JVM이 그대로 툴체인이 되어 **추가 다운로드 없이** 결정론만 얻는다. 21로 내리면 이기종 데몬까지 커버되지만 foojay toolchain resolver를 새로 도입해야 한다 — 존재하지 않는 이질성을 위한 기계장치다.

**불변식(ADR-0004에 기록)**: 팀에 JDK 25 미만으로 빌드하는 환경이 생기면 이 값을 그 하한으로 내린다.

**Alternatives considered**: 고정하지 않고 데몬 JVM에 위임 — 조용한 실패 모드를 남기므로 기각. FR-014.

---

## R-6. multi-project `build-logic`과 Isolated Projects 비호환

**Decision**: multi-project 구조를 채택하고, **Isolated Projects 사용 불가**를 수용된 제약으로 기록한다.

**Rationale**: [typesafe-conventions #186](https://github.com/radoslaw-panuszewski/typesafe-conventions-gradle-plugin/issues/186) (OPEN, 2026-08-27) — *"Not compatible with isolated-projects enabled in Gradle 9.7 and above"*. 실패 형태:

```
Project ':build-logic' cannot access 'Project.plugins' functionality
on subprojects via 'allprojects'
```

**확인 방법**: 재현 결과 **multi-project `build-logic`(`include("conventions")`) + `org.gradle.isolated-projects=true` → FAILED**, **단일 프로젝트 `build-logic` + isolated-projects → SUCCESSFUL**. 이슈 스레드 자체에는 코멘트가 0개이고 이 구분을 명시하지 않으므로, 구분은 재현 실험에서 나온 것이다.

**Alternatives considered**: 단일 프로젝트 `build-logic` — 이 제약을 회피하고 파일도 적다. 그러나 참조 프로젝트(unity)와의 구조 일치를 우선해 기각했다(plan.md Complexity Tracking).

**Follow-up**: 빌드 구성 캐시/Isolated Projects 도입을 검토할 때 이 구조를 재평가한다.

---

## R-7. `build-logic` 단독 구동 금지

**Decision**: `build-logic`은 최상위 빌드를 통해서만 구동한다. CI·스크립트·문서에 `gradle -p build-logic <task>` 형태의 호출을 넣지 않는다.

**Rationale / 확인 방법**: typesafe-conventions는 최상위 빌드에서 구동되는 것을 전제한다. 단독 구동 시 `:compilePluginsBlocks`에서 실패하며 플러그인이 README의 `#top-level-build` 절을 가리킨다.

**설계 반영**: 현재 CI(`pr-ci.yml`, `tag-cd.yml`)와 `lefthook.yml`에 해당 호출이 없다 — 확인함. 이 전환에서도 추가하지 않는다.

---

## R-8. Gradle 9.x 제거 API 중 이 전환에 걸리는 것

**Decision**: 없음. 현행 루트 스크립트를 그대로 옮겨도 안전하다.

**Rationale**: Gradle 9에서 제거된 것 중 convention plugin 이관에 걸릴 만한 것은 (a) Convention API 전면 제거(`project.archivesBaseName`, `JavaPluginConvention` 등)와 (b) `Project#exec` / `Project#javaexec` 제거다. **현행 루트 `build.gradle.kts`에 두 부류의 사용처가 모두 없다 — 확인함.** `allprojects {}`, `subprojects {}`, `afterEvaluate {}`, `project.extensions` 접근, `layout.buildDirectory`는 Gradle 9에서 변경되지 않았다(Isolated Projects 활성화 시를 제외하고, R-6에 따라 켜지 않는다).

**주의(전환과 무관)**: Gradle 9는 "실행되었으나 테스트를 하나도 발견하지 못한" `Test` 태스크를 실패로 처리한다. 이 저장소는 **이미 9.7.1에서 빌드가 통과**하고 있으므로 이 변경은 전환이 도입하는 위험이 아니다. 테스트 소스가 없는 모듈의 `test` 태스크는 `NO-SOURCE`로 스킵되며 실패하지 않는다.

---

## R-9. spotless 배치 — 루트 전역에서 모듈별로

**Decision**: 루트에 `alias(libs.plugins.spotless) apply false`로 한 번 올리고, 실제 설정은 `java-conventions`에 둔다.

**Rationale**:
- 루트 `apply false`가 필요한 이유는 취향이 아니다. Spotless는 `SpotlessTaskService`라는 공유 BuildService를 쓰는데, 루트에 올리지 않고 서브프로젝트에만 적용하면 프로젝트마다 별도 클래스로더가 같은 클래스를 각자 로드해 `loaded with InstrumentingVisitableURLClassLoader(...) using a provider of type ...` 로 실패한다. 루트에 apply false로 한 번 올려 두면 공통 부모 스코프의 클래스를 공유한다. (참조 프로젝트 unity가 동일한 이유로 동일 구조를 씀 — 해당 저장소 루트 `build.gradle.kts` 주석에 기록되어 있다.)
- 모듈별 분할로 `:domain:spotlessApply` 같은 부분 실행과 증분/병렬이 가능해진다.

**이관 시 반드시 유지할 것 — `targetExclude`**: 현행 루트 설정은 `target("**/*.java")` + `targetExclude("**/build/**", "**/generated/**")`다. 모듈별 spotless의 기본 대상은 소스셋의 java이며, **`:adapter-persistence`는 jOOQ 생성 디렉터리를 `main.java.srcDir(...)`로 소스셋에 추가**한다. 따라서 `targetExclude`를 옮기지 않으면 spotless가 생성 코드를 포맷 대상으로 삼아 검사 결과가 달라진다. FR-012의 "검사 대상 소스 집합 동일" 요구가 여기에 걸린다.

**검증 지점**: quickstart.md V-4에서 `spotlessCheck` 대상 파일 수를 전후 비교한다.

---

## R-10. 의존성 동등성 검증의 축 — 스펙보다 강화

**Decision**: 비교 축을 **7개**로 늘린다: `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`, `testRuntimeClasspath`, `annotationProcessor`, `testAnnotationProcessor`, `errorprone`.

**Rationale**: 최초 스펙 초안의 SC-001은 compile/runtime/testRuntime 3축만 요구했다. 그런데 이 전환에서 이관되는 의존성 중 `errorprone(error_prone_core)`, `errorprone(nullaway)`, `annotationProcessor(lombok)`, `testAnnotationProcessor(lombok)`은 **그 3축 어디에도 나타나지 않는 별도 configuration**에 붙는다. 3축만 비교하면 `errorprone(nullaway)` 한 줄을 누락해도 diff가 깨끗이 통과하고 정적 분석만 조용히 죽는다.

이 발견에 따라 **스펙 SC-001·FR-006과 US2 수용 시나리오를 7축/42건으로 개정했다**(`/speckit-analyze` I1). 하위 문서에서 스펙을 재해석하는 대신 스펙 자체를 고쳤다 — 재해석으로 남기면 스펙만 읽는 구현자가 3축으로 검증한다.

**Alternatives considered**: 3축 유지 — 위 실패 모드를 잡지 못하므로 기각.
