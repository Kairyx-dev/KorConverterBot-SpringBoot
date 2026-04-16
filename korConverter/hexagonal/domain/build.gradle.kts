plugins {
    alias(libs.plugins.pitest)
}

pitest {
    targetClasses.set(listOf("org.specter.converter.domain.*"))
    targetTests.set(listOf("org.specter.converter.domain.*"))
    mutationThreshold.set(65)
    junit5PluginVersion.set("1.2.1")
    pitestVersion.set(libs.versions.pitest.core.get())
}

// D-1: ZERO external dependencies
dependencies {
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj)
    testImplementation(libs.jqwik)
}
