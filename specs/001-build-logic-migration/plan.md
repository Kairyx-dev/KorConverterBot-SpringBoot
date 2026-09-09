# Implementation Plan: build-recipe-plugin → build-logic Convention Plugin 전환

**Branch**: `chore/build-logic-migration` | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-build-logic-migration/spec.md`

## Summary

루트 `build.gradle.kts` 한 파일에 집중된 `configureByLabel("java"|"spring"|"test"|"boot")` 4개 블록을, `build-logic` composite build의 precompiled script plugin 4개로 옮긴다. 모듈은 라벨 문자열 대신 `plugins { id("...-conventions") }`로 필요한 설정을 명시 선언한다.

계약은 **무행위변경**이다. 검증은 "빌드가 통과한다"가 아니라 **전환 전후 의존성 보고서의 텍스트 diff 공백**이며, 이 기준선을 전환 작업 시작 **전에** 확보하는 것이 T002의 역할이다.

기술적 미지수는 없다. 이 계획의 모든 기술 선택은 스펙 작성 전 단계에서 이 저장소의 실제 도구 조합(Gradle 9.7.1 / JDK 25.0.3)으로 **동작하는 스파이크를 세워 확인**했다. 근거는 [research.md](./research.md)에 정리했다.

## Technical Context

**Language/Version**: Kotlin DSL (Gradle 9.7.1 내장 Kotlin 2.4.0, `kotlin-dsl` 플러그인) — 빌드 정의용. 애플리케이션 소스(Java 25)는 변경 없음.

**Primary Dependencies**:
- `dev.panuszewski.typesafe-conventions` 0.11.1 — precompiled script plugin 안에서 `libs.*` 타입세이프 접근자 제공
- Gradle plugin marker: `org.springframework.boot` 4.1.1, `io.spring.dependency-management` 1.1.7, `net.ltgt.errorprone` 5.1.1, `com.diffplug.spotless` 8.10.2, `com.google.cloud.tools.jib` 3.5.4

**Storage**: N/A — 빌드 정의 변경. 애플리케이션 데이터 계층 무관.

**Testing**: 이 기능의 검증은 단위 테스트가 아니라 **의존성 보고서 텍스트 diff** + 기존 CI 5게이트 통과다. 애플리케이션 테스트는 한 줄도 추가/수정하지 않는다.

**Target Platform**: 개발자 로컬(Linux/WSL2, JDK 25) 및 GitHub Actions(`ubuntu-latest`, corretto 25).

**Project Type**: Gradle multi-project 빌드의 빌드 정의(build infrastructure).

**Performance Goals**: 명시적 목표 없음. 부수 효과로 spotless가 모듈별 태스크로 분할되어 병렬/증분이 가능해지나, 성능은 수용 기준이 아니다.

**Constraints**:
- 프로젝트 격리(Isolated Projects) 사용 불가 — typesafe-conventions #186, multi-project build-logic에서 재현됨
- `build-logic`은 최상위 빌드를 통해서만 구동 — 단독 구동 시 `:compilePluginsBlocks` 실패
- 설정 묶음의 jvmTarget은 데몬 JVM을 무경고로 추종 — toolchain 25 명시 고정 필요
- 빌드 구성 캐시(configuration cache) 도입은 범위 밖

**Scale/Scope**: 6개 모듈, 라벨 4종 → 설정 묶음 4개. 신규 파일 7개, 수정 파일 10개, 삭제 파일 6개.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md`는 **플레이스홀더가 채워지지 않은 초기 템플릿**이다(`[PRINCIPLE_1_NAME]` 등). 대조할 성문 원칙이 존재하지 않으므로 형식적 게이트는 없다. 대신 이 저장소에서 실제로 구속력을 갖는 규칙 문서를 게이트로 삼는다.

| 게이트 | 출처 | 판정 | 근거 |
|---|---|---|---|
| 모듈별 금지 import (domain/application/adapter) | `.claude/rules/validation.md` | **해당 없음** | 애플리케이션 소스 무변경 — `.java` 파일을 하나도 만들거나 고치지 않는다 |
| Aggregate/VO/Port 필수 패턴 | `.claude/rules/validation.md` | **해당 없음** | 동일 |
| 헥사고날 모듈 의존 방향 | `CLAUDE.md` 모듈 구조 | **통과** | 모듈 간 `implementation(project(...))` 선언을 변경하지 않는다. 의존 방향 불변은 SC-001로 기계 검증된다 |
| 중요한 결정 발생 시 ADR 작성 | `CLAUDE.md` 워크플로우 | **통과 예정** | FR-016 / T030+T031. ADR-0004 + `docs/decisions/index.md` 갱신 |
| 코드 생성 후 자기검증 실행 | `CLAUDE.md` 워크플로우 | **통과 예정** | T023의 7축 의존성 diff가 이 전환에 대응하는 자기검증이다 |
| 빌드 검증 `./gradlew build` | `CLAUDE.md` | **통과 예정** | T024 |

**Post-Design 재평가**: Phase 1 설계 산출물([data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)) 작성 후 재확인 — 위반 없음. 설계는 빌드 정의 파일에만 국한되며 애플리케이션 계층 규칙과 접점이 없다.

