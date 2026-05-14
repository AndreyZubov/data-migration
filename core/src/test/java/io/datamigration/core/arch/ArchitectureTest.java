package io.datamigration.core.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Architectural invariants for the {@code core} module.
 *
 * <p>{@code core} must remain a pure-Java library: no Spring, no JPA, no servlet API. These
 * frameworks belong to outer modules ({@code web}, {@code batch}, {@code bulk-client}).
 */
@AnalyzeClasses(packages = "io.datamigration.core")
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreMustNotDependOnSpring =
            ArchRuleDefinition.noClasses()
                    .that()
                    .resideInAPackage("io.datamigration.core..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule coreMustNotDependOnJpa =
            ArchRuleDefinition.noClasses()
                    .that()
                    .resideInAPackage("io.datamigration.core..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule coreMustNotDependOnServlet =
            ArchRuleDefinition.noClasses()
                    .that()
                    .resideInAPackage("io.datamigration.core..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.servlet..", "javax.servlet..");
}
