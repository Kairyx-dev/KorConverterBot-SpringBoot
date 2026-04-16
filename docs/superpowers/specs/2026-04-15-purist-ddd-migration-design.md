# Purist DDD + Hexagonal Architecture Migration Design

> KorConverterBot-SpringBoot: 헥사고날 아키텍처 → 순수주의 DDD + 헥사고날 전환

## 1. 배경 및 목표

### 현재 상태
- Java 25 + Spring Boot 4 Discord 영타→한글 변환 봇
- 6모듈 헥사고날 구조 (boot, common, domain, application, adapter-bot, adapter-jpa)
- Domain: Lombok record + @Builder, Domain Event/Exception 없음
- Application: 단일 DiscordBotInPort + 단일 JpaOutPort, Command/Query 없음
- Persistence: Spring Data JPA + @Entity
- 테스트: Domain converter 테스트만 존재
- CI: 빌드만 수행하는 GitHub Actions
- 품질 도구: ErrorProne + NullAway

### 목표
purist-ddd-playbook (Part 1~9, 11) 기준으로 전환:
- 순수 DDD Aggregate 패턴 (create/reconstitute, Domain Event, sealed Exception)
- CQRS Level 1 (Command/Query 분리, 3분할 Port)
- JPA → jOOQ + Flyway
- 70/15/10/5 테스트 피라미드
- 5단계 품질 방어선
- 5-gate CI 파이프라인

### 범위 제외
- Part 10: Observability (OpenTelemetry, Grafana LGTM)
- Part 12: Security & Authorization (OAuth2/OIDC, RBAC)
- Spring Modulith (단일 BC에서 불필요)
- 다중 Bounded Context

## 2. 결정사항 요약

| 항목 | 결정 |
|------|------|
| Bounded Context | 단일 BC 유지 |
| 전환 방식 | Big-bang (워크트리 분리) |
| MessageLog | Aggregate 아님 → Output Port 직접 저장 (감사 기록) |
| IgnoreUser | 정식 Aggregate (Full DDD) |
| Persistence | JPA → jOOQ + Flyway DDL 기반 코드 생성 |
| Mapper | MapStruct 유지 (playbook 허용) |
| Spring 설정 | @ComponentScan 제거 → 모듈별 AutoConfiguration |
| 품질 게이트 | 5단계 전부 도입 |

## 3. 모듈 구조

### 변경 전 → 후

```
[변경 전]                              [변경 후]
korConverter/                          korConverter/
├── boot/                              ├── boot/
│   └── configuration/  ←삭제          ├── configuration/  ←신규 독립 모듈
├── common/  ←제거                     └── hexagonal/
└── hexagonal/                             ├── domain/
    ├── domain/                            ├── application/  ←오타 수정
    ├── application/                       └── adapter/
    └── adapter/                               ├── adapter-bot/
        ├── adapter-bot/                       └── adapter-persistence/  ←rename
        └── adapter-jpa/  ←rename
```

### 모듈 의존 방향

```
boot → configuration → adapter-bot        → application → domain
                     → adapter-persistence → application → domain
```

### settings.gradle.kts

```kotlin
module(":korConverter:boot",                                    "korConverter/boot")
module(":korConverter:configuration",                           "korConverter/configuration")
module(":korConverter:hexagonal:domain",                        "korConverter/hexagonal/domain")
module(":korConverter:hexagonal:application",                   "korConverter/hexagonal/application")
module(":korConverter:hexagonal:adapter:adapter-bot",           "korConverter/hexagonal/adapter/adapter-bot")
module(":korConverter:hexagonal:adapter:adapter-persistence",   "korConverter/hexagonal/adapter/adapter-persistence")
```

### 모듈별 의존성

| 모듈 | dependencies |
|------|-------------|
| domain | 없음 (D-1) |
| application | domain |
| adapter-bot | application, JDA |
| adapter-persistence | application, domain, spring-boot-starter-jooq, flyway, postgresql, mapstruct |
| configuration | application, domain, adapter-bot, adapter-persistence, spring-tx |
| boot | configuration, adapter-bot, adapter-persistence |

