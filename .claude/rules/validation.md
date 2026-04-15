# Self-Validation Checklist

> 코드 생성 후, 커밋 전 반드시 실행하는 자기검증 목록.

## 모듈별 금지 import

```
[domain — 위반 시 즉시 수정]
MUST_NOT: org.springframework.*
MUST_NOT: org.jooq.*
MUST_NOT: com.fasterxml.*
MUST_NOT: org.slf4j.*
MUST_NOT: org.apache.logging.*
MUST_NOT: jakarta.*
MUST_NOT: org.jspecify.*
MUST_NOT: lombok.*
MUST_NOT: java.time.Clock
MUST_NOT: java.time.Instant (Instant.now() 호출)

[application — 위반 시 즉시 수정]
MUST_NOT: org.springframework.*
MUST_NOT: org.jooq.*
MUST_NOT: com.fasterxml.*
MUST_NOT: org.jspecify.*
MUST_NOT: lombok.*

[adapter-bot (input) — 위반 시 즉시 수정]
MUST_NOT: *.domain.model.*
MUST_NOT: *.domain.event.*
MUST_NOT: *.domain.service.*
MUST_NOT: *.domain.exception.*
```

## 필수 패턴 검증

```
[Aggregate Root]
MUST: private 생성자
MUST: create() static 팩토리 + registerEvent()
MUST: reconstitute() static 팩토리 + 이벤트 미발행
MUST: pullDomainEvents()
MUST: 모든 파라미터 Objects.requireNonNull
MUST: 비즈니스 로직 내부 캡슐화 — Anemic Model 금지 (D-5)
MUST_NOT: 다른 Aggregate 객체 참조 — ID 참조만 (D-9)

[Value Object]
MUST: record
MUST: Compact Constructor 자체 검증
MUST: 원시값 래핑 — Primitive Obsession 금지 (D-7)

[Domain Event]
MUST: sealed interface 그룹화
MUST: 5필드 (eventId, eventType, aggregateId, occurredAt, aggregateVersion)

[Domain Exception]
MUST: sealed class 그룹화

[Domain Service]
MUST_NOT: Port/Repository 주입 (D-11)

[Application Service]
MUST: @Service/@Transactional/@Component 없음 (A-4)
MUST: eventPublisher 직접 호출 없음 (A-6)
MUST: if/else 비즈니스 판단 없음 — Domain 위임 (A-5)
MUST: 1 TX = 1 Aggregate (A-9)
MUST_NOT: UseCase→UseCase 직접 호출 (A-8)

[Input Port]
MUST: 모든 UseCase 인터페이스 정의 (A-2)

[Output Port]
MUST: 3분할 — Load/Save/Query (A-3)
MUST: Optional 반환 — null 반환 금지

[Command/Query/DTO]
MUST: 원시 타입 필드만 (A-7)
MUST: record (A-10)

[SavePort 구현체]
MUST: UPDATE WHERE version = ? (AD-7)
MUST: affected == 0 → OptimisticLockException (AD-7)
MUST: pullDomainEvents() → 발행 (AD-3)
MUST: reconstitute() 사용 (AD-5)
MUST: 명시적 Mapper (AD-4)

[Adapter]
MUST: UseCase(Input Port)에만 의존 (AD-1)
MUST_NOT: Adapter 간 참조 (AD-2)

[트랜잭션]
MUST: UseCase = TX 경계 — Configuration 프록시 (T-1)
MUST_NOT: Listener에서 TX 시작 (T-2)
MUST_NOT: Port 내부 독립 TX (T-3)
```

## 검증 절차

1. 위 체크리스트를 코드에 대해 순차 검증
2. 위반 발견 시 즉시 수정
3. `./gradlew build` 실행하여 컴파일 검증
4. 테스트 통과 확인

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
