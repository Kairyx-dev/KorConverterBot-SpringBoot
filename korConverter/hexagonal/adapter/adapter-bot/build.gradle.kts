plugins {
    id("java-conventions")
    id("spring-conventions")
}

dependencies {
    implementation(project(":application"))

    implementation(libs.dev8tion.jda)
    implementation(libs.springframework.boot.autoconfigure)
    implementation(libs.springframework.boot.starter.validation)
}
