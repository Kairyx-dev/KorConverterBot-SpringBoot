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
