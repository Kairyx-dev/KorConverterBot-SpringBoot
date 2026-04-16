# ADR-0002: jOOQ Generated Code Location

## Status
Accepted

## Context
`adapter-persistence` 모듈은 jOOQ codegen으로 Flyway migration 기반 타입 세이프 DSL을 생성한다.
초기 migration 계획(`docs/superpowers/plans/2026-04-15-purist-ddd-migration.md`)은 생성물을
`src/main/generated/` 에 두고 git에 체크인하는 방식으로 명시되었으나, 근거는 없었다.

소스 디렉토리에 체크인하는 방식의 문제:
- DDL 변경 시 수십 개의 generated 파일 diff가 리뷰 노이즈를 만든다
- `./gradlew clean`으로 정리되지 않는다 (소스 폴더라서)
- 팀원마다 generate 타이밍이 달라 머지 충돌 가능성
- Checkstyle/Spotless exclude 규칙을 수동으로 관리해야 한다

Gradle/jOOQ 표준 관례는 생성물을 `build/` 하위에 두고 git 추적에서 제외하는 것이다.

## Decision
jOOQ 생성 코드는 `build/generated/sources/jooq/main/` 에 출력하며 git에 커밋하지 않는다.

- `jooq.target.directory = layout.buildDirectory.dir("generated/sources/jooq/main")`
- `sourceSets.main.java.srcDir(jooqGeneratedDir)`
- `compileJava dependsOn jooqCodegen` 으로 빌드 시 자동 재생성
- `.gitignore` 의 기존 `build/` 규칙이 새 위치를 자동 커버

## Consequences
- Pro: `./gradlew clean`으로 완전 정리 가능, 소스 폴더가 깨끗해짐
- Pro: DDL 변경이 git diff에 generated 파일을 남기지 않음
- Pro: Gradle/jOOQ 관례와 일치
- Con: clone 직후에는 `./gradlew jooqCodegen`을 한 번 실행해야 IDE가 심볼을 인식
- Con: CI에서 빌드 시간이 (매우) 소폭 증가 (codegen 재실행)
