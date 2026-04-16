# TimeZoneDefiner 복구 및 모듈 재배치 설계

- **날짜**: 2026-04-15
- **상태**: Implemented (`11ca706`)
- **관련 브랜치**: `feature/purist-ddd-migration`
- **관련 커밋**: `2f09b53` (common 모듈 삭제 — TimeZoneDefiner 증발 지점)

## 1. 배경

`runtime/cfg/logback-spring.xml:3`은 Logback 커스텀 `PropertyDefiner` 구현체 `org.specter.converter.common.utils.TimeZoneDefiner`를 참조한다. 이 클래스는 `TimeZone.getDefault().getID()`를 로그 패턴 변수 `${TIMEZONE}`에 공급한다.

커밋 `2f09b53`("build: restructure modules for purist DDD migration")에서 `common` 모듈이 "unused utility module"로 삭제되면서 해당 클래스도 함께 소실되었고, 현재 애플리케이션 기동 시 로그 패턴의 `${TIMEZONE}` 자리에 기본값 `UNDEFINED`가 찍히는 상태다.

## 2. 목표

Purist DDD + Hexagonal 아키텍처 규칙(프로젝트 `.claude/rules/*.md` + `purist-ddd-playbook` Parts 1·3·4·10)을 준수하면서 `TimeZoneDefiner` 클래스를 올바른 모듈에 복원하고, `logback-spring.xml`의 FQCN 참조를 갱신한다.

## 3. 비목표 (YAGNI)

- 타임존을 설정값(`application.yml` 등)으로 주입하는 기능 추가 — 원본 동작(JVM 기본 타임존) 유지
- 단위 테스트 추가 — JVM 환경 의존 10줄짜리 로깅 인프라 유틸리티로, 테스트 가치 낮음
- `runtime/cfg/*` → 모듈 `src/main/resources/*` 이동 같은 연관 리팩토링
- `common` 모듈 재생성

## 4. 결정

### 4.1 배치 모듈: `configuration`

- **경로**: `korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java`
- **패키지**: `org.specter.converter.configuration.logging`

### 4.2 대안 비교

| 후보 | 판정 | 근거 |
|------|------|------|
| `configuration` (채택) | ✅ | Playbook Part 10 §10.1: logback 커스텀 확장은 Configuration 모듈 담당. Part 1 §7.2: Cross-cutting infra(로깅/관측성/타임존) = Configuration |
| `boot` | ❌ | Playbook Part 1 §1.2 / Part 4 §4.2: Boot는 runnable artifact + `application.yml` 전용. 인프라 Bean/클래스 배치는 Configuration 책임 |
| `domain` / `application` | ❌ | 규칙 D-1 / A-1: `ch.qos.logback.*` 등 외부 의존 금지 |
| `adapter-bot` / `adapter-persistence` | ❌ | 규칙 AD-1/AD-2: Adapter는 Port/UseCase 중심. Cross-cutting 인프라의 자연스러운 자리 아님. Playbook에도 `adapter-logging` 별도 모듈 개념 없음 |
| `common` 모듈 재생성 | ❌ | 커밋 `2f09b53`의 의도(unused utility module 제거)와 정반대. 10줄 파일 하나 때문에 모듈 추가 과잉 |

### 4.3 파일 내용 (원본 보존)

```java
package org.specter.converter.configuration.logging;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyDefiner;
import java.util.TimeZone;

public class TimeZoneDefiner extends ContextAwareBase implements PropertyDefiner {

  @Override
  public String getPropertyValue() {
    return TimeZone.getDefault().getID();
  }
}
```

### 4.4 `runtime/cfg/logback-spring.xml:3` 갱신

```diff
- <define class="org.specter.converter.common.utils.TimeZoneDefiner" name="TIMEZONE"/>
+ <define class="org.specter.converter.configuration.logging.TimeZoneDefiner" name="TIMEZONE"/>
```

### 4.5 의존성 처리

