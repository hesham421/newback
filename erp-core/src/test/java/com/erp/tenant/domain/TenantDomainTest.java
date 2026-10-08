package com.erp.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Unit tests of the tenant business rules (erp-core step 05; tenant-maturity B: RULE-TENANT-016/017; E: RULE-TENANT-018/021; C12: RULE-TENANT-023/024). */
class TenantDomainTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACM", "ACME", "ACME_2", "A1_B2_C3", "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"})
    void create_acceptsCodesMatchingThePattern(String code) {
        assertThat(TenantDomain.create(code, false).getCode()).isEqualTo(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "AB", "acme", "Acme", "A-B", "A B", "ÄCME", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456"})
    void create_rejectsCodesNotMatchingThePattern(String code) {
        assertThatThrownBy(() -> TenantDomain.create(code, false))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_CODE_INVALID);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.VALIDATION_ERROR);
            });
    }

    @Test
    void create_rejectsNullAndTakenCodes() {
        assertThatThrownBy(() -> TenantDomain.create(null, false))
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(TenantErrorCodes.TENANT_CODE_INVALID);
        assertThatThrownBy(() -> TenantDomain.create("ACME", true))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_CODE_DUPLICATE);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.ALREADY_EXISTS);
            });
    }

    @Test
    void thePlatformTenantCannotBeSuspended_butMayBeReactivated() {
        TenantDomain platform = TenantDomain.from(tenant(TenantConstants.PLATFORM_TENANT_ID, TenantConstants.STATUS_ACTIVE));

        assertThatThrownBy(() -> platform.assertCanChangeStatusTo(TenantConstants.STATUS_SUSPENDED))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_PLATFORM_PROTECTED);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
            });
        assertThatCode(() -> platform.assertCanChangeStatusTo(TenantConstants.STATUS_ACTIVE)).doesNotThrowAnyException();
    }

    @Test
    void anyOtherTenant_mayBeSuspendedAndReactivated_idempotently() {
        TenantDomain other = TenantDomain.from(tenant(42L, TenantConstants.STATUS_SUSPENDED));

        assertThat(other.isActive()).isFalse();
        assertThatCode(() -> other.assertCanChangeStatusTo(TenantConstants.STATUS_SUSPENDED)).doesNotThrowAnyException();
        assertThatCode(() -> other.assertCanChangeStatusTo(TenantConstants.STATUS_ACTIVE)).doesNotThrowAnyException();
        assertThat(TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE)).isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ab", "  ab  "})
    void aSuspension_withoutAReasonOfThreeToFiveHundredCharacters_isRefused(String reason) {
        TenantDomain other = TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE));

        for (String given : new String[] {reason, null, "x".repeat(501)}) {
            assertThatThrownBy(() -> other.assertSuspensionReasonGiven(TenantConstants.STATUS_SUSPENDED, given))
                .isInstanceOf(LocalizedException.class)
                .satisfies(e -> {
                    assertThat(((LocalizedException) e).getErrorCode())
                        .isEqualTo(TenantErrorCodes.TENANT_SUSPENSION_REASON_REQUIRED);
                    assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                });
        }
    }

    @Test
    void aSuspensionReason_isCheckedAfterTrimming_andIgnoredForAnActivation() {
        TenantDomain other = TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE));

        assertThatCode(() -> other.assertSuspensionReasonGiven(TenantConstants.STATUS_SUSPENDED, " abc ")).doesNotThrowAnyException();
        assertThatCode(() -> other.assertSuspensionReasonGiven(TenantConstants.STATUS_SUSPENDED, "x".repeat(500)))
            .doesNotThrowAnyException();
        assertThatCode(() -> other.assertSuspensionReasonGiven(TenantConstants.STATUS_ACTIVE, null)).doesNotThrowAnyException();
    }

    @Test
    void reApplyingTheCurrentStatus_isNoTransition() {
        TenantDomain active = TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE));
        TenantDomain suspended = TenantDomain.from(tenant(42L, TenantConstants.STATUS_SUSPENDED));

        assertThat(active.changesStatusTo(TenantConstants.STATUS_ACTIVE)).isFalse();
        assertThat(active.changesStatusTo(TenantConstants.STATUS_SUSPENDED)).isTrue();
        assertThat(suspended.changesStatusTo(TenantConstants.STATUS_SUSPENDED)).isFalse();
        assertThat(suspended.changesStatusTo(TenantConstants.STATUS_ACTIVE)).isTrue();
    }

    @Test
    void adminReset_targetsOnlyAnExistingStaffUserHoldingASuperRole() {
        TenantDomain other = TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE));

        assertThatThrownBy(() -> other.assertCanResetAdministrator("ghost", false, false))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_ADMIN_NOT_FOUND);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.NOT_FOUND);
            });
        assertThatThrownBy(() -> other.assertCanResetAdministrator("clerk", true, false))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_ADMIN_NOT_SUPER);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
            });
        assertThatCode(() -> other.assertCanResetAdministrator("admin", true, true)).doesNotThrowAnyException();
    }

    @Test
    void adminReset_isNeverAllowedOnThePlatformTenant() {
        TenantDomain platform = TenantDomain.from(tenant(TenantConstants.PLATFORM_TENANT_ID, TenantConstants.STATUS_ACTIVE));

        assertThatThrownBy(platform::assertAdminResetAllowed)
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_ADMIN_RESET_PLATFORM);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
            });
        assertThatCode(() -> TenantDomain.from(tenant(42L, TenantConstants.STATUS_ACTIVE)).assertAdminResetAllowed())
            .doesNotThrowAnyException();
    }

    @Test
    void theEntityTransitions_setAndClearTheSuspensionFacts() {
        Tenant entity = tenant(42L, TenantConstants.STATUS_ACTIVE);
        Instant suspendedAt = Instant.parse("2026-10-08T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-10-09T10:00:00Z");

        entity.suspend(suspendedAt, "operator", "Unpaid invoice");
        assertThat(entity.getStatusCode()).isEqualTo(TenantConstants.STATUS_SUSPENDED);
        assertThat(entity.getSuspendedAt()).isEqualTo(suspendedAt);
        assertThat(entity.getSuspendedBy()).isEqualTo("operator");
        assertThat(entity.getSuspensionReason()).isEqualTo("Unpaid invoice");

        entity.activate(activatedAt);
        assertThat(entity.getStatusCode()).isEqualTo(TenantConstants.STATUS_ACTIVE);
        assertThat(entity.getSuspendedAt()).isNull();
        assertThat(entity.getSuspendedBy()).isNull();
        assertThat(entity.getSuspensionReason()).isNull();
        assertThat(entity.getTokensInvalidBefore()).isEqualTo(activatedAt);
    }

    @Test
    void logo_isAcceptedOnlyWhenFileStoredIt_andTheRefusalNamesTheFilePart() {
        assertThatCode(() -> TenantDomain.assertLogoAccepted(true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> TenantDomain.assertLogoAccepted(false))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                LocalizedException refusal = (LocalizedException) e;
                assertThat(refusal.getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_LOGO_INVALID);
                assertThat(refusal.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(refusal.getErrors()).singleElement().satisfies(d -> assertThat(d.field()).isEqualTo("file"));
            });
        assertThat(TenantDomain.LOGO_MAX_BYTES).isEqualTo(1_048_576L);
        assertThat(TenantDomain.LOGO_TYPES).containsExactlyInAnyOrder("image/png", "image/jpeg", "image/webp", "image/svg+xml");
        assertThat(TenantDomain.LOGO_OWNER_TYPE).isEqualTo("CORE_TENANT");
        assertThat(TenantDomain.LOGO_MODULE_CODE).isEqualTo("TENANT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"#1A2B3C", "#1a2b3c", "#000000", "#FFFFFF", " #abcdef ", "", "   "})
    void brandColor_acceptsHashAndSixHexDigits_orBlank(String colour) {
        assertThatCode(() -> TenantDomain.assertBrandColorValid(colour)).doesNotThrowAnyException();
        assertThatCode(() -> TenantDomain.assertBrandColorValid(null)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"red", "#12345", "#1234567", "1A2B3C", "#GGGGGG", "##12345", "#12 345", "rgb(1,2,3)"})
    void brandColor_refusesAnythingElse_namingTheField(String colour) {
        assertThatThrownBy(() -> TenantDomain.assertBrandColorValid(colour))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                LocalizedException refusal = (LocalizedException) e;
                assertThat(refusal.getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_BRAND_COLOR_INVALID);
                assertThat(refusal.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(refusal.getErrors()).singleElement().satisfies(d -> assertThat(d.field()).isEqualTo("brandColor"));
            });
    }

    @Test
    void branding_isServedForAnActiveTenantOnly_andPlatformMayCarryALogo() {
        assertThatCode(() -> TenantDomain.from(tenant(TenantConstants.PLATFORM_TENANT_ID, TenantConstants.STATUS_ACTIVE))
            .assertServed()).doesNotThrowAnyException();
        assertThatThrownBy(() -> TenantDomain.from(tenant(7L, TenantConstants.STATUS_SUSPENDED)).assertServed())
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_SUSPENDED);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.FORBIDDEN);
            });
    }

    @Test
    void tokenCutOff_comparesWholeSeconds_servesTheCutOffsOwnSecond_andRefusesATokenWithoutIat() {
        Instant cutOff = Instant.parse("2026-10-08T10:00:00.500Z");

        assertThat(TenantDomain.isTokenRevoked(Instant.parse("2026-10-08T09:59:59Z"), cutOff)).isTrue();
        assertThat(TenantDomain.isTokenRevoked(Instant.parse("2026-10-08T10:00:00Z"), cutOff))
            .as("issued in the cut-off's own second").isFalse();
        assertThat(TenantDomain.isTokenRevoked(Instant.parse("2026-10-08T10:00:01Z"), cutOff)).isFalse();
        assertThat(TenantDomain.isTokenRevoked(null, cutOff)).as("no iat").isTrue();
        assertThat(TenantDomain.isTokenRevoked(Instant.parse("2000-01-01T00:00:00Z"), null)).as("no cut-off").isFalse();
        assertThat(TenantDomain.isTokenRevoked(null, null)).isFalse();
    }

    @Test
    void tokenRevocation_isRefusedForThePlatformTenantOnly() {
        assertThatThrownBy(() -> TenantDomain.from(tenant(TenantConstants.PLATFORM_TENANT_ID, TenantConstants.STATUS_ACTIVE))
            .assertTokenRevocationAllowed())
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_REVOKE_TOKENS_PLATFORM);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
            });
        assertThatCode(() -> TenantDomain.from(tenant(7L, TenantConstants.STATUS_ACTIVE)).assertTokenRevocationAllowed())
            .doesNotThrowAnyException();
        assertThatCode(() -> TenantDomain.from(tenant(7L, TenantConstants.STATUS_SUSPENDED)).assertTokenRevocationAllowed())
            .as("a suspended tenant may be revoked").doesNotThrowAnyException();
    }

    @Test
    void revokeTokens_setsOnlyTheCutOff() {
        Tenant entity = tenant(7L, TenantConstants.STATUS_ACTIVE);
        Instant at = Instant.parse("2026-10-08T10:00:00Z");

        entity.revokeTokens(at);

        assertThat(entity.getTokensInvalidBefore()).isEqualTo(at);
        assertThat(entity.getStatusCode()).isEqualTo(TenantConstants.STATUS_ACTIVE);
    }

    private static Tenant tenant(Long id, String status) {
        return Tenant.builder().id(id).code("ACME").nameAr("أ").nameEn("A").statusCode(status).build();
    }
}
