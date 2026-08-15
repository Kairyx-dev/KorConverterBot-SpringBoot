# KorConverterBot-SpringBoot

## 프로젝트 개요
Discord 영타 -> 한글 변환 봇. Java 25 + Spring Boot 4 기반.

## 아키텍처
순수주의 DDD + 헥사고날 아키텍처. CQRS Level 1 (단일 DB).
AutoConfiguration per module (not @ComponentScan).

## 모듈 구조 (6 modules)
- `domain`: 순수 Java. 외부 의존 제로. Aggregate, VO, Domain Event, Domain Service.
- `application`: domain만 의존. Input Port(UseCase), Output Port(Load/Save/Query), Command/Query, Service.
- `adapter-bot`: application만 의존. Discord JDA Listener (Inbound Adapter).
- `adapter-persistence`: application+domain 의존. jOOQ + Flyway Persistence (Outbound Adapter).
- `configuration`: 전체 조립. Bean 등록, TX 프록시.
- `boot`: Spring Boot 진입점. @SpringBootApplication.

## 기술 스택
- Java 25, Spring Boot 4.0.x, Gradle 9.x (linecorp build-recipe-plugin)
- JDA (Discord API), jOOQ + Flyway, PostgreSQL
- ErrorProne + NullAway (정적 분석), JSpecify (Adapter/Configuration에서만)
- Testcontainers, ArchUnit, jqwik (PBT)
- JaCoCo, Checkstyle, Lefthook
- PIT mutation testing (domain 65% threshold)
- Spotless (palantir-java-format 2.97.0 — 4-space indent, 120 column. ADR-0003)

## 규칙
`.claude/rules/index.md` 참조.

## 결정 기록
`docs/decisions/index.md` 참조.

## 워크플로우
- Inside-Out 순서: Domain -> Application -> Adapter -> Configuration -> DDL
- 중요한 결정 발생 시 ADR + rule 자동 작성
- 코드 생성 후 `.claude/rules/validation.md` 자기검증 실행
- 빌드 검증: `./gradlew build`

## 빌드 & 실행
```bash
./gradlew build          # 전체 빌드
./gradlew test           # 테스트
./gradlew bootRun        # 로컬 실행 (환경변수 필요)
./gradlew jibDockerBuild # Docker 이미지 빌드
./gradlew checkstyleMain # Checkstyle 검사
./gradlew jacocoTestReport # JaCoCo 커버리지 리포트
```

## 환경 변수
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER_NAME`, `DB_PASSWORD`
- `DISCORD_BOT_TOKEN`

## 빌드 추가 명령어
```bash
./gradlew spotlessApply   # 코드 포맷팅 적용
./gradlew spotlessCheck   # 코드 포맷팅 검증
./gradlew :domain:pitest  # PIT 뮤테이션 테스트
```

## Agent skills

### Issue tracker
GitHub Issues (`Kairyx-dev/KorConverterBot-SpringBoot`), `gh` CLI 사용. `docs/agents/issue-tracker.md` 참조.

### Triage labels
canonical 5종 라벨(`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`)을 그대로 사용. `docs/agents/triage-labels.md` 참조.

### Domain docs
Single-context. ADR은 `docs/adr/`가 아니라 `docs/decisions/`. `docs/agents/domain.md` 참조.