> configuration은 조립(assembly) 모듈이므로 adapter 구체 클래스를 참조하여 UseCase TX 프록시를 생성한다.

### AutoConfiguration 등록

각 모듈은 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`로 자기 설정을 선언:

| 모듈 | AutoConfiguration 클래스 |
|------|--------------------------|
| configuration | `ConverterBeanAutoConfiguration` |
| adapter-bot | `BotAutoConfiguration` |
| adapter-persistence | `PersistenceAutoConfiguration` |

Boot 모듈은 `@SpringBootApplication`만 — scanBasePackages 제거.

## 4. Domain Layer

### 패키지 구조

```
org.specter.converter.domain/
├── model/
│   ├── IgnoreUser.java          # Aggregate Root (final class)
│   ├── IgnoreUserId.java        # VO (record)
│   ├── UserId.java              # VO (record)
│   ├── ChannelId.java           # VO (record)
│   ├── ConversionDomainService.java  # Domain Service (기존 ConverterCoreV2 rename)
│   ├── KeyboardIndex.java       # 유틸리티 (기존 유지)
│   └── KrDataIndex.java         # VO (record, 기존 유지)
├── event/
│   ├── IgnoreUserEvent.java     # sealed interface
│   ├── IgnoreUserAddedEvent.java    # record implements IgnoreUserEvent
│   └── IgnoreUserRemovedEvent.java  # record implements IgnoreUserEvent
└── exception/
    ├── IgnoreUserException.java           # sealed class extends RuntimeException
    ├── IgnoreUserNotFoundException.java   # final class
    └── IgnoreUserAlreadyExistsException.java  # final class
```

### IgnoreUser Aggregate

```java
public final class IgnoreUser {
    private final IgnoreUserId id;
    private final UserId userId;
    private final ChannelId channelId;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private final List<IgnoreUserEvent> domainEvents = new ArrayList<>();

    private IgnoreUser(IgnoreUserId id, UserId userId, ChannelId channelId,
                       String name, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.channelId = Objects.requireNonNull(channelId);
        this.name = Objects.requireNonNull(name);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
    }

    public static IgnoreUser create(UserId userId, ChannelId channelId,
                                     String name, Instant now) {
        var user = new IgnoreUser(IgnoreUserId.UNSAVED, userId, channelId,
                                   name, now, now, 0L);
        user.registerEvent(new IgnoreUserAddedEvent(
            UUID.randomUUID(), "IGNORE_USER_ADDED",
            user.id.value(), now, user.version));
        return user;
    }

    public static IgnoreUser reconstitute(IgnoreUserId id, UserId userId,
                                           ChannelId channelId, String name,
                                           Instant createdAt, Instant updatedAt,
                                           long version) {
        return new IgnoreUser(id, userId, channelId, name, createdAt, updatedAt, version);
    }

    public List<IgnoreUserEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void registerEvent(IgnoreUserEvent event) {
        domainEvents.add(event);
    }
}
```

### Value Objects

ID 생성 전략: **DB BIGSERIAL** 유지.

playbook은 UUIDv7을 권장하지만, 이 프로젝트는:
- Discord ID가 long 기반 (Snowflake)
- 기존 스키마가 BIGSERIAL
- 단일 BC + 단일 DB에서 UUID의 이점이 적음

따라서 IgnoreUserId는 DB가 생성한 값을 받는 방식으로 운영.
create() 시에는 임시 ID(0L)를 사용하고, save() 후 DB 반환값으로 교체.
이 결정은 ADR로 문서화한다.

```java
public record IgnoreUserId(long value) {
    public static final IgnoreUserId UNSAVED = new IgnoreUserId(0L);
    public IgnoreUserId { if (value < 0) throw new IllegalArgumentException(); }
}
public record UserId(long value) {
    public UserId { if (value <= 0) throw new IllegalArgumentException(); }
}
public record ChannelId(long value) {
    public ChannelId { if (value <= 0) throw new IllegalArgumentException(); }
}
```

### Domain Event (sealed interface + 5필드)

```java
public sealed interface IgnoreUserEvent
    permits IgnoreUserAddedEvent, IgnoreUserRemovedEvent {
    UUID eventId();
    String eventType();
    long aggregateId();
    Instant occurredAt();
    long aggregateVersion();
}

