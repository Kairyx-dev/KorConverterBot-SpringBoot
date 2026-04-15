---
paths: "**/domain/**"
---
# Domain Layer Rules

Base package: `org.specter.converter.domain`

## D-1: ZERO 외부 의존
`build.gradle.kts`의 `dependencies {}` 블록은 비어 있어야 한다.
- MUST NOT: `org.springframework.*`, `org.jooq.*`, `com.fasterxml.*`, `jakarta.*`
- MUST NOT: `org.slf4j.*`, `org.apache.logging.*` (D-3: 로깅 프레임워크 금지)
- MUST NOT: `org.jspecify.*` (JSpecify는 Adapter/Configuration에서만)
- MUST NOT: `lombok.*` (Lombok은 순수 도메인에 불필요)
- MUST NOT: `java.time.Clock`, `Instant.now()` (D-4: 시스템 시계 금지. Application이 Instant을 파라미터로 전달)

## D-5: Rich Domain Model
모든 비즈니스 로직은 Entity/VO 메서드 내부에 캡슐화한다. Getter/Setter만 있는 Anemic Model 금지.

## D-6: VO = `record`
예외 없음. Compact Constructor에서 자체 검증(null-check, format validation).

## D-7: Primitive Obsession 금지
도메인 의미 있는 원시값은 반드시 VO로 래핑한다.
- `long userId` → `UserId(long value)`
- `long channelId` → `ChannelId(long value)`
- `String name` → 적절한 VO

## D-8: create() / reconstitute() 분리
```java
private {Subject}(/* all fields */) { /* null-check */ }
public static {Subject} create(/* params, Instant now */) { /* + registerEvent */ }
public static {Subject} reconstitute(/* all fields + version */) { /* 이벤트 미발행 */ }
```
public 생성자 금지. 외부에서 new 호출 불가.

## D-9: Inter-Aggregate 객체 참조 금지
다른 Aggregate는 ID(VO) 참조만 허용. 객체 참조 금지.

## D-10: Domain에 Repository 금지
Repository 인터페이스는 Application의 Output Port에만 위치.

## D-11: Domain Service → Port 호출 금지
Domain Service는 Port/Repository를 주입받지 않는다. 필요한 데이터는 Application이 파라미터로 전달.

## D-12: 금지 접미사
`*Handler`, `*Processor`, `*Manager`, `*Helper`, `*Util`, `*VO`, `*Entity`

## D-13: sealed interface / sealed class
- Domain Event = `sealed interface` (Aggregate별 그룹화)
- Domain Exception = `sealed class` (switch exhaustiveness)

## D-14: null-check
Entity/Domain Service의 모든 파라미터는 `Objects.requireNonNull`로 검증.

## Aggregate Root 필수 구조
```java
public final class {Subject} {
    private final {Subject}Id id;
    private final long version;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private {Subject}(/* all fields */) { this.id = Objects.requireNonNull(id); }

    public static {Subject} create(/* params, Instant now */) {
        var entity = new {Subject}(/* ... */);
        entity.registerEvent(new {Subject}CreatedEvent(/* 5 fields + payload */));
        return entity;
    }

    public static {Subject} reconstitute(/* all fields + version */) {
        return new {Subject}(/* ... */);
    }

    private void registerEvent(DomainEvent e) { domainEvents.add(e); }
    public List<DomainEvent> pullDomainEvents() {
        var e = List.copyOf(domainEvents); domainEvents.clear(); return e;
    }
}
```

## Domain Event 필수 5필드
```java
public sealed interface DomainEvent {
    UUID eventId();            // 멱등성 키
    String eventType();        // 역직렬화 디스크리미네이터
    UUID aggregateId();        // 파티셔닝 키
    Instant occurredAt();      // 비즈니스 시간 (Application에서 전달)
    long aggregateVersion();   // 순서 검증
}
```
eventId, aggregateId는 UUID 직접 사용 (VO 래핑 아님).

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
