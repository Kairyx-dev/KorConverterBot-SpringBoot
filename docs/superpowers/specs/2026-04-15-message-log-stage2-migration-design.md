# message_log Stage 2 정렬 (V004): TEXT + DEFAULT 제거 + TIMESTAMPTZ

- **날짜**: 2026-04-15
- **상태**: Draft
- **관련 브랜치**: `feature/purist-ddd-migration`
- **선행**: Stage 1 (V003) 완료 — `docs/superpowers/specs/2026-04-15-ignore-user-version-migration-design.md`
- **선행 커밋**: `22366af` (Stage 1 전체)
- **관련 규칙**: `.claude/rules/adapter.md` (Flyway manages DDL migrations)

## 1. 배경

Stage 1(V003)은 `ignore_user`/`message_log`를 legacy 운영 스키마에서 일부 타깃 형태로 전진시켰다. 특히 `message_log`는 배포 블로커였던 `id` IDENTITY 전환만 수행했고, 나머지 정렬은 Stage 2로 분리되었다(Stage 1 스펙 §5).

### 1.1 Stage 2 남은 정렬 항목

| 항목 | V003 이후 상태 | Stage 2 목표 | 원인 |
|------|---------------|--------------|------|
| `message_log.message` | `VARCHAR(255)` | `TEXT` | Discord 메시지 최대 2000자 — 데이터 잘림 리스크 제거 |
| `message_log.converted_message` | `VARCHAR(255)` | `TEXT` | 동일 |
| `message_log.channel_id` | `BIGINT DEFAULT 0` | `BIGINT` (nullable) | JPA `@ColumnDefault("0")` 산물. 비즈니스 의미 없음 |
| `message_log.created_at` | `TIMESTAMP(6) WITHOUT TIME ZONE` | `TIMESTAMPTZ` | 도메인 `Instant` 모델과 정합 (V003의 `ignore_user`와 동일 이유) |
| `message_log.updated_at` | 동일 | `TIMESTAMPTZ` | 동일 |

### 1.2 배포 블로커가 아닌 이유

V003과 달리 본 스펙의 변경들은 **배포 블로커가 아니다**:
- `message`/`converted_message` VARCHAR(255)로도 어댑터가 동작 중 (긴 메시지 잘림이 발생하되 INSERT 자체는 성공)
- `channel_id DEFAULT 0`는 어댑터가 명시 값 설정하여 무영향
- 타임스탬프는 어댑터가 전혀 참조하지 않음 (`MessageLogRecordAdapter` `.set()` 호출 7건 중 타임스탬프 없음)

따라서 본 스펙은 **순수 개선**이며 긴급도는 낮다.

## 2. 목표

- `message_log.message`/`converted_message`를 `TEXT`로 확대하여 Discord 메시지 잘림 방지
- `message_log.channel_id`의 무의미한 `DEFAULT 0` 제거
- `message_log.created_at`/`updated_at`를 `TIMESTAMPTZ`로 전환하여 도메인 `Instant` 모델과 정합 (V003 `ignore_user`와 동일 패턴)

## 3. 비목표 (YAGNI)

- `message_log.updated_at` 제거 — Stage 3
- `hibernate_sequence` 정리 — Stage 3
- 인덱스 개명 — Stage 3
- `RecordMessageLogCommand`에 VO 도입 — Application 계층 변경 범위 밖
- Zero-downtime 배포 — Discord 봇 재시작 수십 초 허용

## 4. 결정

### 4.1 전략: 단일 V004에 4개 변경 번들

V003이 "배포 블로커 해소 + 스키마 정렬"을 한 파일로 묶은 것과 동일 패턴. 리뷰 단위/롤백 단위 일관.

### 4.2 대안 비교

| 후보 | 판정 | 근거 |
|------|------|------|
| 단일 V004 번들 (채택) | ✅ | V003 패턴 재사용, 리뷰·롤백 단위 명확 |
| 변경 유형별 분리 (V004/V005/V006) | ❌ | 대부분 마이그레이션이 10 lines 미만 — 오버엔지니어링. 실패 시 중간 상태 수습 번거로움 |
| 락 특성별 분리 (메타데이터 vs 테이블 재작성) | ❌ | `message_log`가 소규모라 분리 이점 없음 |

### 4.3 V004 SQL

