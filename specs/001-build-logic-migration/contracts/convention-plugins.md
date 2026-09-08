# Contract: Convention Plugin 인터페이스

**Date**: 2026-09-08 | **Plan**: [../plan.md](../plan.md)

이 프로젝트가 노출하는 외부 인터페이스는 웹 API가 아니라 **설정 묶음이 모듈에게 제공하는 계약**이다. 모듈 작성자는 이 문서만 보고 어떤 묶음을 선언해야 하는지 판단할 수 있어야 한다.

---

## 계약 요약

| 설정 묶음 ID | 무엇을 적용하는가 | 무엇을 제공하는가 | 전제 조건 |
|---|---|---|---|
| `java-conventions` | java, idea, jacoco, checkstyle, errorprone, spotless | Java 25 toolchain, Lombok, 정적 분석(ErrorProne+NullAway), 커버리지, 포맷 검사, JUnit Platform 실행기 | 없음 — 모든 모듈이 선언 |
| `spring-conventions` | Spring Boot 플러그인, dependency-management | Spring BOM(버전 해석), starter 6종, starter-test, **`bootJar` 비활성** | `java-conventions`와 함께 선언 |
| `test-conventions` | (java만) | JUnit BOM/API/Params/Engine/Launcher, AssertJ | `java-conventions`와 함께 선언 |
| `boot-conventions` | Spring Boot 플러그인(재선언), jib | **`bootJar` 활성**, `buildInfo()`, 컨테이너 이미지 설정 | `spring-conventions`와 **반드시** 함께 선언 |

---

## C-1. `java-conventions`

**적용 대상**: 모든 Java 모듈 (현재 6개)

**제공**
- Java toolchain 25
- `implementation` / `annotationProcessor` / `testImplementation` / `testAnnotationProcessor`: Lombok
- `errorprone`: error_prone_core, NullAway
- `Test` 태스크: JUnit Platform
- `JacocoReport`: `test` 선행
- `JacocoCoverageVerification`: 최소 80% 규칙 정의 *(현재 `check`에 미연결 — 후속 이슈 #1)*
- `Checkstyle`: `config/checkstyle/checkstyle.xml`, 실패 시 빌드 중단, `**/generated/**` 제외
- `Spotless`: palantir-java-format, `**/build/**` · `**/generated/**` 제외

**제공하지 않는 것**: Spring 관련 일체, JUnit/AssertJ 테스트 의존성, 실행 아카이브.

**호출 예**
```kotlin
plugins {
    id("java-conventions")
}
```

---

## C-2. `spring-conventions`

**적용 대상**: Spring 컨텍스트에 참여하는 모듈 (현재 4개)

**제공**
- Spring Boot BOM — 카탈로그의 **버전 없는** `springframework-boot-starter-*` 항목이 이것으로 해석된다
- `implementation`: starter, starter-web, starter-opentelemetry, starter-validation, starter-actuator, starter-json
- `testImplementation`: starter-test
- `bootJar` 태스크 **비활성화**

**계약상 주의 1 — Spring Boot 플러그인은 제거할 수 없다**
BOM 공급원이다. 빼면 버전 없는 카탈로그 항목의 해석이 실패한다.

**계약상 주의 2 — `bootJar`는 기본 비활성이다**
이 묶음을 선언한 모듈은 `bootJar` 태스크를 갖지만 산출물을 만들지 않는다. 실행 아카이브가 필요하면 `boot-conventions`를 **추가로** 선언해야 한다.

**호출 예**
```kotlin
plugins {
    id("java-conventions")
    id("spring-conventions")
}
```

---

## C-3. `test-conventions`

**적용 대상**: 현재 `:domain` 1개

**제공**
- `testImplementation(platform(...))`: JUnit BOM
- `testImplementation`: junit-jupiter-api, junit-jupiter-params, assertj
- `testRuntimeOnly`: junit-jupiter-engine, junit-platform-launcher

**계약상 주의 — 현재 유일한 소비자가 동일 의존성을 중복 선언 중이다**
`:domain/build.gradle.kts`가 같은 5개를 이미 선언한다. 선언이 동일해 그래프에는 영향이 없다. 중복 정리는 후속 이슈 #3.

**호출 예**
```kotlin
plugins {
    id("java-conventions")
    id("test-conventions")
}
```

---

## C-4. `boot-conventions`

**적용 대상**: 배포용 실행 아카이브를 만드는 모듈 (현재 `:boot` 1개)

**제공**
- `bootJar` 태스크 **활성화**
- `springBoot { buildInfo() }`
- jib: base image `amazoncorretto:25.0.1-alpine`, target `kor-bot-spring:${version}`, `USE_CURRENT_TIMESTAMP`, jvmFlags 2종, workingDirectory `/app`

**전제 조건**: `spring-conventions`와 함께 선언해야 한다. 단독 선언은 지원 대상이 아니다 — `bootJar`를 켜지만 Spring 런타임 의존성이 없는 아카이브가 나온다.

**계약상 주의 — Spring Boot 플러그인 재선언은 의도된 것이다**
이 묶음의 `plugins {}` 블록은 `spring-conventions`가 이미 적용한 Spring Boot 플러그인을 다시 선언한다. precompiled script plugin의 타입 접근자(`springBoot {}`)가 자기 `plugins {}` 블록 기준으로만 생성되기 때문이다. 빼면 컴파일이 실패한다. 적용 자체는 멱등이라 런타임 영향은 없다. **중복으로 보인다는 이유로 제거하지 말 것.**

**호출 예**
```kotlin
plugins {
    id("java-conventions")
    id("spring-conventions")
    id("boot-conventions")
}
```

---

## C-5. 계약 밖 — 모듈 고유 설정

다음은 설정 묶음이 제공하지 않으며, 필요한 모듈이 자기 스크립트에 직접 선언한다. 이번 전환에서 **변경하지 않는다**.

| 모듈 | 고유 설정 |
|---|---|
| `:domain` | `alias(libs.plugins.pitest)` + `pitest { targetClasses, targetTests, mutationThreshold=65, ... }` |
| `:adapter-persistence` | `alias(libs.plugins.jooq.codegen)` + `jooq { ... }` + `sourceSets.main.java.srcDir(jooqGeneratedDir)` + `compileJava dependsOn jooqCodegen` |
| 전 모듈 | 모듈 간 `implementation(project(":..."))` 및 모듈 전용 라이브러리 의존성 |

---

## C-6. 운용 제약

| 제약 | 내용 |
|---|---|
| 단독 구동 금지 | `gradle -p build-logic <task>`는 실패한다. `build-logic`은 최상위 빌드를 통해서만 구동한다 |
| Isolated Projects 금지 | `org.gradle.isolated-projects=true`는 이 구조에서 실패한다 (typesafe-conventions #186) |
| jvmTarget 고정 | `build-logic/conventions`의 toolchain은 25로 명시 고정한다. 데몬 JVM 추종을 허용하면 낮은 JVM에서 `UnsupportedClassVersionError`가 조용히 발생한다 |
| 신규 모듈 추가 시 | 새 모듈은 `plugins {}`에 필요한 묶음을 **명시 선언**해야 한다. 라벨 방식과 달리 자동 적용은 없다 |
