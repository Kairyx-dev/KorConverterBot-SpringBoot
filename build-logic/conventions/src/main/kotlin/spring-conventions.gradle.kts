import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    // Spring Boot 플러그인은 제거할 수 없다 — BOM 공급원이다.
    // libs.versions.toml 의 springframework-boot-starter-* 항목이 버전 없이 선언되어 있고
    // 그 버전이 이 플러그인의 BOM 에서 온다. 빼면 의존성 해석이 실패한다.
    // specs/001-build-logic-migration/data-model.md INV-4 참조
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.springframework.boot.starter)
    implementation(libs.springframework.boot.starter.web)
    implementation(libs.springframework.boot.starter.opentelemetry)
    implementation(libs.springframework.boot.starter.validation)
    implementation(libs.springframework.boot.starter.actuator)
    implementation(libs.springframework.boot.starter.json)

    testImplementation(libs.springframework.boot.starter.test)
}

tasks.withType<BootJar>().configureEach {
    enabled = false
}
