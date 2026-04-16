# Observability Stage 1: OTel 기반 + Actuator + 구조화 로깅

- **날짜**: 2026-04-16
- **상태**: Draft
- **관련 브랜치**: (신규 feature 브랜치)
- **참조**: purist-ddd-playbook Part 10 — Observability v1.1
- **관련 규칙**: D-1 (외부 의존 제로), D-3 (Domain 로깅 금지), A-1 (Application은 domain만 의존)

## 1. 배경

purist-DDD 마이그레이션(V001~V005) 완료 후, 프로젝트의 관측성(Observability)은 사실상 제로:
- `spring-boot-starter-actuator`가 의존에 있으나 web 서버가 없어 HTTP endpoint 미노출
- 구조화 로깅 미설정 (텍스트 패턴 logback)
- OpenTelemetry 미도입
- 메트릭/트레이스/로그 수집 인프라 없음

Playbook Part 10은 3대 신호(로그/트레이스/메트릭) 통합 관측성을 정의한다. 본 Stage 1은 **앱 수준 기반(foundation)**을 세운다.

### 1.1 3-Stage 로드맵

| Stage | 범위 | 독립 배포 |
|-------|------|:---------:|
| **1 (본 스펙)** | OTel 의존 + Actuator HTTP endpoint + ECS 구조화 로깅 + management port | ✅ |
| 2 | Grafana LGTM Docker Compose + OTLP 연결 확인 + OTel logback appender | ✅ |
| 3 | @Observed 비즈니스 메트릭 (Configuration 프록시) + DB HealthIndicator + SLO | ✅ |

### 1.2 Discord 봇 특성

| 항목 | 일반 웹 앱 | 본 프로젝트 |
|------|-----------|------------|
| HTTP 서버 | Tomcat/Netty | 없음 (JDA WebSocket) |
| 요청 트레이싱 | Controller → Service → DB | Discord event → UseCase → DB |
| K8s Probes | `/actuator/health/liveness` | Docker HEALTHCHECK |
| 분산 트레이싱 | 서비스 간 HTTP 전파 | 단일 서비스 (내부 span만) |

`spring-boot-starter-web`을 추가하되, 관리 전용 포트(`management.server.port: 8081`)로 격리하여 봇 기능과 분리.

## 2. 목표

- `spring-boot-starter-opentelemetry` 도입으로 Micrometer → OTLP 자동 export 파이프라인 구축
- `spring-boot-starter-web` + `management.server.port: 8081`로 Actuator HTTP endpoint 노출 (health, info, metrics, prometheus)
- console appender를 ECS(Elastic Common Schema) 구조화 로깅으로 전환 → traceId 자동 상관
- Docker HEALTHCHECK 추가
- OTLP endpoint는 환경변수(`OTEL_EXPORTER_OTLP_ENDPOINT`)로 설정 → Stage 2 LGTM 연결 시 코드 변경 불필요

## 3. 비목표 (YAGNI)

- Grafana LGTM Docker 컨테이너 — Stage 2
- OTel logback appender (OTLP log push) — Stage 2 (수신처 필요)
- @Observed 비즈니스 메트릭 (UseCase 프록시) — Stage 3
- 커스텀 DB HealthIndicator — Stage 3
- SLO 알람 정의 — Stage 3
- ScopedValue 컨텍스트 전파 — Stage 3
- PII 보호 로깅 규칙 검증 — Stage 3
- `spring-boot-docker-compose` 자동 감지 — Stage 2

## 4. 결정

### 4.1 의존성 배치

| 의존성 | 위치 | 근거 |
|--------|------|------|
| `spring-boot-starter-web` | `configureByLabel("spring")` (root build.gradle.kts) | 관리 전용 포트. 전 Spring 모듈에 web context 제공 |
| `spring-boot-starter-opentelemetry` | `configureByLabel("spring")` | OTel 자동 계측 (JDBC/jOOQ 포함) 전 모듈 대상 |
| `opentelemetry-logback-appender` | 이번 Stage 1에서 미도입 | Stage 2에서 LGTM 수신처와 함께 추가 |

