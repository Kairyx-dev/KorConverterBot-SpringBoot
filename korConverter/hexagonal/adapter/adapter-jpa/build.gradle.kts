dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    runtimeOnly(libs.h2database)
    implementation(libs.springframework.boot.starter.data.jpa)
    implementation(libs.org.postgresql)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
}