```sql
-- V004__message_log_text_default_and_timestamptz.sql
-- message_log Stage 2 정렬:
--   - message / converted_message: VARCHAR(255) -> TEXT (Discord 2000자 대응)
--   - channel_id DEFAULT 0 제거
--   - created_at / updated_at TIMESTAMPTZ 전환 (KST 해석)

-- 1) TEXT 확대 (단순 ALTER TYPE은 H2가 파싱 가능하므로 jooq-ignore 불필요)
ALTER TABLE message_log ALTER COLUMN message TYPE TEXT;
ALTER TABLE message_log ALTER COLUMN converted_message TYPE TEXT;

-- 2) channel_id DEFAULT 0 제거
ALTER TABLE message_log ALTER COLUMN channel_id DROP DEFAULT;

-- 3) TIMESTAMPTZ 전환 (V003과 동일 패턴: 컨테이너 TZ=Asia/Seoul 근거)
--    USING 절은 PostgreSQL 전용 -> jooq-ignore로 감싸고,
--    밖에 TYPE-only ALTER를 중복 배치하여 jOOQ DDLDatabase가 최종 타입을 추론하도록 함.
--[jooq ignore start]
ALTER TABLE message_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN updated_at SET DEFAULT now();
--[jooq ignore stop]
-- jOOQ DDLDatabase hints (Flyway also executes these as no-ops in PostgreSQL
-- because the USING-form above already performed the conversion).
ALTER TABLE message_log ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE message_log ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE message_log ALTER COLUMN updated_at TYPE TIMESTAMPTZ;
ALTER TABLE message_log ALTER COLUMN updated_at SET DEFAULT now();
```

### 4.4 Flyway 설정

**변경 없음.** 기존 `runtime/cfg/application.yml`의 `baseline-on-migrate: true`, `baseline-version: 2` 설정으로 충분:
- 운영: V1/V2 baseline됨. V3 실행됨. V4가 다음 차례로 자동 실행
- Fresh env: V1→V2→V3→V4 순차 실행

### 4.5 jOOQ codegen 영향

| 필드 | Stage 1 후 | Stage 2 후 | Java 타입 변화 |
|------|-----------|-----------|---------------|
| `MESSAGE` | `VARCHAR(255)` | `CLOB`/`TEXT` | 모두 `String` — **변화 없음** |
| `CONVERTED_MESSAGE` | 동일 | 동일 | 동일 |
| `CHANNEL_ID` | `BIGINT DEFAULT 0` | `BIGINT` | `Long` 유지 (default 메타데이터만 제거) |
| `CREATED_AT` | `LOCALDATETIME(6)` | `TIMESTAMPWITHTIMEZONE(6)` | `LocalDateTime` → **`OffsetDateTime`** |
| `UPDATED_AT` | 동일 | 동일 | 동일 |

### 4.6 어댑터 코드 영향

| 파일 | 변경 여부 | 근거 |
|------|-----------|------|
| `MessageLogRecordAdapter` | **변경 없음** | `.set()` 호출 7건 모두 타임스탬프 비포함. `message`/`converted_message`는 `String` 유지. `channel_id`는 명시 값 전달 |
| `MessageLogRecordAdapterTest` | **변경 없음** | 짧은 메시지 + 타임스탬프 미참조 |
| `RecordMessageLogCommand` | **변경 없음** | 원시/String 타입 유지 (A-7) |
| `IgnoreUser*Adapter` / 테스트 | **변경 없음** | message_log 미접촉 |

### 4.7 기존 회귀 테스트

| 테스트 | 영향 여부 | 근거 |
|--------|-----------|------|
| `IgnoreUserPersistenceAdapterTest` | 무 | message_log 미접촉 |
| `IgnoreUserQueryAdapterTest` | 무 | 동일 |
| `MessageLogRecordAdapterTest` | 무 | 짧은 메시지 + 타임스탬프 미참조 |
| `LegacyToTargetV003MigrationIT` | 무 | `target("3")`까지만 migrate, V4 무관 |
| `IgnoreUserE2ETest` | 무 | message_log 미관여 |

## 5. 후속 단계 로드맵 (Stage 3)

Stage 1 §5에서 발췌·갱신:

- `message_log.updated_at` 제거 (append-only 로그에 무의미)
  - **주의**: jOOQ 생성 코드의 `MessageLog.UPDATED_AT` 필드가 사라지므로, 착수 전 이 필드를 참조하는 코드 여부 확인 필요 (현재 미참조 예상)
- `hibernate_sequence` (존재 시) `DROP SEQUENCE IF EXISTS`
- 인덱스 `idx_ignore_user_user_id` → `idx_ignore_user_lookup` 개명

Stage 3는 별도 스펙으로 진행.

## 6. 롤백 전략

| 시나리오 | 대응 |
|----------|------|
| V004 실행 중 실패 | Flyway 파일 단위 TX → 전체 롤백. 운영 상태 무변화 |
| V004 적용 후 버그 발견 | 수동 역마이그레이션 SQL + `flyway_schema_history` V4 row 삭제 (아래 SQL 참조) |
| TEXT 데이터 축소 시 초과 row 존재 | 롤백 전 `SELECT COUNT(*) FROM message_log WHERE LENGTH(message) > 255` audit 필요. 초과 row가 있으면 데이터 손실 없이 롤백 불가 — 백업 복원 |
| 타임존 변환 오해석 발견 | 백업 복원 (동일 KST+9h shift는 대칭적 역변환 가능하나 감정적 보수성 측면에서 백업 우선) |

