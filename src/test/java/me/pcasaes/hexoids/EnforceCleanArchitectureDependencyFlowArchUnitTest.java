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
                .importPackages("me.pcasaes.hexoids.domain..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.application..");


        rule.check(importedClasses);
    }

    @Test
    void testApplicationShouldOnlyDependOnDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.application..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.entrypoints..");


        rule.check(importedClasses);
    }

    @Test
    void testClientInterfaceShouldOnlyDependOnApplicationAndDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.entrypoints..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("me.pcasaes.hexoids.infrastructure..");


        rule.check(importedClasses);
    }
}