### 4.2 libs.versions.toml 추가

```toml
[libraries]
springframework-boot-starter-web = { group = "org.springframework.boot", name = "spring-boot-starter-web" }
springframework-boot-starter-opentelemetry = { group = "org.springframework.boot", name = "spring-boot-starter-opentelemetry" }
```

Spring Boot BOM 관리 하에 있으므로 version 미지정.

### 4.3 build.gradle.kts — `configureByLabel("spring")` 변경

```kotlin
configureByLabel("spring") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        implementation(rootProject.libs.springframework.boot.starter)
        implementation(rootProject.libs.springframework.boot.starter.web)           // NEW
        implementation(rootProject.libs.springframework.boot.starter.opentelemetry) // NEW
        implementation(rootProject.libs.springframework.boot.starter.validation)
        implementation(rootProject.libs.springframework.boot.starter.actuator)
        implementation(rootProject.libs.springframework.boot.starter.json)

        testImplementation(rootProject.libs.springframework.boot.starter.test)
    }
}
```

### 4.4 application.yml 최종 형태

```yaml
spring:
  application:
    name: kor-converter-bot
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER_NAME}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 2
    baseline-description: "legacy JPA schema snapshot"

server:
  port: 8080

management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  otlp:
    metrics:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/metrics
    tracing:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/traces
    logging:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/logs
  tracing:
    sampling:
      probability: ${OTEL_SAMPLING_PROBABILITY:1.0}

logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO

bot:
  token: ${DISCORD_BOT_TOKEN}
```

**주의: `logging.structured.format.console: ecs`는 커스텀 logback-spring.xml이 존재하면 무시됨** (Spring Boot 기본 appender가 xml에 의해 오버라이드). 따라서 `application.yml`의 이 설정은 "xml이 없는 환경" fallback으로만 남기고, 실제 ECS 적용은 logback-spring.xml에서 직접 수행 (§4.5 참조).

### 4.5 logback-spring.xml 변경

**console appender만 교체. file/file-warn는 유지:**

현재:
```xml
<appender name="console" class="ch.qos.logback.core.ConsoleAppender">
  <encoder>
    <Pattern>%date{ISO8601} ${TIMEZONE:-UNDEFINED} %highlight(%-6level) ...</Pattern>
  </encoder>
</appender>
```

변경 후:
```xml
<appender name="console" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="org.springframework.boot.logging.logback.StructuredLogEncoder">
    <format>ecs</format>
  </encoder>
</appender>
```

- ECS JSON 출력에 traceId/spanId가 자동 포함 (OTel MDC 자동 주입)
- file appender는 기존 텍스트 패턴 유지 (로컬 디버깅용)
- `TimeZoneDefiner`의 `TIMEZONE` 변수는 console에서 더 이상 사용되지 않으나, file appender에서 여전히 사용

**구현 시 확인사항:** `StructuredLogEncoder`가 Spring Boot 4.0.4에서 logback-spring.xml 내부에서 사용 가능한지 검증 필요. 만약 불가하면 `net.logstash.logback.encoder.LogstashEncoder`(ECS 호환) 또는 커스텀 `JsonLayout`으로 대체.

### 4.6 deploy/docker-compose.yml 변경

```yaml
services:
  bot:
    container_name: kor-converter-bot
    image: kor-bot-spring:latest
    volumes:
      - ./cfg:/app/cfg
      - ./log:/app/log
    restart: always
    ports:
      - "8081:8081"
    networks:
      - converter_backend
    environment:
      TZ: "Asia/Seoul"
      DB_HOST: $DB_HOST
      DB_PORT: $DB_PORT
      DB_NAME: $DB_NAME
      DB_USER_NAME: $DB_USER_NAME
      DB_PASSWORD: $DB_PASSWORD
      DISCORD_BOT_TOKEN: $DISCORD_BOT_TOKEN
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT:-}
      OTEL_SAMPLING_PROBABILITY: ${OTEL_SAMPLING_PROBABILITY:-1.0}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

networks:
  converter_backend:
    external: true
```

