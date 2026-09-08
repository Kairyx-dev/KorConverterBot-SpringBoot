import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    idea
    jacoco
    checkstyle
    alias(libs.plugins.net.ltgt.errorprone)
    alias(libs.plugins.spotless)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

// repositories 는 루트 allprojects 로 통합했다 (data-model.md J-12).

dependencies {
    // Library
    implementation(libs.projectlombok.lombok)
    annotationProcessor(libs.projectlombok.lombok)

    // Static Analysis
    errorprone(libs.com.google.errorprone.core)
    errorprone(libs.com.uber.nullaway)

    testImplementation(libs.projectlombok.lombok)
    testAnnotationProcessor(libs.projectlombok.lombok)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // 알려진 하자 — 이 프로젝트의 패키지가 아니라 NullAway 자신의 패키지를 가리킨다.
        // 무행위변경 계약에 따라 의도적으로 그대로 이관한다.
        // specs/001-build-logic-migration/data-model.md J-6 참조
        option("NullAway:AnnotatedPackages", "com.uber")
    }
}

// JaCoCo
tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.named("test"))
}

// 알려진 하자 — 이 검증 규칙은 check 에 연결되어 있지 않아 실제로 강제되지 않는다.
// 무행위변경 계약에 따라 의도적으로 그대로 이관한다.
// specs/001-build-logic-migration/data-model.md J-8 참조
tasks.withType<JacocoCoverageVerification>().configureEach {
    violationRules {
        rule {
            limit {
                minimum = 0.80.toBigDecimal()
            }
        }
    }
}

// Checkstyle
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}
tasks.withType<Checkstyle>().configureEach {
    // Exclude jOOQ generated sources
    exclude("**/generated/**")
}

spotless {
    java {
        target("**/*.java")
        targetExclude("**/build/**", "**/generated/**")
        palantirJavaFormat(libs.versions.palantir.java.format.get())
    }
}
