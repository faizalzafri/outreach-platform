package com.outreach.platform.common.architecture;

import com.outreach.platform.common.entity.BaseEntity;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Automated architecture rule enforcement using ArchUnit.
 *
 * <p>These rules run as part of {@code mvn test} and fail the build if architecture drifts.
 * When running from common-lib, only common-lib classes are on the classpath.
 * When services include common-lib as a dependency, these rules scan the full
 * {@code com.outreach.platform} package hierarchy.
 *
 * <p>Services inherit these rules via the shared test dependency on common-lib.
 */
@AnalyzeClasses(packages = "com.outreach.platform", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureRulesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1: Entities in domain/entity packages must extend BaseEntity hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule entities_in_domain_package_must_extend_base_entity =
            classes()
                    .that().resideInAnyPackage("..domain..", "..entity..")
                    .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().beAssignableTo(BaseEntity.class)
                    .because("All JPA entities must extend BaseEntity or TenantAwareBaseEntity " +
                            "to ensure consistent audit fields and tenant isolation");

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 2: No field injection — only constructor injection allowed
    // ─────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule no_field_injection =
            noFields()
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .because("Field injection makes classes harder to test and hides dependencies. " +
                            "Use constructor injection instead");

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 3: Services must not depend on controllers
    // ─────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule services_must_not_depend_on_controllers =
            noClasses()
                    .that().resideInAnyPackage("..service..")
                    .should().dependOnClassesThat().resideInAnyPackage("..controller..")
                    .because("Service layer must not depend on controller layer. " +
                            "This violates the dependency direction: controllers → services → repositories");

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 4: Repositories must reside in *.repository.* or *.repo.* packages
    // ─────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule repositories_must_be_in_correct_package =
            classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAnyPackage("..repository..", "..repo..")
                    .because("Repository interfaces must be located in a repository or repo package " +
                            "for consistent project structure and discoverability");

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5: No java.util.Date — use java.time.* (JSR 310) exclusively
    // ─────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule no_java_util_date =
            noClasses()
                    .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")
                    .because("java.util.Date is mutable, not thread-safe, and poorly designed. " +
                            "Use java.time.* (JSR 310) classes instead: LocalDate, LocalDateTime, " +
                            "Instant, ZonedDateTime");

    @ArchTest
    static final ArchRule no_java_util_calendar =
            noClasses()
                    .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Calendar")
                    .because("java.util.Calendar is legacy and should not be used. " +
                            "Use java.time.* (JSR 310) classes instead");
}
