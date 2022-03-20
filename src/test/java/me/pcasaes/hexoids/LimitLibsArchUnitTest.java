package me.pcasaes.hexoids;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LimitLibsArchUnitTest {

    /**
     * The domain package should not use CDI.
     * The domain package should be 100% POJO
     */
    @Test
    void testNoCDIInDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.domain..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("javax.enterprise..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("javax.inject..")
                .allowEmptyShould(true);



        rule.check(importedClasses);
    }

    /**
     * The domain package should not use Quarkus directly.
     * The domain package should be 100% POJO
     */
    @Test
    void testNoQuarkusInDomain() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.domain..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.quarkus..")
                .allowEmptyShould(true);


        rule.check(importedClasses);
    }

    /**
     * The application package should not use CDI.
     * The application package should be 100% POJO
     */
    @Test
    void testNoCDIInApplication() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.application..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("javax.enterprise..")
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage("javax.inject..")
                .allowEmptyShould(true);


        rule.check(importedClasses);
    }

    /**
     * The application package should not use Quarkus directly.
     * The application package should be 100% POJO
     */
    @Test
    void testNoQuarkusInApplication() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("me.pcasaes.hexoids.application..");

        ArchRule rule = noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.quarkus..")
                .allowEmptyShould(true);


        rule.check(importedClasses);
    }
}
