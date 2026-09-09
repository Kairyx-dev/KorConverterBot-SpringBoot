import dev.panuszewski.gradle.pluginMarker

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // pluginMarker() artifacts
    mavenCentral() // kotlin-stdlib
}

java {
    toolchain {
        // Gradle 9.7.1의 kotlin-dsl 플러그인은 jvmTarget을 데몬 JVM 버전으로 암묵적으로 맞춘다.
        // JDK 25(major 69)로 컴파일된 클래스 파일이 그보다 낮은 JVM에서 로드되면
        // UnsupportedClassVersionError로 죽으므로, toolchain을 25로 명시적으로 고정한다. (R-5)
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(pluginMarker(libs.plugins.springframework.boot))
    implementation(pluginMarker(libs.plugins.spring.dependency.management))
    implementation(pluginMarker(libs.plugins.net.ltgt.errorprone))
    implementation(pluginMarker(libs.plugins.spotless))
    implementation(pluginMarker(libs.plugins.com.google.cloud.tools.jib))
}