**수동 역마이그레이션 SQL** (runbook only, 저장소 비커밋):

```sql
-- 타임스탬프 역변환
ALTER TABLE message_log
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITHOUT TIME ZONE
        USING updated_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITHOUT TIME ZONE
        USING created_at AT TIME ZONE 'Asia/Seoul';
-- DEFAULT now() 복원
ALTER TABLE message_log ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE message_log ALTER COLUMN updated_at SET DEFAULT now();
-- channel_id DEFAULT 복원
ALTER TABLE message_log ALTER COLUMN channel_id SET DEFAULT 0;
-- TEXT 축소 (256자 이상 row 존재 시 실패!)
ALTER TABLE message_log ALTER COLUMN converted_message TYPE VARCHAR(255);
ALTER TABLE message_log ALTER COLUMN message TYPE VARCHAR(255);
-- Flyway 이력 정리
DELETE FROM flyway_schema_history WHERE version = '4';
```

## 7. 테스트 전략

### 7.1 신규 테스트 `MessageLogV004MigrationIT`

파일: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/MessageLogV004MigrationIT.java`

`LegacyToTargetV003MigrationIT`와 동일 구조 (자체 컨테이너, PER_CLASS 수명, raw JDBC).

**절차:**
```
1. Testcontainers Postgres 17 기동
2. Flyway.target("3").migrate()  — V1/V2/V3 순차 실행 (V3 적용 후 상태)
3. raw JDBC로 legacy-shaped row 삽입:
   - id 명시 (V3 이후 IDENTITY는 명시 허용), 짧은 message
   - created_at/updated_at = TIMESTAMP '2025-12-31 23:00:00' (KST 벽시계)
4. Flyway.target("4").migrate()
5. Assert:
   5a. id=300 row의 created_at.toInstant() == Instant.parse("2025-12-31T14:00:00Z")
       (즉 KST -9h)
   5b. id=300 row의 updated_at.toInstant() == Instant.parse("2025-12-31T14:00:00Z")
   5c. 2000자 message 새 INSERT 성공 (TEXT 확대 검증)
   5d. channel_id 미지정 INSERT → 조회 시 channel_id IS NULL
       (DEFAULT 0 제거 검증)
```

### 7.2 회귀 검증

| 계층 | 검증 | 도구 |
|------|------|------|
| 전체 | `./gradlew build` 통과 | 기존 파이프라인 |
| 어댑터 | 기존 테스트 통과 (위 표 참조) | 기존 테스트 |
| jOOQ codegen | `./gradlew :adapter-persistence:generateJooq` 성공 + diff 확인 | 빌드 검증 |

## 8. 운영 배포 체크리스트

1. 사전: 운영 DB 논리 백업 (`pg_dump --table=message_log`)
2. 사전: 운영 `message_log` row 개수 확인 — 타임스탬프 변환은 AccessExclusiveLock + 테이블 재작성을 유발. 현재 소규모 봇이라 무시 가능하지만 확인 습관화
3. 사전: `deploy/docker-compose.yml`의 `TZ: Asia/Seoul` 변경 이력 없음 확인 (V003과 동일 전제)
4. 배포: 봇 컨테이너 중지 → 신 버전 기동 → Flyway V4 자동 실행
5. 사후: `flyway_schema_history`에 V4 success row 확인
6. 사후: Discord에서 2000자 메시지 변환 smoke test (TEXT 확대 효과 검증)
7. 사후: `message_log.created_at` 샘플 SELECT로 TIMESTAMPTZ 정상 동작 확인

## 9. 영향 범위

| 영역 | 영향 | 변경 |
|------|------|------|
| `adapter-persistence/src/main/resources/db/migration/V004*.sql` | **신규** | forward migration |
| `adapter-persistence/src/test/java/**/migration/MessageLogV004MigrationIT.java` | **신규** | 마이그레이션 회귀 테스트 |
| `adapter-persistence/build/generated/sources/jooq/**` | 재생성 (자동) | `MESSAGE`/`CONVERTED_MESSAGE` TEXT, `CHANNEL_ID` default 제거, 타임스탬프 `OffsetDateTime` |
| `runtime/cfg/application.yml` | **변경 없음** | |
| `adapter-persistence/src/main/java/**/*.java` | **변경 없음** | |

## 10. 참고

- Stage 1 스펙: `docs/superpowers/specs/2026-04-15-ignore-user-version-migration-design.md`
- Stage 1 플랜: `docs/superpowers/plans/2026-04-15-ignore-user-version-migration.md`
- PostgreSQL ALTER COLUMN TYPE: <https://www.postgresql.org/docs/current/sql-altertable.html>
- jOOQ DDLDatabase `--[jooq ignore start/stop]`: 프로젝트 내 V003가 참고 예시
