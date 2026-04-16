dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(project(":adapter-persistence"))
    implementation(project(":adapter-bot"))

    implementation(libs.springframework.tx)
    implementation(libs.springframework.boot.autoconfigure)
}
