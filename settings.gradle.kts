rootProject.name = "KorConverterBot-SpringBoot"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

data class Module(
    val name: String,
    val path: String
)

val modules = mutableListOf<Module>()

fun module(name: String, path: String) {
    modules.add(Module(name, "$rootDir/$path"))
}

module(":boot", "/korConverter/boot")
module(":common", "/korConverter/common")
module(":application", "/korConverter/hexagonal/application")
module(":domain", "/korConverter/hexagonal/domain")
module(":adapter-jpa", "/korConverter/hexagonal/adapter/adapter-jpa")
module(":adapter-bot", "/korConverter/hexagonal/adapter/adapter-bot")

modules.forEach {
    include(it.name)
    project(it.name).projectDir = file(it.path)
}
