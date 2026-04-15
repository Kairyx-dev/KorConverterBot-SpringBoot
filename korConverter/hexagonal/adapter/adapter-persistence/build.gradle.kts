plugins {
    alias(libs.plugins.jooq.codegen)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.springframework.boot.starter.jooq)
    implementation(libs.springframework.boot.autoconfigure)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.org.postgresql)

    jooqCodegen(libs.jooq.codegen)
    jooqCodegen("org.jooq:jooq-meta-extensions:${libs.versions.jooq.get()}")

    testImplementation(libs.springframework.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

val jooqGeneratedDir = layout.buildDirectory.dir("generated/sources/jooq/main")

sourceSets {
    main {
        java {
            srcDir(jooqGeneratedDir)
        }
    }
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "semantic"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            target {
                packageName = "org.specter.converter.adapter.persistence.generated"
                directory = jooqGeneratedDir.get().asFile.absolutePath
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn(tasks.named("jooqCodegen"))
}
