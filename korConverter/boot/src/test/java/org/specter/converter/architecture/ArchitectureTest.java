package org.specter.converter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.specter.converter")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jooq =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.jooq..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_spring =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule adapter_bot_must_not_depend_on_domain =
            noClasses().that().resideInAPackage("..adapter.bot..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..domain.model..", "..domain.event..",
                            "..domain.service..", "..domain.exception..");

    @ArchTest
    static final ArchRule adapter_bot_must_not_depend_on_persistence =
            noClasses().that().resideInAPackage("..adapter.bot..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.persistence..");

    @ArchTest
    static final ArchRule adapter_persistence_must_not_depend_on_bot =
            noClasses().that().resideInAPackage("..adapter.persistence..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.bot..");
}
