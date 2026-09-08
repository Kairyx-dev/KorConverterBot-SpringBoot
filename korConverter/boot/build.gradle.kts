// 순서 고정 (java → spring → boot) — BootJar.enabled 는 spring(false) → boot(true) configureEach 순서로 결정된다.
plugins {
    id("java-conventions")
    id("spring-conventions")
    id("boot-conventions")
}

dependencies {
    implementation(project(":configuration"))
    implementation(project(":adapter-bot"))
    implementation(project(":adapter-persistence"))
    implementation(project(":application"))
    implementation(project(":domain"))

    testImplementation(libs.springframework.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgresql)
}
