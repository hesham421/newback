package com.erp.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Unit tests of the tenant business rules (erp-core step 05). */
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

    private static Tenant tenant(Long id, String status) {
        return Tenant.builder().id(id).code("ACME").nameAr("أ").nameEn("A").statusCode(status).build();
    }
}
