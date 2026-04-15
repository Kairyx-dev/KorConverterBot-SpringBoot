# Stage 3 Cleanup (V005): updated_at drop + hibernate_sequence drop + index rename

- **날짜**: 2026-04-15
- **상태**: Draft
- **관련 브랜치**: `feature/purist-ddd-migration`
- **선행**: Stage 1 (V003) + Stage 2 (V004) 완료
  - `docs/superpowers/specs/2026-04-15-ignore-user-version-migration-design.md`
  - `docs/superpowers/specs/2026-04-15-message-log-stage2-migration-design.md`
- **선행 커밋**: `ed502c2` (Stage 2 마지막)
- **관련 규칙**: `.claude/rules/adapter.md` (Flyway manages DDL migrations)

## 1. 배경

Stage 1과 Stage 2가 완료되면서 `ignore_user`/`message_log` 스키마는 타깃에 거의 도달했다. 남은 정리 작업 세 건은 Stage 1 스펙 §5와 Stage 2 스펙 §5 로드맵에 명시되었다.

### 1.1 남은 정리 작업

| 항목 | 현재 상태 (post-V4) | 타깃 | 이유 |
|------|---------------------|------|------|
| `message_log.updated_at` | 존재 (TIMESTAMPTZ) | 제거 | append-only 로그에 무의미. JPA `BaseTimeEntity` 상속 산물 |
| `hibernate_sequence` | 운영에 존재 가능 | 제거 | 옛 JPA `@GeneratedValue(SEQUENCE)` 공용 시퀀스. V003에서 `ignore_user`/`message_log`를 IDENTITY로 전환한 뒤 미사용 |
| 인덱스 `idx_ignore_user_user_id` | 존재 | `idx_ignore_user_lookup`으로 rename | JPA `@Index(name = ...)` 자동 생성명. 의미 기반 네이밍으로 정리 |

### 1.2 사전 검증 결과

- `MESSAGE_LOG.UPDATED_AT` / `MessageLog.UPDATED_AT` — Java 코드(프로덕션/테스트) 전 영역에서 **0건 참조** (grep 확인)
- `hibernate_sequence` — Java 코드 및 Flyway 외 리소스에서 **0건 참조**
- `idx_ignore_user_user_id` — V001 마이그레이션 파일 외 **0건 참조** (인덱스는 PG 옵티마이저 구조)

모든 변경은 **"무참조 제거"** 또는 **"카탈로그 메타데이터 수정"**이다.

## 2. 목표

- `message_log.updated_at` 컬럼 제거 (append-only 로그 정리)
- 운영에 남아있을 수 있는 legacy `hibernate_sequence` 제거 (`IF EXISTS`로 idempotent)
- JPA-artifact 인덱스명 `idx_ignore_user_user_id` → 의미 기반 `idx_ignore_user_lookup`로 rename
- Stage 1~3 시리즈를 `ignore_user`/`message_log` 타깃 스키마와 완전 일치시켜 마무리

## 3. 비목표 (YAGNI)

- Flyway baseline 설정 정리 (`baseline-on-migrate: true` 유지 정책 재검토) — 본 시리즈 범위 밖
- `MessageLog` Aggregate Root 승격 — Application 계층 재설계 영역, 본 시리즈와 무관
- 다른 테이블/인덱스 정리 — 없음

## 4. 결정

### 4.1 전략: 단일 V005 번들

V003/V004 패턴 유지. 세 변경 모두 H2에서 파싱 가능하므로 `--[jooq ignore start/stop]` 불필요. 단일 Flyway 파일 = 단일 TX로 원자 처리.

### 4.2 대안 비교

| 후보 | 판정 | 근거 |
|------|------|------|
| 단일 V005 번들 (채택) | ✅ | V003/V004 선례, 각 변경 1줄, 단일 TX 원자성 |
| 순서별 분리 (V005/V006/V007) | ❌ | 1줄짜리 3파일은 오버엔지니어링. Flyway 이력 비대화. V003/V004와 비일관 |
| ADR로 대체, 스펙 생략 | ❌ | Stage 1/2 스펙 기반 일관성 깨짐. 배포 체크리스트/롤백 SQL 참조 곤란 |

### 4.3 V005 SQL

