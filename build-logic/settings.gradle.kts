pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // 버전을 카탈로그에 넣지 않고 리터럴로 둔다 — 이 플러그인 자신이 libs.versions.toml을
    // "libs" 카탈로그로 등록하는 주체이므로, 카탈로그에 자기 버전을 실으면 순환이 생긴다.
    id("dev.panuszewski.typesafe-conventions") version "0.11.1"
}

rootProject.name = "build-logic"

include("conventions")
