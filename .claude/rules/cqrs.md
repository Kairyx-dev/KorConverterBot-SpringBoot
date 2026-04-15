# CQRS Level 1 Rules

> 단일 DB, Command/Query 클래스 분리.

## Command Side (Write)
1. Adapter → `{Verb}{Subject}Command` 생성 (원시 타입)
2. UseCase(Input Port) 호출
3. Service: Command → VO 변환 → Aggregate 행위 → SavePort.save()
4. SavePort 구현체: DB 저장 + `pullDomainEvents()` → Outbox
5. TX COMMIT

## Query Side (Read)
1. Adapter → `{Verb}{Subject}Query` 생성 (원시 타입)
2. UseCase(Input Port) 호출
3. Service: QueryPort 호출 → `{Subject}Result` (DTO) 반환
4. QueryPort 구현체: DB → DTO 직접 프로젝션 (Aggregate bypass)

## Output Port 3분할 (A-3)

| Port | 역할 | Command/Query |
|------|------|---------------|
| `Load{Subject}Port` | Aggregate 로딩 | Command Side |
| `Save{Subject}Port` | Aggregate 저장 + 이벤트 수거 | Command Side |
| `{Subject}QueryPort` | DTO 직접 프로젝션 | Query Side |

## Outbox 패턴

```
Aggregate.create()
    → registerEvent(DomainEvent)

SavePort.save(entity)
    → DB upsert (WHERE version = ?)
    → entity.pullDomainEvents()
    → eventPublisher.publishEvent() (같은 TX)

TX COMMIT
    → 이벤트 자동 발행 (Spring Modulith 또는 수동)
```

## 이벤트 진화
- 필드 추가: 기존 필드 유지 + 신규 필드 추가 (하위 호환)
- 필드 삭제: deprecated 마킹 → 다음 major에서 제거
- 타입 변경: 새 이벤트 타입 생성 + sealed permits 추가

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호(ADR-NNNN)에 해당하는 docs/decisions/ 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
