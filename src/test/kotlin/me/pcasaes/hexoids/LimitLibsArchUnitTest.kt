package me.pcasaes.hexoids

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import org.junit.jupiter.api.Test

class LimitLibsArchUnitTest {
    /**
     * The domain package should not use CDI.
     * The domain package should be 100% POJO
     */
    @Test
    fun testNoCDIInDomain() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.domain..")

        val rule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javax.enterprise..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("javax.inject..")
            .allowEmptyShould(true)



        rule.check(importedClasses)
    }

    /**
     * The domain package should not use Quarkus directly.
     * The domain package should be 100% POJO
     */
    @Test
    fun testNoQuarkusInDomain() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.domain..")

        val rule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.quarkus..")
            .allowEmptyShould(true)


        rule.check(importedClasses)
    }

    /**
     * The application package should not use CDI.
     * The application package should be 100% POJO
     */
    @Test
    fun testNoCDIInApplication() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.application..")

        val rule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javax.enterprise..")
            .orShould()
            .dependOnClassesThat()
            .resideInAnyPackage("javax.inject..")
            .allowEmptyShould(true)


        rule.check(importedClasses)
    }

    /**
     * The application package should not use Quarkus directly.
     * The application package should be 100% POJO
     */
    @Test
    fun testNoQuarkusInApplication() {
        val importedClasses = ClassFileImporter()
            .importPackages("me.pcasaes.hexoids.application..")

        val rule = ArchRuleDefinition.noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.quarkus..")
            .allowEmptyShould(true)


        rule.check(importedClasses)
    }
}