```sql
-- V005__stage3_cleanup_updated_at_sequence_and_index.sql
-- Stage 3 cleanup — purely structural, no data transformation:
--   1) Drop message_log.updated_at (append-only log, field unused by adapter code)
--   2) Drop legacy hibernate_sequence if present (JPA @GeneratedValue artifact)
--   3) Rename idx_ignore_user_user_id to idx_ignore_user_lookup (semantic name)
-- Adapter code and tests are unaffected — verified via grep for MESSAGE_LOG.UPDATED_AT
-- (zero Java references) and index-by-name usage (none; indexes are optimizer-only).

-- 1) Drop the append-only log's unused updated_at column.
--    jOOQ will regenerate MessageLog without UPDATED_AT; no Java code references it.
ALTER TABLE message_log DROP COLUMN updated_at;

-- 2) Drop the legacy JPA shared sequence (if it exists in the deployment).
--    Fresh environments never created it; production may have it from pre-V001 JPA.
--    IF EXISTS makes this idempotent across environments.
DROP SEQUENCE IF EXISTS hibernate_sequence;

-- 3) Rename JPA-artifact index to semantic name.
ALTER INDEX idx_ignore_user_user_id RENAME TO idx_ignore_user_lookup;
```

### 4.4 Flyway 설정

**변경 없음.** `baseline-on-migrate: true`, `baseline-version: 2` 설정이 운영에서 V5 자동 실행을 지원. 신규 환경은 V1→V5 순차 실행.

### 4.5 jOOQ codegen 영향

| 항목 | Stage 2 후 | Stage 3 후 | 영향 |
|------|-----------|-----------|------|
| `MessageLog.UPDATED_AT` | `TableField<MessageLogRecord, OffsetDateTime>` | **필드 제거** | 참조 코드 없음 → 컴파일 영향 없음 |
| `MessageLogRecord.getUpdatedAt()/setUpdatedAt()` | 존재 | **메서드 제거** | 동일 |
| `Indexes.java` 인덱스 객체 이름 | `IDX_IGNORE_USER_USER_ID` | `IDX_IGNORE_USER_LOOKUP` | Java 코드에서 인덱스 객체 미참조 → 영향 없음 |

### 4.6 어댑터 코드 영향

| 파일 | 변경 여부 | 근거 |
|------|-----------|------|
| `MessageLogRecordAdapter` | **변경 없음** | `updated_at` 미참조 |
| `MessageLogRecordAdapterTest` | **변경 없음** | 비즈니스 필드만 assert |
| `IgnoreUserPersistenceAdapter` | **변경 없음** | 인덱스는 옵티마이저 구조체 |
| `IgnoreUserQueryAdapter` | **변경 없음** | 동일 |
| 기타 Java 파일 | **변경 없음** | |

## 5. 후속 로드맵

Stage 3가 본 시리즈의 마지막 단계. 추가 계획 없음.

향후 별도 이니셔티브로 고려할 수 있는 사항(본 시리즈와 독립):
- `MessageLog`의 Aggregate Root 승격 — 현재는 append-only 로그로 Command/QueryPort 분리 부재. Application 계층 재설계 필요
- Flyway baseline 정리 — 운영에 baseline row가 기록된 이후 `baseline-on-migrate` 설정 유지 여부 재검토

## 6. 롤백 전략

| 시나리오 | 대응 |
|----------|------|
| V005 실행 중 실패 | Flyway 파일 단위 TX → 전체 롤백. 운영 상태 무변화 |
| V005 적용 후 버그 발견 | 수동 역마이그레이션 SQL + `flyway_schema_history` V5 row 삭제 |
| `updated_at` 데이터 복구 필요 | 삭제된 row는 복구 불가. Stage 3 배포 전 **`pg_dump` 백업이 유일한 복구 경로** |

**수동 역마이그레이션 SQL** (runbook only, 저장소 비커밋):

```sql
-- 인덱스 역개명
ALTER INDEX idx_ignore_user_lookup RENAME TO idx_ignore_user_user_id;

-- hibernate_sequence 복원 (JPA 동작이 필요한 경우에만)
-- CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START 1 INCREMENT 1;

-- message_log.updated_at 재생성 (빈 컬럼. 기존 데이터는 백업에서 복원)
ALTER TABLE message_log
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Flyway 이력 정리
DELETE FROM flyway_schema_history WHERE version = '5';
```

## 7. 테스트 전략

### 7.1 신규 테스트 `Stage3CleanupV005MigrationIT`

파일: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/Stage3CleanupV005MigrationIT.java`

`LegacyToTargetV003MigrationIT`/`MessageLogV004MigrationIT`와 동일 구조 (isolated `PostgreSQLContainer`, `@TestInstance(PER_CLASS)`, 두 단계 Flyway, raw JDBC).

**절차:**

```
1. Testcontainers Postgres 17 기동
2. Flyway.target("4").migrate()  — V1/V2/V3/V4 실행 (Stage 2 완료 상태)
3. raw JDBC:
   - sanity: updated_at 컬럼이 존재함을 사전 확인 (information_schema.columns)
   - 더미 row INSERT (id=500, short message, channel_id=555)