public record IgnoreUserAddedEvent(
    UUID eventId, String eventType, long aggregateId,
    Instant occurredAt, long aggregateVersion
) implements IgnoreUserEvent {}
```

### Domain Exception (sealed class)

```java
public sealed class IgnoreUserException extends RuntimeException
    permits IgnoreUserNotFoundException, IgnoreUserAlreadyExistsException {
    protected IgnoreUserException(String message) { super(message); }
}
public final class IgnoreUserNotFoundException extends IgnoreUserException {
    public IgnoreUserNotFoundException(UserId userId, ChannelId channelId) {
        super("IgnoreUser not found: userId=%d, channelId=%d"
              .formatted(userId.value(), channelId.value()));
    }
}
public final class IgnoreUserAlreadyExistsException extends IgnoreUserException {
    public IgnoreUserAlreadyExistsException(UserId userId, ChannelId channelId) {
        super("IgnoreUser already exists: userId=%d, channelId=%d"
              .formatted(userId.value(), channelId.value()));
    }
}
```

### 규칙 준수 검증

- D-1: dependencies {} 비어있음
- D-2: @Entity, @Component 등 프레임워크 어노테이션 없음
- D-3: 로깅 없음
- D-4: Instant.now() 없음 — Application에서 전달
- D-5: Rich Domain Model (create/reconstitute + registerEvent)
- D-6: VO는 record
- D-7: Primitive Obsession 방지 (UserId, ChannelId, IgnoreUserId)
- D-8: create()/reconstitute() 분리
- D-9: cross-Aggregate 참조 없음 (Aggregate 1개)
- D-11: Domain Service에 Port 호출 없음
- D-12: 금지 접미사 없음
- D-13: sealed interface (Event), sealed class (Exception)
- D-14: Objects.requireNonNull

## 5. Application Layer

### 패키지 구조

```
org.specter.converter.application/
├── port/
│   ├── input/
│   │   ├── AddIgnoreUserUseCase.java
│   │   ├── RemoveIgnoreUserUseCase.java
│   │   ├── ConvertMessageUseCase.java
│   │   └── CheckIgnoreUserUseCase.java
│   └── output/
│       ├── LoadIgnoreUserPort.java
│       ├── SaveIgnoreUserPort.java
│       ├── IgnoreUserQueryPort.java
│       └── RecordMessageLogPort.java
├── dto/
│   ├── command/
│   │   ├── AddIgnoreUserCommand.java
│   │   ├── RemoveIgnoreUserCommand.java
│   │   ├── ConvertMessageCommand.java
│   │   └── RecordMessageLogCommand.java
│   ├── query/
│   │   └── CheckIgnoreUserQuery.java
│   └── result/
│       ├── IgnoreUserResult.java
│       └── ConvertMessageResult.java
└── service/
    ├── AddIgnoreUserService.java
    ├── RemoveIgnoreUserService.java
    ├── ConvertMessageService.java
    └── CheckIgnoreUserService.java
```

### Input Ports

```java
public interface AddIgnoreUserUseCase {
    IgnoreUserResult execute(AddIgnoreUserCommand command);
}
public interface RemoveIgnoreUserUseCase {
    void execute(RemoveIgnoreUserCommand command);
}
public interface ConvertMessageUseCase {
    ConvertMessageResult execute(ConvertMessageCommand command);
}
public interface CheckIgnoreUserUseCase {
    boolean execute(CheckIgnoreUserQuery query);
}
```

### Output Ports (3분할 + 기록용)

```java
public interface LoadIgnoreUserPort {
    Optional<IgnoreUser> loadByUserIdAndChannelId(UserId userId, ChannelId channelId);
}
public interface SaveIgnoreUserPort {
    void save(IgnoreUser ignoreUser);
    void delete(IgnoreUser ignoreUser);
}
public interface IgnoreUserQueryPort {
    List<IgnoreUserResult> findAllByChannelId(long channelId);
    boolean existsByUserIdAndChannelId(long userId, long channelId);
}
public interface RecordMessageLogPort {
    void record(RecordMessageLogCommand command);
}
```

### Command / Query / Result (record, 원시 타입)

```java
public record AddIgnoreUserCommand(long userId, long channelId, String name) {}
public record RemoveIgnoreUserCommand(long userId, long channelId) {}
public record ConvertMessageCommand(String message, long guildId, long channelId,
                                     String nickName, String effectiveName) {}
