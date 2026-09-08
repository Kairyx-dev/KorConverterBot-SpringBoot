import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    // Spring Boot 플러그인 재선언은 의도된 것이다. precompiled script plugin 의
    // 타입 접근자(springBoot {})는 자기 plugins {} 블록에 선언된 플러그인에서만 생성되므로,
    // spring-conventions 가 이미 적용했더라도 여기서 빼면
    // Unresolved reference 'springBoot' 로 컴파일이 실패한다. 적용 자체는 멱등이다.
    // 중복으로 보인다는 이유로 제거하지 말 것 — specs/001-build-logic-migration/research.md R-3 참조
    // 이 묶음은 spring-conventions 뒤에 선언해야 한다 — BootJar.enabled 가 spring(false) → boot(true)
    // configureEach 순서로 결정되며, 순서가 바뀌면 bootJar 가 조용히 SKIPPED 된다.
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.com.google.cloud.tools.jib)
}

tasks.withType<BootJar>().configureEach {
    enabled = true
}

springBoot {
    buildInfo()
}

jib {
    from {
        image = "amazoncorretto:25.0.1-alpine"
    }

    to {
        image = "kor-bot-spring"
        tags = setOf("${project.version}")
    }

    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
        jvmFlags = listOf(
            "-Dspring.config.location=file:./cfg/application.yml",
            "-Dlogging.config=file:./cfg/logback-spring.xml"
        )
        workingDirectory = "/app"
    }
}