**변경점:**
- `ports: ["8081:8081"]` — 관리 포트 노출
- `OTEL_EXPORTER_OTLP_ENDPOINT` 환경변수 — 비어있으면 OTel export가 localhost로 향하고 실패하지만 앱은 계속 동작 (OTel graceful degradation)
- `healthcheck` — Actuator health endpoint 폴링

**주의: `curl`이 컨테이너에 있어야 함.** `amazoncorretto:25-alpine` 기반이므로 `curl`이 없을 수 있음. 대안:
- JIB 설정에서 base image에 curl 포함된 것 사용
- 또는 `wget`으로 교체: `["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]`
- 또는 Java healthcheck script

### 4.7 JIB 설정 확인사항

현재 JIB `from.image = "amazoncorretto:25.0.1-alpine"`. Alpine 기반이므로 curl/wget 포함 여부 구현 시 확인. 없으면:
- 옵션 A: base image를 `amazoncorretto:25.0.1` (non-alpine, curl 포함)으로 변경
- 옵션 B: healthcheck를 `CMD-SHELL` + `java` 기반 HTTP check로 교체
- 옵션 C: Alpine에 `apk add curl` — JIB에서 비표준

## 5. 어댑터/코드 영향

| 파일 | 변경 | 근거 |
|------|------|------|
| `gradle/libs.versions.toml` | 2 라이브러리 추가 | web, opentelemetry |
| `build.gradle.kts` (root) | `configureByLabel("spring")`에 2 의존 추가 | |
| `runtime/cfg/application.yml` | `spring.application.name`, `server.*`, `management.*` 블록 추가 | |
| `runtime/cfg/logback-spring.xml` | console appender → ECS StructuredLogEncoder | |
| `deploy/docker-compose.yml` | ports, healthcheck, env vars | |
| **Java 코드** | **변경 없음** | 자동 계측 + 설정만 |

## 6. 테스트 전략

| 계층 | 검증 | 도구 |
|------|------|------|
| 빌드 | 의존성 resolve + 전 모듈 컴파일 | `./gradlew build` |
| 부팅 | Tomcat 8080 + management 8081 기동, Flyway 실행 | 기존 `@SpringBootTest` (E2E) |
| Actuator | `GET http://localhost:8081/actuator/health` → 200 | 수동 smoke test |
| ECS 로깅 | console stdout이 JSON 형태 | 수동 확인 (구현 시 스크린샷) |
| OTLP export | LGTM 미가동 시 export 실패해도 앱 비크래시 | 수동 확인 (graceful degradation) |
| 기존 테스트 회귀 | 전 테스트 통과 | `./gradlew build` |

**주의: `spring-boot-starter-web` 추가로 기존 `@SpringBootTest`의 context 로딩이 변경될 수 있음 (`WebEnvironment.MOCK` 기본).** E2E 테스트(`IgnoreUserE2ETest`)가 봇 기능 테스트인데 web context 로딩으로 인한 불필요한 Tomcat 초기화가 생길 수 있음. 구현 시 검증 필요. 문제 시 `@SpringBootTest(webEnvironment = WebEnvironment.NONE)` 적용.

## 7. 운영 배포 체크리스트

1. 사전: `runtime/cfg/application.yml` 운영 호스트에 갱신 (management 블록 포함)
2. 사전: `deploy/docker-compose.yml` 운영 호스트에 갱신 (ports, healthcheck)
3. 배포: 새 이미지 빌드 → 컨테이너 재시작
4. 사후: `curl http://localhost:8081/actuator/health` → `{"status":"UP"}` 확인
5. 사후: `curl http://localhost:8081/actuator/info` → 서비스 정보 확인
6. 사후: Docker `docker inspect <container> --format='{{.State.Health}}'` → healthy

## 8. 참고

- purist-ddd-playbook Part 10 — Observability v1.1
- Spring Boot 4 OpenTelemetry: <https://docs.spring.io/spring-boot/reference/actuator/opentelemetry.html>
- Spring Boot Structured Logging: <https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured>
- Elastic Common Schema: <https://www.elastic.co/guide/en/ecs/current/index.html>