public record RecordMessageLogCommand(long guildId, String channel, String nickName,
                                       String effectiveName, String message,
                                       boolean converted, String convertedMessage,
                                       long channelId) {}
public record CheckIgnoreUserQuery(long userId, long channelId) {}
public record IgnoreUserResult(long id, long userId, long channelId, String name) {}
public record ConvertMessageResult(String originalMessage, String convertedMessage,
                                    boolean converted) {}
```

### Service 예시

```java
public class AddIgnoreUserService implements AddIgnoreUserUseCase {
    private final LoadIgnoreUserPort loadPort;
    private final SaveIgnoreUserPort savePort;
    private final Clock clock;

    public AddIgnoreUserService(LoadIgnoreUserPort loadPort,
                                 SaveIgnoreUserPort savePort, Clock clock) {
        this.loadPort = Objects.requireNonNull(loadPort);
        this.savePort = Objects.requireNonNull(savePort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public IgnoreUserResult execute(AddIgnoreUserCommand command) {
        var userId = new UserId(command.userId());
        var channelId = new ChannelId(command.channelId());

        loadPort.loadByUserIdAndChannelId(userId, channelId)
            .ifPresent(existing -> { throw new IgnoreUserAlreadyExistsException(); });

        var ignoreUser = IgnoreUser.create(userId, channelId, command.name(),
                                            clock.instant());
        savePort.save(ignoreUser);
        return new IgnoreUserResult(ignoreUser.id().value(), command.userId(),
                                     command.channelId(), command.name());
    }
}
```

### 규칙 준수 검증

- A-1: domain만 의존
- A-2: UseCase 인터페이스 정의
- A-3: Output Port 3분할 (Load/Save/Query)
- A-4: @Service/@Transactional/@Component 없음
- A-5: if/else 비즈니스 판단 없음 (Domain 위임)
- A-6: eventPublisher 직접 호출 없음
- A-7: Command/Query 원시 타입
- A-8: UseCase→UseCase 호출 없음
- A-9: 1 TX = 1 Aggregate
- A-10: DTO는 record

## 6. Adapter Layer

### Inbound Adapter (adapter-bot)

```
org.specter.converter.adapter.bot/
├── listener/
│   ├── MessageListener.java     # ConvertMessageUseCase, CheckIgnoreUserUseCase
│   └── CommandListener.java     # AddIgnoreUserUseCase, RemoveIgnoreUserUseCase
├── configuration/
│   └── BotAutoConfiguration.java  # JDA Bean + Listener Bean + BotProperties
├── properties/
│   └── BotProperties.java
└── exception/
    └── UnEditableMessageException.java
```

- UseCase(Input Port)에만 의존 (AD-1)
- Domain model/event/service/exception import 없음
- Command/Query 생성 → UseCase 호출

### Outbound Adapter (adapter-persistence)

```
org.specter.converter.adapter.persistence/
├── mapper/
│   └── IgnoreUserPersistenceMapper.java   # MapStruct (jOOQ Record ↔ Domain)
├── port/
│   ├── IgnoreUserPersistenceAdapter.java  # implements LoadIgnoreUserPort, SaveIgnoreUserPort
│   ├── IgnoreUserQueryAdapter.java        # implements IgnoreUserQueryPort
│   └── MessageLogRecordAdapter.java       # implements RecordMessageLogPort
├── configuration/
│   └── PersistenceAutoConfiguration.java  # DSLContext, Adapter Bean, Flyway
└── generated/                              # jOOQ 자동 생성 코드
```

### IgnoreUser PersistenceAdapter 핵심 로직

- reconstitute() 사용하여 DB → Domain 복원 (AD-5)
- save(): INSERT or UPDATE WHERE version = ? (AD-7)
- affected == 0 → OptimisticLockException
- pullDomainEvents() → applicationEventPublisher.publishEvent() (AD-3)
- MapStruct로 명시적 매핑 (AD-4)

### MessageLog RecordAdapter

- 단순 jOOQ INSERT (Aggregate 아님)
- Domain Event, Optimistic Lock 없음

### Flyway DDL

```sql
-- V001__create_ignore_user.sql
CREATE TABLE ignore_user (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    channel_id  BIGINT      NOT NULL,
    name        VARCHAR(255),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ignore_user_lookup ON ignore_user (user_id, channel_id);

-- V002__create_message_log.sql
CREATE TABLE message_log (
    id                BIGSERIAL PRIMARY KEY,
    guild             VARCHAR(255),
    channel           VARCHAR(255),
    nick_name         VARCHAR(255),
    effective_name    VARCHAR(255),
    message           TEXT,
    is_converted      BOOLEAN NOT NULL DEFAULT false,
    converted_message TEXT,
    channel_id        BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 7. Configuration Module

### ConverterBeanAutoConfiguration

```java
@AutoConfiguration
public class ConverterBeanAutoConfiguration {

    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    @Bean
    public ConversionDomainService conversionDomainService() {
        return new ConversionDomainService();
    }

    // Command UseCases — R/W TX 프록시
    @Bean
    public AddIgnoreUserUseCase addIgnoreUserUseCase(
            IgnoreUserPersistenceAdapter adapter, Clock clock,
            PlatformTransactionManager txManager) {
        return createTxProxy(
            new AddIgnoreUserService(adapter, adapter, clock),
            AddIgnoreUserUseCase.class, txManager);
    }

    @Bean
    public RemoveIgnoreUserUseCase removeIgnoreUserUseCase(
            IgnoreUserPersistenceAdapter adapter, Clock clock,
            PlatformTransactionManager txManager) {
        return createTxProxy(
            new RemoveIgnoreUserService(adapter, adapter, clock),
            RemoveIgnoreUserUseCase.class, txManager);
    }

    @Bean
    public ConvertMessageUseCase convertMessageUseCase(
            ConversionDomainService conversionService,
            MessageLogRecordAdapter messageLogAdapter,
            PlatformTransactionManager txManager) {
        return createTxProxy(
            new ConvertMessageService(conversionService, messageLogAdapter),
            ConvertMessageUseCase.class, txManager);
    }

    // Query UseCases — R/O TX 프록시
    @Bean
    public CheckIgnoreUserUseCase checkIgnoreUserUseCase(
            IgnoreUserQueryAdapter queryAdapter,
            PlatformTransactionManager txManager) {
        return createReadOnlyTxProxy(
            new CheckIgnoreUserService(queryAdapter),
            CheckIgnoreUserUseCase.class, txManager);
    }

    @SuppressWarnings("unchecked")
    private <T> T createTxProxy(T target, Class<T> iface,
                                 PlatformTransactionManager txManager) {
        var template = new TransactionTemplate(txManager);
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(), new Class<?>[]{iface},
            (proxy, method, args) -> template.execute(
                status -> ReflectionUtils.invokeMethod(method, target, args)));
    }

    @SuppressWarnings("unchecked")
    private <T> T createReadOnlyTxProxy(T target, Class<T> iface,
                                         PlatformTransactionManager txManager) {
        var template = new TransactionTemplate(txManager);
        template.setReadOnly(true);
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(), new Class<?>[]{iface},
            (proxy, method, args) -> template.execute(
                status -> ReflectionUtils.invokeMethod(method, target, args)));
    }
}
```

### TX 규칙 준수

- T-1: UseCase = TX 경계 (Configuration 프록시)
- T-2: Listener에서 TX 시작 없음
- T-3: Port 내부 독립 TX 없음

## 8. 빌드 인프라 + 품질 게이트

### libs.versions.toml 추가 항목

```toml
[versions]
jooq = "3.20.4"
flyway = "11.8.2"
testcontainers = "1.21.1"
archunit = "1.4.0"
pit = "1.17.4"
spotless = "7.0.4"
checkstyle = "10.25.0"
jqwik = "1.9.3"
```

> 실제 구현 시점에 최신 안정 버전을 확인하여 조정한다.

### 5단계 품질 방어선

| 방어선 | 도구 | 적용 시점 |
|--------|------|-----------|
| 0 (로컬) | Lefthook | pre-commit: Spotless + Checkstyle, pre-push: Unit + ArchUnit |
| 1 (컴파일) | Gradle 모듈 의존 | domain dependencies {} 비어있음 |
| 2 (정적분석) | ErrorProne + NullAway + Spotless + Checkstyle | 빌드 시 |
| 3 (테스트) | ArchUnit + JUnit + JaCoCo 80% | 테스트 시 |
| 4 (CI) | PIT Mutation (domain 70%) | CI 파이프라인 |

### CI Pipeline (5-gate)

```
Gate 1: spotlessCheck + checkstyleMain + compileJava
  ↓
Gate 2: test + jacocoTestCoverageVerification (ArchUnit 포함)
  ↓
Gate 3: integrationTest (Testcontainers)
  ↓
Gate 4: pitest (mutation testing)
  ↓
Gate 5: dependencyCheckAnalyze (OWASP)
```

## 9. 테스트 전략

### 테스트 피라미드 (70/15/10/5)

| 레이어 | 비율 | Spring | 도구 | 위치 |
|--------|:----:|:------:|------|------|
| Domain | 70% | X | JUnit + AssertJ + jqwik | domain/src/test/ |
| Application | 15% | X | JUnit + Mockito | application/src/test/ |
| Adapter | 10% | 최소 | Testcontainers + jOOQ | adapter-persistence/src/test/ |
| E2E | 5% | 전체 | @SpringBootTest | boot/src/test/ |

### Domain 테스트

- Aggregate: create() 행위 + pullDomainEvents() 검증
- VO: jqwik Property-Based 테스트 (경계값 탐색)
- ConversionDomainService: 기존 12개 parameterized 테스트 이관
- 고정 시간: `Instant.parse("2026-01-01T00:00:00Z")`

### Application 테스트

- Mock Port (Mockito) — Spring Context 없음
- 오케스트레이션 로직 검증 (Command → VO 변환 → Domain → SavePort)

### Adapter 테스트

- Testcontainers PostgreSQL 싱글턴 컨테이너
- jOOQ 실제 쿼리 실행
- reconstitute() 복원 검증
- Optimistic Lock 충돌 시나리오

### E2E 테스트

- @SpringBootTest + Testcontainers
- UseCase 전체 흐름 (추가 → 확인 → 삭제)

### ArchUnit 테스트 (boot 모듈)

- domain → Spring/Jakarta 의존 금지
- application → Spring/Jakarta 의존 금지
- adapter-bot → domain.model/event/service/exception 의존 금지
- adapter 간 cross-reference 금지

## 10. 작업 순서 (Inside-Out)

1. **Domain**: VO, Event, Exception, Aggregate (IgnoreUser), Domain Service rename
2. **Application**: Port (Input/Output), Command/Query/Result, Service
3. **Adapter-Persistence**: Flyway DDL, jOOQ 코드 생성, Mapper, PersistenceAdapter, QueryAdapter, RecordAdapter
4. **Adapter-Bot**: Listener 리팩토링 (UseCase 호출 변경)
5. **Configuration**: AutoConfiguration + TX 프록시
6. **Boot**: @SpringBootApplication 단순화, AutoConfiguration 등록
7. **Build**: jOOQ + Flyway 플러그인, Spotless, Checkstyle, JaCoCo, PIT, ArchUnit, Lefthook
8. **Test**: Domain 70% → Application 15% → Adapter 10% → E2E 5%
9. **CI**: 5-gate GitHub Actions 파이프라인
10. **Rules/CLAUDE.md**: 규칙 파일 업데이트, ADR 작성