4. Flyway.target("5").migrate()
5. Assert (raw JDBC + information_schema / pg_indexes / pg_sequences):
   5a. updated_at 컬럼 부재:
       SELECT COUNT(*) FROM information_schema.columns
         WHERE table_name='message_log' AND column_name='updated_at' -> 0
   5b. 기존 row (id=500) 생존: SELECT COUNT(*) FROM message_log WHERE id = 500
       -> 1 (DROP COLUMN은 해당 컬럼만 제거하고 row는 보존)
   5c. idx_ignore_user_lookup 존재 + idx_ignore_user_user_id 미존재:
       SELECT indexname FROM pg_indexes WHERE tablename='ignore_user'
   5d. hibernate_sequence 미존재:
       SELECT COUNT(*) FROM pg_sequences WHERE sequencename='hibernate_sequence' -> 0
   5e. 새 INSERT (updated_at 미지정) 성공:
       INSERT INTO message_log (guild, channel, ..., channel_id) VALUES (...)
       -> 실패하지 않음 (DROP COLUMN이 INSERT 경로에 무영향임을 확인)
```

### 7.2 회귀 검증

| 테스트 | 영향 여부 | 근거 |
|--------|-----------|------|
| `IgnoreUserPersistenceAdapterTest` | 무 | 인덱스명 변경은 쿼리에 투명 |
| `IgnoreUserQueryAdapterTest` | 무 | 동일 |
| `MessageLogRecordAdapterTest` | 무 | `updated_at` 미참조. `dsl.selectFrom(MESSAGE_LOG).fetch()`는 존재 컬럼만 반환 |
| `LegacyToTargetV003MigrationIT` | 무 | `target("3")`까지만 |
| `MessageLogV004MigrationIT` | 무 | `target("4")`까지만. Step 2의 INSERT는 target=3 상태(updated_at 존재)에서 실행되므로 정상 |
| `IgnoreUserE2ETest` | 무 | message_log 미관여 |

### 7.3 전체 빌드

`./gradlew build`로 Checkstyle, Spotless, ErrorProne/NullAway, jOOQ codegen, 전 모듈 테스트 통과 확인.

## 8. 운영 배포 체크리스트

1. 사전: `pg_dump --table=message_log` 논리 백업 (updated_at 데이터 보존)
2. 사전: `SELECT COUNT(*) FROM pg_sequences WHERE sequencename = 'hibernate_sequence'` 로 시퀀스 존재 여부 기록 (post-deploy 검증용)
3. 사전: `SELECT indexname FROM pg_indexes WHERE tablename = 'ignore_user'` 로 현재 인덱스명 기록
4. 배포: 봇 컨테이너 중지 → 신 버전 기동 → Flyway V5 자동 실행
5. 사후: `flyway_schema_history`에 V5 success row 확인
6. 사후: `updated_at` 컬럼 제거 확인 (`\d message_log` or `information_schema.columns`)
7. 사후: `idx_ignore_user_lookup` 존재 확인 (`\d ignore_user`)
8. 사후: Discord 메시지 변환/IgnoreUser 추가 smoke test

## 9. 영향 범위

| 영역 | 영향 | 변경 |
|------|------|------|
| `adapter-persistence/src/main/resources/db/migration/V005*.sql` | **신규** | forward migration |
| `adapter-persistence/src/test/java/**/migration/Stage3CleanupV005MigrationIT.java` | **신규** | 마이그레이션 회귀 테스트 |
| `adapter-persistence/build/generated/sources/jooq/**` | 재생성 (자동) | `MessageLog.UPDATED_AT` 제거, 인덱스 객체 이름 변경 |
| `runtime/cfg/application.yml` | **변경 없음** | |
| `adapter-persistence/src/main/java/**/*.java` | **변경 없음** | |
| `adapter-persistence/src/test/java/**` (기존 테스트) | **변경 없음** | |

## 10. 참고

- Stage 1 스펙: `docs/superpowers/specs/2026-04-15-ignore-user-version-migration-design.md`
- Stage 2 스펙: `docs/superpowers/specs/2026-04-15-message-log-stage2-migration-design.md`
- PostgreSQL ALTER TABLE: <https://www.postgresql.org/docs/current/sql-altertable.html>
- PostgreSQL ALTER INDEX: <https://www.postgresql.org/docs/current/sql-alterindex.html>
- PostgreSQL DROP SEQUENCE: <https://www.postgresql.org/docs/current/sql-dropsequence.html>
