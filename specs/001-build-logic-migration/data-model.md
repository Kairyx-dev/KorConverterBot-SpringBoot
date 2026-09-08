# Phase 1: Data Model — 설정 묶음 ↔ 모듈 매핑

**Date**: 2026-09-08 | **Plan**: [plan.md](./plan.md)

이 기능의 "데이터"는 런타임 엔티티가 아니라 **빌드 설정의 배치**다. 이 문서는 이관의 단일 출처(source of truth)이며, 여기 적힌 것 외의 설정 이동은 무행위변경 계약 위반이다.

---

## E1. 라벨 → 설정 묶음 매핑

| 현행 라벨 | 신규 설정 묶음 | 적용 모듈 | 모듈 수 |
|---|---|---|---|
| `java` | `java-conventions` | boot, configuration, application, domain, adapter-persistence, adapter-bot | 6 |
| `spring` | `spring-conventions` | boot, configuration, adapter-persistence, adapter-bot | 4 |
| `test` | `test-conventions` | domain | 1 |
| `boot` | `boot-conventions` | boot | 1 |

**불변식 INV-1**: 이 표는 현행 `korConverter/**/gradle.properties`의 `label=` 값과 완전히 일치한다. 전환 후 각 모듈이 선언하는 설정 묶음 집합은 이 표의 행과 정확히 대응해야 한다.

### 모듈별 선언 (전환 후)

| 모듈 | 경로 | 선언할 설정 묶음 | 모듈 고유 설정(유지) |
|---|---|---|---|
| `:boot` | `korConverter/boot` | java, spring, boot | — |
| `:configuration` | `korConverter/configuration` | java, spring | — |
| `:application` | `korConverter/hexagonal/application` | java | — |
| `:domain` | `korConverter/hexagonal/domain` | java, test | `alias(libs.plugins.pitest)` + `pitest {}` 블록 |
| `:adapter-persistence` | `korConverter/hexagonal/adapter/adapter-persistence` | java, spring | `alias(libs.plugins.jooq.codegen)` + `jooq {}` + `sourceSets` + `compileJava dependsOn jooqCodegen` |
| `:adapter-bot` | `korConverter/hexagonal/adapter/adapter-bot` | java, spring | — |

**불변식 INV-2**: 모듈 고유 설정(pitest, jOOQ)은 라벨 체계 밖에 있었으므로 **한 글자도 변경하지 않는다**. 모듈 스크립트의 기존 `dependencies {}` 블록도 마찬가지다.

---

## E2. `java-conventions` — 이관 인벤토리

현행 `build.gradle.kts`의 `configureByLabel("java") { ... }` 전체를 옮긴다.

