pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "KorConverterBot-SpringBoot"

fun module(name: String, path: String) {
    include(name)
    project(name).projectDir = file("$rootDir/$path")
}

module(":boot", "/korConverter/boot")
module(":configuration", "/korConverter/configuration")
module(":application", "/korConverter/hexagonal/application")
module(":domain", "/korConverter/hexagonal/domain")
module(":adapter-persistence", "/korConverter/hexagonal/adapter/adapter-persistence")
module(":adapter-bot", "/korConverter/hexagonal/adapter/adapter-bot")
