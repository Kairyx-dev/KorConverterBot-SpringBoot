import com.linecorp.support.project.multi.recipe.configureByLabel
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.linecorp.build.recipe.plugin)
}

allprojects {
    group = "org.specter.template"
    version = "0.0.1"

    tasks.withType<BootJar>() {
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
        //BOM
        implementation(platform(rootProject.libs.junit.bom))

        // Library
        implementation(rootProject.libs.projectlombok.lombok)
        annotationProcessor(rootProject.libs.projectlombok.lombok)

        // Test
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
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

        testImplementation(rootProject.libs.springframework.boot.starter.test)
    }
}

configureByLabel("boot") {
    tasks.withType<BootJar> {
        enabled = true
    }
}