dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.springframework.boot.starter.jooq)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    runtimeOnly(libs.org.postgresql)

    testImplementation(libs.springframework.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}
