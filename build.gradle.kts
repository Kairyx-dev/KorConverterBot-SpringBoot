plugins {
    // Spotless 는 SpotlessTaskService 라는 공유 BuildService 를 쓴다. 루트에 올리지 않고
    // 서브프로젝트에만 적용하면 프로젝트마다 별도 클래스로더가 같은 클래스를 각자 로드해
    // 실패한다. 여기서는 클래스로더 공유만 담당하고 실제 설정은 java-conventions 에 있다.
    // specs/001-build-logic-migration/research.md R-9 참조
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "org.specter.converter"
    version = "2.2.1"

    repositories {
        mavenCentral()
    }
}
