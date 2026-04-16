---
paths: "**/application/**"
---
# Application Layer Rules

Base package: `org.specter.converter.application`

## A-1: Domain만 의존
- MUST NOT: `org.springframework.*`, `org.jooq.*`, `com.fasterxml.*`, `org.jspecify.*`

## A-2: Input Port 인터페이스 필수
모든 UseCase는 인터페이스로 정의한다. "단순 CRUD라서 생략" 금지.
모든 조회도 UseCase 경유 — Adapter→QueryPort 직접 호출(Bypass) 금지.

## A-3: Output Port 3분할
| Port 유형 | 역할 | 반환 타입 |
|-----------|------|----------|
| `Load{Subject}Port` | Aggregate 로딩 | `Optional<{Subject}>` |
| `Save{Subject}Port` | Aggregate 저장 + 이벤트 수거 | `void` |
| `{Subject}QueryPort` | DTO 직접 프로젝션 (Query Side) | `Optional<{Subject}Result>` |

단일 `JpaOutPort` 같은 통합 포트 금지. 관심사별 분리.

## A-4: @Transactional 금지
Application 모듈에 `@Transactional`, `@Service`, `@Component` 어노테이션 금지.
TX는 Configuration 모듈의 프록시가 담당.

## A-5: 비즈니스 로직 금지
Application Service는 오케스트레이션만 수행 (4역할):
1. Input 변환: 원시 타입 → Domain VO
2. Query: 의존 Aggregate 로드 (필요 시)
3. Delegation: Aggregate 행위 호출
4. Saving: savePort.save()

if/else 비즈니스 판단은 Domain의 책임.

## A-6: EventPublisher 직접 호출 금지
이벤트 발행은 SavePort 구현체(Adapter)가 `pullDomainEvents()` 수거 후 처리.

## A-7: Command/Query 필드 = 원시 타입만
Domain VO 없음. `String`, `long`, `int`, `UUID` 등.

## A-8: UseCase→UseCase 직접 호출 금지
UseCase 간 통신은 이벤트 기반.

## A-9: 1 TX = 1 Aggregate
하나의 UseCase에서 복수 Aggregate 변경 금지. Multi-Aggregate → 이벤트 기반 분리.

## A-10: DTO = `record` 강제
Command, Query, Result 모두 `record`. Compact Constructor Self-Validation.

## Application Service 4역할 템플릿
```java
public class {Verb}{Subject}Service implements {Verb}{Subject}UseCase {
    private final Save{Subject}Port savePort;
    private final Load{Subject}Port loadPort;
    private final Clock clock;

    @Override
    public {Subject}Result execute({Verb}{Subject}Command cmd) {
        // [1] Input 변환: 원시 타입 → Domain VO
        var name = new UserName(cmd.name());

        // [2] Query: 의존 Aggregate 로드 (필요 시)
        // var existing = loadPort.findById(id);

        // [3] Delegation: Aggregate 행위 호출
        var entity = {Subject}.create(name, clock.instant());

        // [4] Saving: SavePort가 이벤트 수거 + Outbox
        savePort.save(entity);

        return new {Subject}Result(entity.id().value().toString());
    }
}
```

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
