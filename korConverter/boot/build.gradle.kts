dependencies {
    implementation(project(":configuration"))
    implementation(project(":adapter-bot"))
    implementation(project(":adapter-persistence"))
    implementation(project(":application"))
    implementation(project(":domain"))

    // OTel Logback appender bridges Logback events → OTel LogRecord → OTLP → Loki.
    // Declared here (not in configureByLabel("spring")) because only the boot module
    // loads logback-spring.xml at application startup. Referenced as <appender> in
    // runtime/cfg/logback-spring.xml.
    implementation(libs.opentelemetry.logback.appender)

    testImplementation(libs.springframework.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgresql)
}
