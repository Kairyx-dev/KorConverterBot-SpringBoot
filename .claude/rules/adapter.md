---
paths: "**/adapter/**"
---
# Adapter Layer Rules

## AD-1: Inbound Adapter → Domain 직접 참조 금지
Controller/Listener는 Application의 Input Port(UseCase)에만 의존.
- MUST NOT: `*.domain.model.*`, `*.domain.event.*`, `*.domain.service.*`, `*.domain.exception.*`

## AD-2: Adapter 간 직접 참조 금지
`adapter-bot` ↔ `adapter-persistence` 순환 의존 방지.

## AD-3: SavePort = 이벤트 수거자
SavePort 구현체가 `pullDomainEvents()` 호출 → Outbox/publish, 같은 TX.
```java
@Override
public void save({Subject} entity) {
    // DB 저장
    // ...
    // 이벤트 수거 → 발행 (같은 TX)
    entity.pullDomainEvents().forEach(eventPublisher::publishEvent);
}
```

## AD-4: 명시적 매핑
Domain ↔ Technology 구조 간 Mapper 클래스 사용. 직접 변환 금지.
- Domain Entity ↔ jOOQ Record: `{Subject}PersistenceMapper`
- Domain Entity ↔ Request/Response DTO: Application Command/Result 경유

## AD-5: reconstitute() 사용
DB에서 로드한 Aggregate는 반드시 `reconstitute()` 팩토리 사용. public 생성자/create() 금지.

## AD-7: Optimistic Lock
SavePort UPDATE 시 `WHERE version = ?`. affected == 0 → `OptimisticLockException`.

## AD-6: JSpecify는 Adapter/Configuration에서만
`@Nullable`, `@NonNull`은 Adapter와 Configuration 모듈에서만 사용.
Domain, Application 모듈에서 금지.

## Discord Inbound Adapter (adapter-bot) 규칙
- JDA `ListenerAdapter` 구현
- UseCase(Input Port)에만 의존 — Domain import 금지
- Discord API 관련 로직(Embed 생성, 메시지 편집 등)은 Adapter 내부에만
- 요청 DTO → Command 변환 후 UseCase 호출

## jOOQ Outbound Adapter (adapter-persistence) 규칙
- jOOQ DSLContext is the Adapter's internal implementation detail
- Domain Entity ↔ jOOQ Record 매핑은 Mapper 클래스가 담당
- No JPA imports (`@Entity`, `@Table`, Spring Data Repository) anywhere
- No `jakarta.persistence.*` imports anywhere
- Flyway manages DDL migrations (`db/migration/*.sql`)
- Adapter 계층은 DSL 위임만 담당, 비즈니스 로직 금지

## T-1: UseCase = TX 경계
Configuration 모듈의 TX 프록시가 UseCase 메서드 실행을 감싼다.

## T-2: Controller/Listener에서 TX 시작 금지

## T-3: Port 내부에서 독립 TX 시작 금지

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
