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

/** Unit tests of the tenant business rules (erp-core step 05; tenant-maturity B: RULE-TENANT-016/017). */
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

    private static Tenant tenant(Long id, String status) {
        return Tenant.builder().id(id).code("ACME").nameAr("أ").nameEn("A").statusCode(status).build();
    }
}