`configuration` 모듈의 `build.gradle.kts` **수정 불필요**.

`./gradlew :configuration:dependencies --configuration compileClasspath` 확인 결과, `ch.qos.logback:logback-classic:1.5.32` 및 `logback-core:1.5.32`가 `spring-boot-starter-logging`을 통해 이미 transitive로 compileClasspath에 포함되어 있다.

> Trade-off 기록: "명시성"을 원하면 `compileOnly(libs.logback.core)` 추가 가능. 다만 Playbook은 logback 의존성을 Spring Boot Starter 계열에 위임하는 방식을 기본으로 하며, 현재 전이 경로가 안정적(공식 starter)이므로 추가 선언은 중복. 향후 configuration 모듈의 adapter 의존이 끊기는 리팩토링 발생 시 그 시점에 명시적 선언으로 전환하기로 한다.

## 5. 변경 파일 요약

| # | 파일 | 변경 | 비고 |
|---|------|------|------|
| 1 | `korConverter/configuration/src/main/java/org/specter/converter/configuration/logging/TimeZoneDefiner.java` | 신규 | 10 LOC |
| 2 | `runtime/cfg/logback-spring.xml` | 수정 (1 line) | FQCN 교체 |

## 6. 검증 절차

1. `./gradlew :configuration:build` — 컴파일 성공
2. `./gradlew build` — 전체 빌드 성공
3. `./gradlew spotlessCheck` — 포맷팅 적합
4. `./gradlew checkstyleMain` — Checkstyle 통과
5. 로컬 실행 시 (`./gradlew bootRun`) 로그 라인의 `${TIMEZONE}` 자리에 JVM 타임존 ID(예: `Asia/Seoul`) 출력 확인 (기존엔 `UNDEFINED`)
6. `.claude/rules/validation.md` 모듈별 금지 import 체크 — configuration 모듈 제약 없음

## 7. 근거 매트릭스 (Playbook & Rules)

| 출처 | 항목 | 적용 |
|------|------|------|
| `.claude/rules/domain.md` D-1 | `ch.qos.logback.*` 등 외부 의존 금지 | domain 배치 기각 |
| `.claude/rules/application.md` A-1 | 기술 의존 금지 | application 배치 기각 |
| `.claude/rules/adapter.md` AD-1/AD-2 | Adapter 책임 범위 한정 | adapter 배치 부적합 |
| Playbook Part 1 §1.2 | Boot ≠ Configuration. Boot = runnable artifact 전용 | boot 배치 기각 |
| Playbook Part 1 §7.2 | Cross-cutting infra → Configuration | configuration 배치 근거 |
| Playbook Part 4 §4.2 | Boot 모듈 리소스는 `application.yml` 중심, 기술 설정은 Configuration | boot 배치 기각 |
| Playbook Part 10 §10.1 | logback 커스텀 확장/PropertyDefiner → Configuration | configuration 배치 직접 근거 |

## 8. 리스크 및 완화

| 리스크 | 완화 |
|--------|------|
| logback-core transitive 경로 단절 (adapter 의존 구조 변경 시) | 해당 리팩토링 PR에서 `compileOnly(libs.logback.core)` 명시 선언으로 전환 |
| `runtime/cfg/logback-spring.xml`은 classpath 외부 파일 — 경로 해석 문제 | 기존과 동일 배치·동일 해석 경로 사용. 변경된 건 FQCN 문자열뿐이므로 추가 리스크 없음 |
| 타임존 로그 출력 없는 상태로 장기 운영 중이었을 가능성 | 검증 절차 #5에서 실제 로그 출력 확인 |

## 9. 출구 기준

- [ ] `TimeZoneDefiner.java`가 configuration 모듈에 생성됨
- [ ] `logback-spring.xml`의 FQCN 갱신됨
- [ ] `./gradlew build` 성공
- [ ] 실행 로그에서 `${TIMEZONE}` 값이 JVM 타임존 ID로 정상 출력됨
