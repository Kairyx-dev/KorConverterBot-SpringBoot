---

description: "Task list for build-recipe-plugin → build-logic convention plugin migration"
---

# Tasks: build-recipe-plugin → build-logic Convention Plugin 전환

**Input**: Design documents from `/specs/001-build-logic-migration/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: 테스트 태스크 **없음**. 명세가 TDD를 요청하지 않았고, 이 기능의 검증은 새 테스트 작성이 아니라 **전환 전후 의존성 보고서 비교**와 기존 CI 게이트 재현이다. 애플리케이션 테스트 코드는 한 줄도 추가·수정하지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (서로 다른 파일, 미완료 태스크에 의존하지 않음)
- **[Story]**: 대응하는 사용자 스토리 (US1~US4)

## ⚠️ 이 기능의 스토리 독립성에 대한 정직한 진술

템플릿은 스토리별 독립 배포를 전제하지만, **이 기능에서는 성립하지 않는다**. 명세 결정에 따라 전환은 **빅뱅 단일 변경**이며(중간 공존 상태를 만들지 않음), US1(설정 이관)과 US2(동등성 증명)는 같은 원자적 변경의 두 측면이다. US1만 배포하고 US2를 미루는 것은 "검증하지 않은 빌드 교체를 머지한다"는 뜻이라 선택지가 아니다.

따라서 여기서 스토리 경계가 뜻하는 것은 **독립 배포**가 아니라 **독립 검증**이다. 각 Phase 끝의 체크포인트에서 그 스토리의 수용 기준만 따로 판정할 수 있다. MVP 스코프 역시 "US1만"이 아니라 **Phase 1~4 (US1+US2)** 다.

---

## Phase 1: Setup (작업 공간 및 기준선)

**Purpose**: 격리된 작업 공간 확보와, 이후 모든 검증의 기준이 되는 의존성 기준선 캡처

- [X] T001 `.worktrees/build-logic-migration`에 `chore/build-logic-migration` 브랜치로 워크트리 생성 (`git worktree add`). 이후 모든 작업은 이 워크트리에서 수행하고 `main` 워크트리는 건드리지 않는다
- [X] T002 **[선행 필수]** 미변경 상태의 워크트리에서 기준선 42건 캡처 → 스크래치패드 `baseline/`. 6개 모듈(domain, application, adapter-bot, adapter-persistence, configuration, boot) × 7축(compileClasspath, runtimeClasspath, testCompileClasspath, testRuntimeClasspath, annotationProcessor, testAnnotationProcessor, errorprone). 절차는 [quickstart.md](./quickstart.md) V-0
- [X] T003 [P] `korConverter/**/gradle.properties` 6개 파일에 `label=` 외의 키가 없음을 재확인 ([data-model.md](./data-model.md) INV-8). 다른 키가 있으면 삭제가 아니라 그 키만 남기는 편집으로 계획을 수정한다

**⚠️ CRITICAL**: T002를 건너뛰거나 전환 이후에 수행하면 SC-001이 아무것도 검증하지 못한다 ([data-model.md](./data-model.md) INV-9)

**Checkpoint**: 스크래치패드에 42개 파일 존재. 워크트리는 아직 미변경 상태

---

## Phase 2: Foundational (build-logic 뼈대)

**Purpose**: composite build가 조립되는 최소 골격. 설정 묶음 작성 전에 완료되어야 한다

**⚠️ CRITICAL**: 이 Phase가 끝나기 전에는 어떤 설정 묶음도 작성할 수 없다

- [X] T004 `build-logic/settings.gradle.kts` 생성 — `pluginManagement { repositories { gradlePluginPortal() } }`, `plugins { id("dev.panuszewski.typesafe-conventions") version "0.11.1" }`, `rootProject.name = "build-logic"`, `include("conventions")`. 버전을 카탈로그에 넣지 않는 이유(자기 자신이 카탈로그 등록 주체라 순환)를 주석으로 남긴다
- [X] T005 `build-logic/conventions/build.gradle.kts` 생성 — `plugins { \`kotlin-dsl\` }`, `repositories { gradlePluginPortal(); mavenCentral() }`, **Java toolchain 25 명시 고정** ([research.md](./research.md) R-5), `dependencies`에 `pluginMarker(...)` 5종: springframework-boot, spring-dependency-management, net-ltgt-errorprone, spotless, com-google-cloud-tools-jib
- [X] T006 `settings.gradle.kts` 최상단에 `pluginManagement { includeBuild("build-logic"); repositories { gradlePluginPortal() } }` 추가. 기존 `module()` 헬퍼와 6개 include는 변경하지 않는다 ([data-model.md](./data-model.md) C-3)
- [X] T007 `./gradlew help --console=plain` 실행하여 build-logic 조립 확인. `:build-logic:conventions:compileKotlin`과 `:jar`가 수행되고 `BUILD SUCCESSFUL`이어야 한다 ([quickstart.md](./quickstart.md) V-1)

**Checkpoint**: build-logic이 빌드에 참여하나 아직 아무 설정 묶음도 없다. 루트는 여전히 build-recipe로 동작 중이며 빌드가 깨지지 않은 상태여야 한다

---

## Phase 3: User Story 1 - 모듈 스크립트만 보고 빌드 설정 파악 (Priority: P1) 🎯 MVP

**Goal**: 라벨 문자열 간접참조를 제거하고, 각 모듈이 자기 스크립트에서 설정 묶음을 명시 선언하도록 전환

**Independent Test**: 임의 모듈의 `build.gradle.kts` 한 파일만 열어 적용 설정 묶음을 읽어낼 수 있고, `find korConverter -name gradle.properties`가 빈 결과를 낸다

### 설정 묶음 작성 (4개 파일 — 전부 병렬 가능)

- [X] T008 [P] [US1] `build-logic/conventions/src/main/kotlin/java-conventions.gradle.kts` 작성 — [data-model.md](./data-model.md) E2의 J-1~J-12 인벤토리 전체 이관. J-6(NullAway `"com.uber"`)과 J-8(JaCoCo 미연결 규칙)은 **하자이나 그대로 유지**하고, 주석에는 **아직 존재하지 않는 이슈 번호 대신 `specs/001-build-logic-migration/data-model.md` J-6 / J-8 앵커를 참조**한다 (이슈 생성은 Phase 7 T032다 — 이 시점에 번호가 없다). J-11의 `targetExclude("**/build/**", "**/generated/**")` 누락 금지 ([research.md](./research.md) R-9)
- [X] T009 [P] [US1] `build-logic/conventions/src/main/kotlin/spring-conventions.gradle.kts` 작성 — [data-model.md](./data-model.md) E3의 S-1~S-4. Spring Boot 플러그인 적용이 BOM 공급원이므로 제거 불가임을 주석으로 명시 (INV-4). S-4로 `tasks.withType<BootJar>().configureEach { enabled = false }` 포함
- [X] T010 [P] [US1] `build-logic/conventions/src/main/kotlin/test-conventions.gradle.kts` 작성 — [data-model.md](./data-model.md) E4의 T-1~T-4 (JUnit BOM/API/Params/Engine/Launcher, AssertJ)
- [X] T011 [P] [US1] `build-logic/conventions/src/main/kotlin/boot-conventions.gradle.kts` 작성 — [data-model.md](./data-model.md) E5의 B-1~B-6. **Spring Boot 플러그인 재선언 필수**이며 그 이유(타입 접근자 생성 범위, [research.md](./research.md) R-3)를 주석으로 남긴다. 없으면 `Unresolved reference 'springBoot'`로 컴파일 실패

### 최상위 빌드 정리

- [X] T012 [US1] `build.gradle.kts` 재작성 — [data-model.md](./data-model.md) E6의 RT-1~RT-7. `configureByLabel` 4블록과 관련 import 전부 삭제, `plugins`는 `alias(libs.plugins.spotless) apply false`만 남김(BuildService 클래스로더 이유를 주석으로), `allprojects`에 group/version/`repositories { mavenCentral() }` 통합

### 모듈 스크립트에 설정 묶음 선언 (6개 파일 — 전부 병렬 가능)

- [X] T013 [P] [US1] `korConverter/hexagonal/domain/build.gradle.kts`에 `plugins { id("java-conventions"); id("test-conventions"); alias(libs.plugins.pitest) }` 적용. 기존 `pitest {}` 블록과 `dependencies {}`는 변경하지 않는다
- [X] T014 [P] [US1] `korConverter/hexagonal/application/build.gradle.kts`에 `plugins { id("java-conventions") }` 추가. 기존 `dependencies {}` 변경 없음
- [X] T015 [P] [US1] `korConverter/hexagonal/adapter/adapter-bot/build.gradle.kts`에 `plugins { id("java-conventions"); id("spring-conventions") }` 추가
- [X] T016 [P] [US1] `korConverter/hexagonal/adapter/adapter-persistence/build.gradle.kts`에 `id("java-conventions")`, `id("spring-conventions")`를 기존 `alias(libs.plugins.jooq.codegen)`과 함께 선언. `jooq {}`·`sourceSets`·`compileJava dependsOn jooqCodegen` 블록은 변경하지 않는다
- [X] T017 [P] [US1] `korConverter/configuration/build.gradle.kts`에 `plugins { id("java-conventions"); id("spring-conventions") }` 추가
- [X] T018 [P] [US1] `korConverter/boot/build.gradle.kts`에 `plugins { id("java-conventions"); id("spring-conventions"); id("boot-conventions") }` 추가

### 라벨 메커니즘 제거

- [X] T019 [US1] `korConverter/**/gradle.properties` 6개 파일 삭제 (boot, configuration, hexagonal/application, hexagonal/domain, hexagonal/adapter/adapter-bot, hexagonal/adapter/adapter-persistence)
- [X] T020 [US1] `gradle/libs.versions.toml`에서 `[versions] linecorp-build-recipe-plugin`과 `[plugins] linecorp-build-recipe-plugin` 두 항목 삭제 ([data-model.md](./data-model.md) C-1, C-2)

**Checkpoint**: 라벨 파일 0개, `configureByLabel` 참조 0건. 이 시점의 빌드 성공 여부는 아직 검증되지 않았다 — Phase 4가 판정한다

---

## Phase 4: User Story 2 - 전환이 런타임 동작을 바꾸지 않았음을 증명 (Priority: P1) 🎯 MVP

**Goal**: 무행위변경 계약을 기계적으로 증명

**Independent Test**: 42건 의존성 보고서 diff가 0건이고, 빌드·실행 아카이브·컨테이너 이미지 산출이 전환 전과 동일

- [X] T021 [US2] `./gradlew help --console=plain` 재실행하여 4개 설정 묶음이 전부 해석되는지 확인. 실패 유형별 대응은 [quickstart.md](./quickstart.md) V-1 표 참조
- [X] T022 [US2] 전환 후 42건 덤프 → 스크래치패드 `after/`. T002와 **완전히 동일한 모듈·축·명령**으로 수행 ([quickstart.md](./quickstart.md) V-2)
- [X] T023 [US2] **핵심 게이트** — `diff -r baseline/ after/` 실행. 출력 0건, 종료 코드 0이어야 한다. 차이 발생 시 해당 모듈·축 파일을 열어 [data-model.md](./data-model.md) E2~E5 인벤토리에서 누락된 항목을 찾아 수정 후 T022부터 재실행 (SC-001)
- [X] T024 [US2] `./gradlew clean build --console=plain` 실행. `:boot:bootJar` 산출물 생성, `:configuration`/`:adapter-bot`/`:adapter-persistence`의 `bootJar`는 `SKIPPED`이고 산출물 없음을 확인 ([quickstart.md](./quickstart.md) V-3, SC-003)
- [X] T025 [US2] `./gradlew :boot:jibBuildTar` 실행 후 `korConverter/boot/build/jib-image.tar` 존재 확인. **경로가 바뀌면 `tag-cd.yml`의 SCP 전송이 깨진다** ([quickstart.md](./quickstart.md) V-6)
- [X] T026 [US2] `spotlessCheck`가 `:adapter-persistence`의 jOOQ 생성 소스를 검사 대상에 포함하지 않음을 확인 ([quickstart.md](./quickstart.md) V-4, FR-012)

**Checkpoint**: 무행위변경 계약 증명 완료. 이 시점이 **MVP 완료 지점**이며 머지 가능한 최소 상태다

---

## Phase 5: User Story 3 - 기존 빌드 명령어가 그대로 동작 (Priority: P2)

**Goal**: 전환의 파급 범위가 빌드 정의 안에 갇혀 있음을 확인

**Independent Test**: CI/훅 정의 파일의 diff가 비어 있고, 거기 적힌 명령이 그대로 성공

- [X] T027 [US3] `git diff main -- .github/workflows/ lefthook.yml`이 **빈 diff**임을 확인. 변경이 있으면 그 변경이 정말 불가피한지 재검토한다 (SC-004)
- [X] T028 [US3] CI 4개 게이트를 로컬에서 재현 — `./gradlew spotlessCheck checkstyleMain compileJava`, `./gradlew :domain:test :application:test :boot:test`, `./gradlew :adapter-persistence:test`(Docker 필요), `./gradlew :domain:pitest`. Docker 미가용 시 Gate 3만 CI에 위임 ([quickstart.md](./quickstart.md) V-5, SC-002)

**Checkpoint**: 파이프라인과 훅이 변경 없이 동작

---

## Phase 6: User Story 4 - 빌드 정의 의존성도 자동 업데이트 대상 (Priority: P3)

**Goal**: build-logic 안의 버전이 Dependabot 시야에 들어오게 한다

**Independent Test**: Dependabot 설정이 build-logic 디렉터리를 스캔 대상으로 선언

- [ ] T029 [US4] `.github/dependabot.yml`의 gradle 항목에서 `directory: /`를 `directories: ["/", "/build-logic"]`로 변경. 기존 `ignore`/`groups`/`assignees`/`labels` 설정은 유지 (FR-015)

**Checkpoint**: build-logic의 typesafe-conventions 버전이 갱신 대상에 포함됨

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 결정 기록, 후속 작업 분리, 최종 확인

- [ ] T030 [P] `docs/decisions/0004-build-logic-convention-plugins.md` 작성 — 전환 배경(build-recipe의 라벨 문자열 간접참조: IDE 정의 이동·자동완성·타입 검사 모두 불가), 채택 구조(multi-project build-logic + typesafe-conventions), **수용한 제약 2건**(Isolated Projects 사용 불가 — [research.md](./research.md) R-6 / build-logic 단독 구동 금지 — R-7), **불변식 1건**(toolchain 25 고정, 팀에 더 낮은 JVM이 생기면 그 하한으로 내린다 — R-5), 후속 과제(configuration cache 도입) (FR-016, SC-009)
- [ ] T031 `docs/decisions/index.md`에 ADR-0004 행 추가 (제목 / Accepted / 2026-09-08)
- [ ] T032 [P] 후속 이슈 4건 등록 (`gh issue create`) — ① `JacocoCoverageVerification` 80% 룰이 `check`에 미연결되어 실효 없음, ② `NullAway:AnnotatedPackages`가 `"com.uber"`로 박혀 `org.specter.converter`를 검사하지 않음, ③ `test-conventions`와 `:domain/build.gradle.kts`의 junit/assertj 중복 선언, ④ configuration cache 도입(Isolated Projects는 #186 때문에 build-logic 구조 재검토 필요) (FR-017, SC-008)
- [X] T033 최종 확인 — `find korConverter -name gradle.properties`가 빈 결과(SC-005), `build-logic/conventions/build.gradle.kts`에 toolchain 선언 1곳 존재하고 데몬 JVM 위임 지점 0곳(SC-010), `git diff main`에 애플리케이션 `.java` 변경 0건
- [X] T033a **명시 선언 검증 (SC-006)** — 6개 모듈 `build.gradle.kts` 각각이 자기 파일 안에 `id("...-conventions")`를 선언하는지 확인. 기대: 6/6. 라벨·상속·`subprojects` 등 다른 파일을 읽어야 알 수 있는 암묵적 적용 경로가 0건이어야 한다 (`grep -c 'id("[a-z-]*-conventions")' <각 모듈 스크립트>`)
- [X] T033b **버전 중복 검증 (SC-007)** — `build-logic/` 이하에서 리터럴 버전 문자열을 추출해 `gradle/libs.versions.toml`의 항목과 대조. 카탈로그에 이미 있는 구성요소의 버전이 빌드 정의에 리터럴로 재선언된 건이 0건이어야 한다. **유일한 허용 예외**는 `build-logic/settings.gradle.kts`의 typesafe-conventions 버전 1건(자기 참조 순환 회피). 2건 이상이면 실패 — 이 기준이 typesafe-conventions 채택의 근거이므로([research.md](./research.md) R-1) 반드시 판정한다
- [ ] T034 PR 생성 — 본문에 T023의 diff 결과(42건 0차이)와 T028의 게이트 결과를 기록한다. 기준선 파일 자체는 커밋하지 않는다 ([data-model.md](./data-model.md) E8)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존 없음. **T002가 모든 것의 선행 조건**
- **Phase 2 (Foundational)**: Phase 1 완료 후. 모든 스토리를 차단
- **Phase 3 (US1)**: Phase 2 완료 후
- **Phase 4 (US2)**: **Phase 3 완료 후** — 빅뱅 전환이라 US1 없이 US2를 검증할 수 없다. 템플릿의 "스토리 병렬 진행"이 이 기능에는 적용되지 않는다
- **Phase 5 (US3)**: Phase 4 완료 후 (빌드가 통과해야 게이트 재현이 의미를 가짐)
- **Phase 6 (US4)**: Phase 2 이후 언제든 가능 — 다른 스토리와 **진짜로 독립적인 유일한 스토리**
- **Phase 7 (Polish)**: Phase 4~6 완료 후

### 태스크 수준 의존

- T002 → (Phase 3 전체). 기준선 없이 전환을 시작하면 되돌릴 수 없다
- T004, T005 → T006 → T007
- T008~T011 (설정 묶음 4개) → T013~T018 (모듈 선언). 묶음이 없으면 `id("...")` 해석 실패
- T012 (루트 정리) → T021. 루트에 `configureByLabel`이 남아 있으면 중복 설정이 된다
- T022 → T023. T023 실패 시 수정 후 T022부터 재실행
- T030 → T031
- **T008 ⊥ T032** — T008의 주석은 이슈 번호가 아니라 `data-model.md`의 J-6/J-8 앵커를 가리키므로 T032(Phase 7 이슈 생성)에 의존하지 않는다. 이슈 번호를 코드 주석에 넣고 싶다면 T032를 Phase 1로 앞당겨야 한다

### Parallel Opportunities

| 그룹 | 태스크 | 근거 |
|---|---|---|
| 설정 묶음 작성 | T008, T009, T010, T011 | 서로 다른 4개 파일. 상호 참조 없음 |
| 모듈 선언 | T013, T014, T015, T016, T017, T018 | 서로 다른 6개 모듈 스크립트 |
| 문서/이슈 | T030, T032 | 서로 다른 산출물 |
| 독립 스토리 | T029 (US4) | Phase 2 이후 아무 때나 |

**병렬 불가에 주의**: T012(루트 재작성)는 T013~T018과 파일이 다르지만, 루트가 정리되기 전에는 모듈에 설정 묶음과 라벨 설정이 **이중 적용**된다. 같은 커밋 안에서 함께 완료해야 한다.

---

## Parallel Example: Phase 3 설정 묶음 작성

```bash
# 4개 설정 묶음을 동시에 작성 (서로 다른 파일)
Task: "java-conventions.gradle.kts — data-model.md E2의 J-1~J-12 이관"
Task: "spring-conventions.gradle.kts — data-model.md E3의 S-1~S-4"
Task: "test-conventions.gradle.kts — data-model.md E4의 T-1~T-4"
Task: "boot-conventions.gradle.kts — data-model.md E5의 B-1~B-6"

# 이어서 6개 모듈 스크립트를 동시에 수정
Task: ":domain 에 java+test-conventions 선언"
Task: ":application 에 java-conventions 선언"
Task: ":adapter-bot 에 java+spring-conventions 선언"
Task: ":adapter-persistence 에 java+spring-conventions 선언"
Task: ":configuration 에 java+spring-conventions 선언"
Task: ":boot 에 java+spring+boot-conventions 선언"
```

---

## Implementation Strategy

### MVP = Phase 1~4 (US1 + US2)

1. Phase 1 — 워크트리 + **기준선 42건** (T002를 빠뜨리지 말 것)
2. Phase 2 — build-logic 뼈대, `./gradlew help` 통과
3. Phase 3 — 설정 묶음 4개 + 모듈 6개 + 라벨 제거
4. Phase 4 — **42건 diff 0건**. 여기서 멈춰 검증한다
5. 이 지점이 머지 가능한 최소 상태다

### 이후 증분

6. Phase 5 — CI/훅 무변경 확인 및 게이트 재현
7. Phase 6 — Dependabot 확장 (언제든 병렬 가능)
8. Phase 7 — ADR-0004, 후속 이슈 4건, PR

### 되돌리기

전환은 단일 브랜치의 단일 PR이다. 어느 단계에서든 실패하면 워크트리를 버리고 `main`에서 다시 시작한다 — `main`은 T001부터 끝까지 미변경 상태로 유지된다.

---

## Notes

- 이 기능은 애플리케이션 소스 코드를 **한 줄도** 변경하지 않는다. `git diff main`에 `.java` 파일이 나타나면 범위 이탈이다
- J-6(NullAway 패키지)과 J-8(JaCoCo 미연결)은 **알려진 하자를 의도적으로 그대로 옮기는 것**이다. "고치면서 옮기고 싶다"는 충동이 이 전환의 가장 큰 위험이다 — 지금 고치면 무행위변경 계약이 깨지고 T023 diff에 원인이 섞인다
- `boot-conventions`의 Spring Boot 플러그인 재선언과 `spring-conventions`의 Spring Boot 플러그인 적용은 **둘 다 필수**다. 중복으로 보인다는 이유로 제거하면 각각 컴파일 실패·의존성 해석 실패로 이어진다
- 커밋은 Phase 단위로 끊되, T012와 T013~T018은 같은 커밋에 넣는다
