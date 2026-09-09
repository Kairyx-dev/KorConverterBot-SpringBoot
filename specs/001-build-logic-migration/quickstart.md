# Quickstart: 전환 검증 절차

**Date**: 2026-09-08 | **Plan**: [plan.md](./plan.md)

이 전환의 검증은 단위 테스트가 아니라 **전후 비교**다. 아래 V-0 ~ V-6을 순서대로 수행한다. V-0을 건너뛰면 이후 모든 검증이 무의미해진다.

## 사전 조건

- JDK 25 (`java -version` → 25.x). 이 저장소는 toolchain·CI·배포 이미지가 모두 25다
- Gradle wrapper 9.7.1 (`gradle/wrapper/gradle-wrapper.properties`)
- 네트워크 접근 — `build-logic`이 최초 1회 Plugin Portal에서 typesafe-conventions와 plugin marker를 받는다
- 미변경 상태의 `main` 워크트리 (V-0 기준선 캡처용)

---

## V-0. 기준선 캡처 — **반드시 전환 작업 전에**

미변경 워크트리에서 42건(6모듈 × 7축)의 의존성 보고서를 스크래치패드에 덤프한다.

```bash
BASE="$SCRATCH/baseline"   # 저장소 밖. 커밋하지 않는다
mkdir -p "$BASE"

MODULES="domain application adapter-bot adapter-persistence configuration boot"
AXES="compileClasspath runtimeClasspath testCompileClasspath testRuntimeClasspath \
      annotationProcessor testAnnotationProcessor errorprone"

for m in $MODULES; do
  for a in $AXES; do
    ./gradlew ":$m:dependencies" --configuration "$a" --console=plain -q \
      > "$BASE/$m.$a.txt" 2>&1
  done
done
```

**기대**: `$BASE`에 42개 파일. 각 파일은 해당 configuration의 의존성 트리를 담는다.

**주의**: `--configuration`으로 지정한 이름이 그 모듈에 없으면 Gradle이 오류를 남긴다. 그 경우도 파일로 기록되며, **전환 후에도 동일한 오류가 기록되면 diff는 통과한다** — 축의 존재 여부 자체가 비교 대상이므로 의도된 동작이다.

---

## V-1. build-logic이 조립되는가

```bash
./gradlew help --console=plain
```

**기대**: `BUILD SUCCESSFUL`. 로그에 `:build-logic:conventions:compileKotlin`, `:build-logic:conventions:jar`, `generatePrecompiledScriptPluginAccessors`가 나타난다.

**실패 시**
| 증상 | 원인 | 조치 |
|---|---|---|
| `Unresolved reference 'springBoot'` (또는 `jib`, `spotless`) | 해당 확장을 제공하는 플러그인이 그 스크립트 자신의 `plugins {}`에 없음 | 그 스크립트의 `plugins {}`에 선언 추가 (contracts C-4 주의 참조) |
| `Unresolved reference 'libs'` | typesafe-conventions 미적용 또는 `build-logic`을 단독 구동 중 | `build-logic/settings.gradle.kts`의 plugins 블록 확인. 최상위 빌드에서 구동 |
| plugin marker 해석 실패 | `build-logic/conventions/build.gradle.kts`에 `repositories { gradlePluginPortal() }` 누락 | 추가 |

---

## V-2. 의존성 그래프 동등성 — **핵심 게이트 (SC-001)**

전환 후 동일한 덤프를 뜨고 비교한다.

```bash
AFTER="$SCRATCH/after"
mkdir -p "$AFTER"
# V-0과 동일한 루프를 $AFTER 로 실행

diff -r "$BASE" "$AFTER" && echo "PASS: 42건 전부 동일" || echo "FAIL: 위 차이 확인"
```

**기대**: `diff` 출력 없음, 종료 코드 0.

**차이가 나면**: 그 모듈/축의 파일 하나를 직접 열어 어느 의존성이 늘거나 줄었는지 본다. 대부분 [data-model.md](./data-model.md)의 E2~E5 인벤토리에서 한 줄을 빠뜨린 경우다.

