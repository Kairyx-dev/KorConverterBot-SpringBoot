package org.specter.converter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.specter.converter")
class ArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_SPRING = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_JOOQ = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.jooq..");

    @ArchTest
    static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_SPRING = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule ADAPTER_BOT_MUST_NOT_DEPEND_ON_DOMAIN = noClasses()
            .that()
            .resideInAPackage("..adapter.bot..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..domain.model..", "..domain.event..",
                    "..domain.service..", "..domain.exception..");

    @ArchTest
    static final ArchRule ADAPTER_BOT_MUST_NOT_DEPEND_ON_PERSISTENCE = noClasses()
            .that()
            .resideInAPackage("..adapter.bot..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter.persistence..");

    @ArchTest
    static final ArchRule ADAPTER_PERSISTENCE_MUST_NOT_DEPEND_ON_BOT = noClasses()
            .that()
            .resideInAPackage("..adapter.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter.bot..");
}