| # | 항목 | 현행 | 이관 후 |
|---|---|---|---|
| J-1 | 플러그인 적용 | `apply(plugin = "idea"/"java"/"net.ltgt.errorprone"/"jacoco"/"checkstyle")` | `plugins {}` 블록에 `java`, `idea`, `jacoco`, `checkstyle`, `alias(libs.plugins.net.ltgt.errorprone)`, `alias(libs.plugins.spotless)` |
| J-2 | toolchain | `java.toolchain.languageVersion = JavaLanguageVersion.of(25)` | 동일 |
| J-3 | Lombok | `implementation` + `annotationProcessor` + `testImplementation` + `testAnnotationProcessor` | 동일 4개 전부 |
| J-4 | 정적 분석 의존 | `errorprone(error_prone_core)`, `errorprone(nullaway)` | 동일 |
| J-5 | 테스트 실행기 | `tasks.withType<Test> { useJUnitPlatform() }` | `configureEach` 사용 |
| J-6 | NullAway 옵션 | `options.errorprone { option("NullAway:AnnotatedPackages", "com.uber") }` | **문자열 그대로 유지** (하자이나 수정 금지 — 후속 이슈 #2) |
| J-7 | JaCoCo 리포트 순서 | `tasks.withType<JacocoReport> { dependsOn(tasks.named("test")) }` | 동일 |
| J-8 | JaCoCo 검증 규칙 | `JacocoCoverageVerification` minimum 0.80 | **그대로 유지** (`check`에 미연결 상태 유지 — 후속 이슈 #1) |
| J-9 | Checkstyle | `toolVersion` = 카탈로그, `configFile` = `config/checkstyle/checkstyle.xml`, `isIgnoreFailures = false` | 동일. `configFile`은 `rootProject.file(...)`로 해석 |
| J-10 | Checkstyle 제외 | `tasks.withType<Checkstyle> { exclude("**/generated/**") }` | 동일 |
| J-11 | Spotless | 루트 전역 `target("**/*.java")` + `targetExclude("**/build/**", "**/generated/**")` + palantir | **모듈별로 이동.** `targetExclude` 필수 유지 (research.md R-9) |
| J-12 | repositories | 라벨 블록 안의 `repositories { mavenCentral() }` | **제거** — 루트 `allprojects`로 통합 (FR-011) |

**불변식 INV-3**: J-6, J-8은 알려진 하자다. **수정하면 무행위변경 계약 위반이다.** 주석으로 후속 이슈 번호를 남긴다.

---

## E3. `spring-conventions` — 이관 인벤토리

| # | 항목 | 현행 | 이관 후 |
|---|---|---|---|
| S-1 | 플러그인 적용 | `apply(plugin = "org.springframework.boot")`, `apply(plugin = "io.spring.dependency-management")` | `plugins {}`에 `java`, `alias(libs.plugins.springframework.boot)`, `alias(libs.plugins.spring.dependency.management)` |
| S-2 | 런타임 의존 6종 | `starter`, `starter-web`, `starter-opentelemetry`, `starter-validation`, `starter-actuator`, `starter-json` | 동일 6종 전부 |
| S-3 | 테스트 의존 | `testImplementation(starter-test)` | 동일 |
| S-4 | bootJar 비활성 | 루트 `allprojects { BootJar { enabled = false } }` | **여기로 이동** — `tasks.withType<BootJar>().configureEach { enabled = false }` |

**불변식 INV-4**: S-1의 Spring Boot 플러그인 적용은 **필수**다. 카탈로그의 `springframework-boot-starter-*`가 버전 없이 선언되어 있고 그 버전은 이 플러그인의 BOM에서 온다. 제거하면 의존성 해석이 실패한다 (research.md R-4).

**불변식 INV-5**: S-2는 `:adapter-bot`에 `starter-web`을 주는 등 과다 공급으로 보이나, 현행과 동일하게 유지한다. 정리는 이번 범위 밖이다.

---

## E4. `test-conventions` — 이관 인벤토리

| # | 항목 | 현행 `configureByLabel("test")` | 이관 후 |
|---|---|---|---|
| T-1 | 플랫폼 | `testRuntimeOnly(junit-platform-launcher)`, `testRuntimeOnly(junit-jupiter-engine)` | 동일 |
| T-2 | BOM | `testImplementation(platform(junit-bom))` | 동일 |
| T-3 | API | `testImplementation(junit-jupiter-api)`, `testImplementation(junit-jupiter-params)` | 동일 |
| T-4 | 단언 | `testImplementation(assertj)` | 동일 |

**불변식 INV-6**: `:domain/build.gradle.kts`가 T-1 ~ T-4와 **동일한 5개 의존성을 이미 자기 스크립트에 선언**하고 있다. 중복이지만 선언이 완전히 같으므로 의존성 그래프는 변하지 않는다. **양쪽 모두 유지한다** — 중복 제거는 후속 이슈 #3.

---

## E5. `boot-conventions` — 이관 인벤토리

| # | 항목 | 현행 `configureByLabel("boot")` | 이관 후 |
|---|---|---|---|
| B-1 | 플러그인 적용 | `apply(plugin = "com.google.cloud.tools.jib")` | `plugins {}`에 `java`, `alias(libs.plugins.com.google.cloud.tools.jib)`, **그리고 `alias(libs.plugins.springframework.boot)` 재선언** |
| B-2 | bootJar 활성 | `tasks.withType<BootJar> { enabled = true }` | `configureEach` 사용 |
| B-3 | buildInfo | `springBoot { buildInfo() }` | 동일 |
| B-4 | jib base image | `from { image = "amazoncorretto:25.0.1-alpine" }` | 동일 |
| B-5 | jib target | `to { image = "kor-bot-spring"; tags = setOf("${project.version}") }` | 동일 |
| B-6 | jib container | `creationTime = USE_CURRENT_TIMESTAMP`, `jvmFlags` 2개, `workingDirectory = "/app"` | 동일 |

**불변식 INV-7**: B-1의 Spring Boot 플러그인 재선언은 **의도된 것**이다. precompiled script plugin의 타입 접근자(`springBoot {}`)는 자기 `plugins {}` 블록에 선언된 플러그인에서만 생성되므로, 빼면 `Unresolved reference 'springBoot'`로 컴파일이 실패한다. 적용 자체는 멱등이라 런타임 영향이 없다. **정리 대상으로 오인해 제거하지 말 것** — 이유를 주석으로 남긴다 (research.md R-3).

---

## E6. 최상위 빌드 잔존 항목

| # | 항목 | 처리 |
|---|---|---|
| RT-1 | `allprojects { group = "org.specter.converter"; version = "2.2.1" }` | **루트 유지** — 릴리스마다 사람이 고치는 값이고 jib 태그가 참조 (FR-007) |
| RT-2 | `allprojects { tasks.withType<BootJar> { enabled = false } }` | **삭제** → `spring-conventions`로 이동 (E3 S-4) |
| RT-3 | 루트 `repositories { mavenCentral() }` | `allprojects { repositories { mavenCentral() } }`로 통합 (FR-011) |
| RT-4 | `plugins {}`의 boot / dependency-management / jib / errorprone / build-recipe alias | **삭제** — 각 설정 묶음이 자립적으로 선언 |
| RT-5 | `plugins {}`의 spotless | `alias(libs.plugins.spotless) apply false`로 **유지** (BuildService 클래스로더 문제, research.md R-9) |
| RT-6 | 루트 `spotless {}` 블록 | **삭제** → `java-conventions`로 이동 (E2 J-11) |
| RT-7 | `configureByLabel(...)` 4블록 및 관련 import | **전부 삭제** |

---

## E7. 카탈로그 및 부가 설정

| # | 대상 | 변경 |
|---|---|---|
| C-1 | `gradle/libs.versions.toml` `[versions] linecorp-build-recipe-plugin` | 삭제 |
| C-2 | `gradle/libs.versions.toml` `[plugins] linecorp-build-recipe-plugin` | 삭제 |
| C-3 | `settings.gradle.kts` | 최상단에 `pluginManagement { includeBuild("build-logic"); repositories { gradlePluginPortal() } }` 추가. 기존 `module()` 헬퍼와 6개 include는 **변경 없음** |
| C-4 | `.github/dependabot.yml` gradle 항목 | `directory: /` → `directories: ["/", "/build-logic"]` (FR-015) |
| C-5 | `korConverter/**/gradle.properties` 6개 | 삭제. 다른 키가 없음을 확인함 — 파일 자체를 제거 |

**불변식 INV-8**: C-5의 6개 파일에는 `label=` 외의 키가 없다. 삭제 전 재확인한다.

---

## E8. 의존성 기준선(Baseline) — 검증 자료

| 속성 | 값 |
|---|---|
| 대상 | 6개 모듈 |
| 축 | 7개: `compileClasspath`, `runtimeClasspath`, `testCompileClasspath`, `testRuntimeClasspath`, `annotationProcessor`, `testAnnotationProcessor`, `errorprone` |
| 총 보고서 수 | **42건** |
| 캡처 시점 | 전환 작업 **시작 전**, 미변경 상태의 워크트리에서 |
| 저장 위치 | 스크래치패드 (저장소 밖) |
| 커밋 여부 | **하지 않음** — 커밋하면 의존성 갱신마다 함께 고쳐야 하는 부채가 된다 |
| 판정 | 전후 텍스트 diff 0건 |

**불변식 INV-9**: 기준선을 전환 이후에 캡처하면 SC-001은 아무것도 검증하지 못한다. 작업 순서상 이것이 **첫 번째 작업**이어야 한다.

**INV-10**: 7축은 스펙 SC-001이 요구하는 축 전부다(초안의 3축에서 개정됨). `errorprone` / `annotationProcessor` 축이 없으면 `errorprone(nullaway)` 누락 같은 실패를 컴파일·런타임 비교로 잡을 수 없다 (research.md R-10).
