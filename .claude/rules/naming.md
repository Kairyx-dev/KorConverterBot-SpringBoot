# Naming Conventions

## 계층별 네이밍 패턴

| 계층 | 객체 | 패턴 | 예시 |
|------|------|------|------|
| **Adapter (In)** | Request DTO | `{Subject}Request` | `IgnoreUserRequest` |
| **Adapter (In)** | Response DTO | `{Subject}Response` | `ConvertResponse` |
| **Application** | Command | `{Verb}{Subject}Command` | `AddIgnoreUserCommand` |
| **Application** | Query | `{Verb}{Subject}Query` | `FindIgnoreUserQuery` |
| **Application** | Command UseCase | `{Verb}{Subject}UseCase` | `AddIgnoreUserUseCase` |
| **Application** | Query UseCase | `{Verb}{Subject}UseCase` | `FindIgnoreUserUseCase` |
| **Application** | Service | `{Verb}{Subject}Service` | `AddIgnoreUserService` |
| **Domain** | Domain Service | `{Subject}DomainService` | `ConversionDomainService` |
| **Domain** | Entity (Aggregate Root) | `{Subject}` | `IgnoreUser`, `MessageLog` |
| **Domain** | Value Object | `{Subject}` | `UserId`, `ChannelId`, `GuildId` |
| **Domain** | Domain Event | `{AggregateRoot}{PastTenseVerb}Event` | `IgnoreUserAddedEvent` |
| **Domain** | Domain Exception | `{Subject}Exception` (sealed) | `IgnoreUserException` |
| **Application** | Load Port | `Load{Subject}Port` | `LoadIgnoreUserPort` |
| **Application** | Save Port | `Save{Subject}Port` | `SaveIgnoreUserPort` |
| **Application** | Query Port | `{Subject}QueryPort` | `IgnoreUserQueryPort` |
| **Adapter (Out)** | Persistence Adapter | `{Subject}PersistenceAdapter` | `IgnoreUserPersistenceAdapter` |
| **Adapter (Out)** | Query Adapter | `{Subject}QueryAdapter` | `IgnoreUserQueryAdapter` |
| **Adapter (Out)** | Mapper | `{Subject}PersistenceMapper` | `IgnoreUserPersistenceMapper` |

## 금지 접미사 (D-12)
`*Handler`, `*Processor`, `*Manager`, `*Helper`, `*Util`, `*VO`, `*Entity`

## 패키지 구조

```
org.specter.converter.domain/
    ├── model/          # Entity(Aggregate Root), VO, Enum
    ├── event/          # DomainEvent sealed interface + record
    ├── exception/      # sealed class + final subclasses
    └── service/        # Domain Service (Port 호출 금지)

org.specter.converter.application/
    ├── port/
    │   ├── input/      # UseCase interfaces
    │   └── output/     # Load/Save/Query Port interfaces
    ├── dto/
    │   ├── command/    # Command records (원시 타입)
    │   └── query/      # Query records (원시 타입)
    └── service/        # UseCase 구현체

org.specter.converter.adapter.bot/        # Inbound: Discord JDA
    ├── listener/       # MessageListener, CommandListener
    ├── configuration/  # JDA Bean setup
    └── properties/     # BotProperties

org.specter.converter.adapter.persistence/  # Outbound: jOOQ + Flyway Persistence
    ├── configuration/  # PersistenceAutoConfiguration
    ├── generated/      # jOOQ generated code (do not edit)
    ├── mapper/         # Domain ↔ jOOQ Record Mapper
    └── port/           # Port 구현체 (PersistenceAdapter, QueryAdapter)
```

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
