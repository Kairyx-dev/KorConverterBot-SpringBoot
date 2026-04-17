dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(project(":adapter-persistence"))
    implementation(project(":adapter-bot"))

    implementation(libs.springframework.tx)
    implementation(libs.springframework.boot.autoconfigure)

    // OTel Logback appender is referenced by runtime/cfg/logback-spring.xml and
    // installed into Spring's OpenTelemetry instance by OpenTelemetryAppenderInstaller
    // in this module. Cross-cutting observability wiring belongs in configuration
    // per Part 10 §10.1.
    implementation(libs.opentelemetry.logback.appender)
}
