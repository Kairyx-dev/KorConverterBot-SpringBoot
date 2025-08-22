import com.linecorp.support.project.multi.recipe.configureByLabel
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.linecorp.build.recipe.plugin)
    alias(libs.plugins.com.google.cloud.tools.jib)
}

allprojects {
    group = "org.specter.converter"
    version = "2.1.0"

    tasks.withType<BootJar> {
        enabled = false
    }
}

repositories {
    mavenCentral()
}

configureByLabel("java") {
    apply(plugin = "idea")
    apply(plugin = "java")

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    dependencies {
        // Library
        implementation(rootProject.libs.projectlombok.lombok)
        annotationProcessor(rootProject.libs.projectlombok.lombok)

        testImplementation(rootProject.libs.projectlombok.lombok)
        testAnnotationProcessor(rootProject.libs.projectlombok.lombok)
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

configureByLabel("spring") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        implementation(rootProject.libs.springframework.boot.starter)
        implementation(rootProject.libs.springframework.boot.starter.actuator)

        testImplementation(rootProject.libs.springframework.boot.starter.test)
    }
}

configureByLabel("test") {
    dependencies {

        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)

        testImplementation(platform(rootProject.libs.junit.bom))
        testImplementation(rootProject.libs.junit.jupiter.api)
        testImplementation(rootProject.libs.junit.jupiter.params)
        testImplementation(rootProject.libs.assertj)
    }
}

configureByLabel("boot") {
    apply(plugin = "com.google.cloud.tools.jib")
    tasks.withType<BootJar> {
        enabled = true
    }

    jib {
        from {
            image = "amazoncorretto:21.0.7-alpine"
        }

        to {
            val dockerId = System.getProperty("dockerId") ?: "UNKNOWN"
            val repositoryName = System.getProperty("dockerRepository") ?: "UNKNOWN"

            image = "$dockerId/$repositoryName"
            tags = setOf("${project.version}")
        }

        container {
            creationTime.set("USE_CURRENT_TIMESTAMP")
            jvmFlags = listOf(
                "-Dspring.config.location=file:./cfg/application.yml",
                "-Dlogging.config=file:./cfg/logback-spring.xml"
            )
            workingDirectory = "/app"
        }
    }
}