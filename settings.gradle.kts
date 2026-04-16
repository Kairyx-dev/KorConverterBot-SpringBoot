rootProject.name = "KorConverterBot-SpringBoot"

data class Module(
    val name: String,
    val path: String
)

val modules = mutableListOf<Module>()

fun module(name: String, path: String) {
    modules.add(Module(name, "$rootDir/$path"))
}

module(":boot", "/korConverter/boot")
module(":configuration", "/korConverter/configuration")
module(":application", "/korConverter/hexagonal/application")
module(":domain", "/korConverter/hexagonal/domain")
module(":adapter-persistence", "/korConverter/hexagonal/adapter/adapter-persistence")
module(":adapter-bot", "/korConverter/hexagonal/adapter/adapter-bot")

modules.forEach {
    include(it.name)
    project(it.name).projectDir = file(it.path)
}
