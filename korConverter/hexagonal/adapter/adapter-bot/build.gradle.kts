dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.slf4j.api)
    annotationProcessor(libs.slf4j.api)

    implementation(libs.dev8tion.jda)
}