---

## V-3. 빌드 및 실행 아카이브 (SC-003)

```bash
./gradlew clean build --console=plain
```

**기대**
- `BUILD SUCCESSFUL`
- `:boot:bootJar` 실행됨, `korConverter/boot/build/libs/`에 실행 아카이브 생성
- `:configuration:bootJar`, `:adapter-bot:bootJar`, `:adapter-persistence:bootJar` → **`SKIPPED`**, 산출물 없음

```bash
ls korConverter/boot/build/libs/
ls korConverter/configuration/build/libs/ 2>&1   # 없거나 bootJar 산출물이 없어야 한다
```

---

## V-4. 포맷 검사 대상 집합 동등성 (FR-012)

`spotlessCheck`가 전환 전과 **같은 파일 집합**을 검사하는지 확인한다. jOOQ 생성 소스가 새로 포함되면 안 된다 ([research.md](./research.md) R-9).

```bash
./gradlew spotlessCheck --console=plain -i 2>&1 | grep -ci "generated"
```

**기대**: 생성 디렉터리 경로가 검사 대상으로 등장하지 않는다. `:adapter-persistence`의 `build/generated/sources/jooq` 이하 파일이 하나도 잡히지 않아야 한다.

**실패 시**: `java-conventions`의 spotless 블록에 `targetExclude("**/build/**", "**/generated/**")` 추가.

---

## V-5. CI 게이트 재현 (SC-002)

CI 워크플로우에 적힌 명령을 로컬에서 그대로 실행한다. **명령어가 바뀌지 않았다는 것 자체가 SC-004의 검증이다.**

```bash
./gradlew spotlessCheck checkstyleMain compileJava    # Gate 1
./gradlew :domain:test :application:test :boot:test   # Gate 2
./gradlew :adapter-persistence:test                   # Gate 3 (Docker 필요 — Testcontainers)
./gradlew :domain:pitest                              # Gate 4
```

**기대**: 4개 모두 성공. Gate 5는 플레이스홀더라 실행 대상이 아니다.

**Gate 3 주의**: Testcontainers가 Docker 데몬을 요구한다. 로컬에 없으면 이 게이트만 CI에 위임한다.

---

## V-6. 컨테이너 이미지 산출 (SC-003)

```bash
./gradlew :boot:jibBuildTar --console=plain
ls korConverter/boot/build/jib-image.tar
```

**기대**: `jib-image.tar` 생성. CD 워크플로우가 이 경로를 SCP로 전송하므로 **경로가 바뀌면 배포가 깨진다**.

---

## 최종 확인 체크

| 항목 | 확인 방법 | 대응 기준 |
|---|---|---|
| 라벨 파일 0개 | `find korConverter -name gradle.properties` → 결과 없음 | SC-005 |
| 의존성 diff 0건 | V-2 | SC-001 |
| CI 게이트 통과 | V-5 + PR CI | SC-002 |
| CI/훅 명령어 무변경 | `git diff main -- .github/workflows lefthook.yml` → 빈 diff | SC-004 |
| 명시 선언 6/6 | 각 모듈 `build.gradle.kts`에 `id("...-conventions")` 존재, 암묵 적용 경로 0건 | SC-006 |
| 버전 리터럴 중복 0건 | `build-logic/` 리터럴 버전 ↔ `libs.versions.toml` 대조. 허용 예외는 typesafe-conventions 1건 | SC-007 |
| jvmTarget 명시 고정 | `build-logic/conventions/build.gradle.kts`에 toolchain 선언 1곳 | SC-010 |
| ADR 작성 | `docs/decisions/0004-*.md` 존재 + `index.md`에 행 추가 | SC-009 |
| 하자 미혼입 | `git diff main` 에 J-6(NullAway 패키지) · J-8(JaCoCo) 의미 변경 없음 | SC-008 |
| 후속 이슈 등록 | GitHub Issues 4건 (`gh issue list`) | SC-008 |
