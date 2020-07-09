package me.pcasaes.hexoids;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class EnforceCleanArchitectureDependencyFlowArchUnitTest {

    @Test
    void testDomainsShouldNotDependOnOtherLayers() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.core.domain..");

        ArchRule rule = noClasses()
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
                .resideInAnyPackage("me.pcasaes.hexoids.core.application..");


        rule.check(importedClasses);
    }

    @Test
    void testApplicationShouldOnlyDependOnDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.core.application..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..");


        rule.check(importedClasses);
    }

    @Test
    void testEntrypointShouldOnlyDependOnApplicationAndDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.entrypoints..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..");


        rule.check(importedClasses);
    }

    @Test
    void testInfrastructureShouldOnlyDependOnApplicationAndDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.infrastructure..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.configuration..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..");


        rule.check(importedClasses);
    }
}
