dependencies {
    implementation(libs.ch.qos.logback.core)
    implementation(libs.ch.qos.logback.classic)
    implementation(libs.org.jspecify)

    implementation(libs.slf4j.api)
    annotationProcessor(libs.slf4j.api)
}