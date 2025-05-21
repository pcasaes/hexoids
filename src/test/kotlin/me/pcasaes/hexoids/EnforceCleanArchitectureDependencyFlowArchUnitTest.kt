package me.pcasaes.hexoids

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import org.junit.jupiter.api.Test

class EnforceCleanArchitectureDependencyFlowArchUnitTest {
    @Test
    fun testDomainsShouldNotDependOnOtherLayers() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.core.domain..")

        val rule: ArchRule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.core.application..")


        rule.check(importedClasses)
    }

    @Test
    fun testApplicationShouldOnlyDependOnDomain() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.core.application..")

        val rule: ArchRule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..")


        rule.check(importedClasses)
    }

    @Test
    fun testEntrypointShouldOnlyDependOnApplicationAndDomain() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.entrypoints..")

        val rule: ArchRule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")


        rule.check(importedClasses)
    }

    @Test
    fun testInfrastructureShouldOnlyDependOnApplicationAndDomain() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.infrastructure..")

        val rule: ArchRule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..")


        rule.check(importedClasses)
    }
}
