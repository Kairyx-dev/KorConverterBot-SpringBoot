dependencies {
    implementation(project(":domain"))

    implementation(libs.slf4j.api)
    annotationProcessor(libs.slf4j.api)
}