package com.outreach.platform.common.architecture;

import com.outreach.platform.common.entity.BaseEntity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import java.util.Set;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Automated architecture rule enforcement using ArchUnit.
 *
 * <p>These rules run as part of {@code mvn test} and fail the build if architecture drifts.
 * When running from common-lib, only common-lib classes are on the classpath.
 * When services include common-lib as a dependency (both a regular and a {@code test-jar}
 * dependency, plus {@code dependenciesToScan} configured on that service's
 * {@code maven-surefire-plugin} — see ADR 0001, docs/adr/0001-archunit-enforcement-scope.md),
 * these rules scan the full {@code com.outreach.platform} package hierarchy, including that
 * service's own compiled classes.
 *
 * <p>Services inherit these rules via the shared test dependency on common-lib.
 */
@AnalyzeClasses(packages = "com.outreach.platform",
        importOptions = {ImportOption.DoNotIncludeTests.class, ArchitectureRulesTest.DoNotIncludeJarPackagedTestClasses.class})
public class ArchitectureRulesTest {

    /**
     * {@link ImportOption.DoNotIncludeTests} only recognizes test classes sitting in an exploded
     * {@code target/test-classes} directory (matched by path substring) — it does NOT recognize
     * test classes packaged inside a {@code test-jar} (like common-lib's own
     * {@code common-lib-*-tests.jar}, which every service consuming these rules also depends on).
     * Without this, a consuming service's ArchUnit run also analyzes common-lib's own test
     * classes (e.g. {@code GlobalExceptionHandlerIT}) as if they were application code, flagging
     * ordinary Spring test fixtures (an {@code @Autowired MockMvc} field) as field-injection
     * violations — confirmed by running this exact scenario before adding this filter. Matches
     * this project's own {@code **&#47;*Test.java}/{@code **&#47;*IT.java} Surefire include
     * convention as the definition of "a test class."
     */
    public static class DoNotIncludeJarPackagedTestClasses implements ImportOption {
        private static final Pattern JAR_PACKAGED_TEST_CLASS = Pattern.compile(".*/[^/]+(Test|IT)\\.class$");

        @Override
        public boolean includes(Location location) {
            return !(location.isJar() && location.matches(JAR_PACKAGED_TEST_CLASS));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1: Entities in domain/entity packages must extend BaseEntity hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    // Composite-key entities (@EmbeddedId, e.g. EventBeneficiaryEntity) cannot extend BaseEntity —
    // BaseEntity declares its own single @Id UUID field, which a composite key can't coexist
    // with. Excluded here rather than frozen: this is a fully understood, structural exception
    // (confirmed the first time this rule ever ran against a real service, see
    // docs/specs/platform-hardening/), not a violation someone should eventually go fix.
    private static final DescribedPredicate<JavaClass> HAS_COMPOSITE_KEY = DescribedPredicate.describe(
            "has a composite primary key (@EmbeddedId)",
            javaClass -> javaClass.getFields().stream()
                    .anyMatch(field -> field.isAnnotatedWith(jakarta.persistence.EmbeddedId.class)));

    // Frozen: notification-service's NotificationScheduleEntity/NotificationTemplateEntity and
    // report-service's ReportScheduleEntity still hand-roll their own tenant filter instead of
    // extending TenantAwareBaseEntity — a known, already-tracked gap (Requirement 1, Task 1.2b,
    // docs/specs/platform-hardening/), not something this rollout should block on fixing. Freezing
    // accepts today's violations as a baseline that must never grow; fixing Task 1.2b will shrink
    // it back toward zero, at which point this rule should stop being frozen (see FreezingArchRule
    // usage above for tenant_scoped_repositories_must_not_be_queried_unscoped — same pattern).
    @ArchTest
    static final ArchRule entities_in_domain_package_must_extend_base_entity =
            FreezingArchRule.freeze(
                    classes()
                            .that().resideInAnyPackage("..domain..", "..entity..")
                            .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                            .and(DescribedPredicate.not(HAS_COMPOSITE_KEY))
                            .should().beAssignableTo(BaseEntity.class)
                            .because("All JPA entities must extend BaseEntity or TenantAwareBaseEntity " +
                                    "to ensure consistent audit fields and tenant isolation")
            );

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

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 6: Tenant-scoped repositories must not be queried via findById/deleteById/
    // getReferenceById — Hibernate's @Filter does not protect these primary-key-based
    // lookups, only query-based access. See docs/specs/platform-hardening/
    // requirements.md Finding 0 for the regression test that proved this, and
    // Finding 0's fix pattern: every affected repository gained a
    // findByIdAndTenantId(id, tenantId) method — its presence on a repository is
    // this rule's signal that the repository is tenant-scoped and has a safe
    // alternative that must be used instead.
    //
    // Frozen (see FreezingArchRule / archunit.properties) rather than a hard failure:
    // each service's fallback branch for the PLATFORM_ADMIN case (TenantContext
    // empty) intentionally still calls findById directly, and existing services may
    // have other pre-existing violations not yet audited. Freezing accepts today's
    // known violations as a baseline that must never grow, without requiring every
    // one to be fixed before this rule can exist at all. Run `mvn test` after fixing
    // a frozen violation to shrink the baseline — it is never allowed to grow back.
    // ─────────────────────────────────────────────────────────────────────────

    private static final Set<String> UNSCOPED_LOOKUP_METHODS =
            Set.of("findById", "deleteById", "getReferenceById", "getById");

    @ArchTest
    static final ArchRule tenant_scoped_repositories_must_not_be_queried_unscoped =
            FreezingArchRule.freeze(
                    noClasses()
                            .should().callMethodWhere(callsUnscopedLookupOnTenantScopedRepository())
                            .because("Hibernate's @Filter does not protect findById()/EntityManager.find() " +
                                    "(or deleteById()/getReferenceById(), which resolve the same way) — see " +
                                    "docs/specs/platform-hardening/requirements.md Finding 0. Use the " +
                                    "repository's findByIdAndTenantId(id, tenantId) method instead, falling " +
                                    "back to the unscoped method only for the PLATFORM_ADMIN " +
                                    "(TenantContext empty) case.")
            );

    private static DescribedPredicate<JavaMethodCall> callsUnscopedLookupOnTenantScopedRepository() {
        return new DescribedPredicate<JavaMethodCall>(
                "calls findById/deleteById/getReferenceById/getById on a tenant-scoped repository") {
            @Override
            public boolean test(JavaMethodCall call) {
                if (!UNSCOPED_LOOKUP_METHODS.contains(call.getTarget().getName())) {
                    return false;
                }
                return isTenantScopedRepository(call.getTarget().getOwner());
            }
        };
    }

    private static boolean isTenantScopedRepository(JavaClass owner) {
        return owner.getMethods().stream()
                .anyMatch(method -> method.getName().equals("findByIdAndTenantId"));
    }
}