> **후속 권고(범위 밖)**: `constitution.md`가 빈 템플릿으로 방치되어 있다. 이 저장소는 `.claude/rules/`에 실질적 규범이 이미 있으므로, 그것을 constitution으로 승격하면 이후 speckit 기능들이 실제 게이트를 갖게 된다. 이번 전환의 범위는 아니다.

## Project Structure

### Documentation (this feature)

```text
specs/001-build-logic-migration/
├── plan.md                        # 이 파일
├── spec.md                        # 기능 명세
├── research.md                    # Phase 0 — 기술 선택 근거 및 실측 결과
├── data-model.md                  # Phase 1 — 설정 묶음 ↔ 모듈 매핑
├── quickstart.md                  # Phase 1 — 검증 절차
├── contracts/
│   └── convention-plugins.md      # Phase 1 — 설정 묶음이 모듈에 제공하는 계약
├── checklists/
│   └── requirements.md            # 명세 품질 체크리스트
└── tasks.md                       # /speckit-tasks 산출물
```

### Source Code (repository root)

```text
build-logic/                                    # [신규] composite build
├── settings.gradle.kts                         # typesafe-conventions 0.11.1, include("conventions")
└── conventions/
    ├── build.gradle.kts                        # kotlin-dsl, toolchain 25, pluginMarker 5종
    └── src/main/kotlin/
        ├── java-conventions.gradle.kts         # [신규] 라벨 "java" 이관 — 6개 모듈 전부
        ├── spring-conventions.gradle.kts       # [신규] 라벨 "spring" 이관 — 4개 모듈
        ├── test-conventions.gradle.kts         # [신규] 라벨 "test" 이관 — domain
        └── boot-conventions.gradle.kts         # [신규] 라벨 "boot" 이관 — boot

settings.gradle.kts                             # [수정] pluginManagement { includeBuild("build-logic") }
build.gradle.kts                                # [수정] configureByLabel 4블록 제거, spotless apply false

korConverter/
├── boot/
│   ├── build.gradle.kts                        # [수정] plugins { java+spring+boot-conventions }
│   └── gradle.properties                       # [삭제] label=java,spring,boot
├── configuration/
│   ├── build.gradle.kts                        # [수정] plugins { java+spring-conventions }
│   └── gradle.properties                       # [삭제]
└── hexagonal/
    ├── domain/
    │   ├── build.gradle.kts                    # [수정] plugins { java+test-conventions } + 기존 pitest 유지
    │   └── gradle.properties                   # [삭제]
    ├── application/
    │   ├── build.gradle.kts                    # [수정] plugins { java-conventions }
    │   └── gradle.properties                   # [삭제]
    └── adapter/
        ├── adapter-bot/
        │   ├── build.gradle.kts                # [수정] plugins { java+spring-conventions }
        │   └── gradle.properties               # [삭제]
        └── adapter-persistence/
            ├── build.gradle.kts                # [수정] plugins 추가 + 기존 jooq 블록 유지
            └── gradle.properties               # [삭제]

gradle/libs.versions.toml                       # [수정] linecorp-build-recipe-plugin 항목 제거
.github/dependabot.yml                          # [수정] directories: ["/", "/build-logic"]
docs/decisions/0004-build-logic-convention-plugins.md   # [신규] ADR
docs/decisions/index.md                         # [수정] ADR-0004 행 추가
```

**Structure Decision**: unity(`/home/kshull/project/nanoit/java/unity`)와 동일한 **multi-project `build-logic`** 구조를 채택했다 — `build-logic/settings.gradle.kts`가 `include("conventions")`로 서브프로젝트를 한 겹 판다.

단일 프로젝트 `build-logic`(파일 한 개 적음, Isolated Projects 호환)이라는 대안이 실측으로 확인되었으나, **참조 프로젝트와의 구조 일치**를 우선해 multi-project를 유지하기로 결정했다. 그 대가인 "Isolated Projects 사용 불가"는 수용된 제약으로 ADR-0004에 기록한다. 근거와 재현 결과는 [research.md](./research.md) R-6.

애플리케이션 소스 트리(`korConverter/**/src/`)는 **한 파일도 건드리지 않는다**.

## Complexity Tracking

Constitution Check에 위반이 없으므로 정당화할 항목이 없다. 다만 **의도적으로 더 단순한 대안을 기각한 결정 1건**을 투명성을 위해 기록한다.

| 선택 | 더 단순한 대안 | 기각 사유 |
|---|---|---|
| multi-project `build-logic` (`include("conventions")`) | 단일 프로젝트 `build-logic` — 디렉터리·빌드 파일 각 1개 감소, Isolated Projects 호환 | 참조 프로젝트(unity)와 구조를 일치시켜 두 저장소를 오가는 유지보수자의 인지 부하를 낮추는 쪽을 택했다. Isolated Projects는 현재 사용하지 않으며, 도입 검토 시 이 구조를 재평가한다 (research.md R-6) |
| 설정 묶음 4개 (`test-conventions` 포함) | 3개 — `test-conventions`는 소비자가 `:domain` 하나뿐이고, 그 모듈이 동일 의존성을 이미 자기 스크립트에 선언 중 | 현행 라벨 4종과 1:1 대응을 유지해 이관의 추적 가능성을 확보했다. 소비자 1개 묶음과 중복 선언은 후속 이슈로 분리 (spec.md 범위 경계) |
