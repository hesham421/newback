package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.config.TenantIdentifierResolver;
import com.erp.tenant.exception.TenantErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link TenantContext} and {@link TenantIdentifierResolver} (erp-core step 05). */
class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void require_withoutATenant_throwsTenantContextMissing() {
        assertThat(TenantContext.current()).isNull();
        assertThat(TenantContext.find()).isEmpty();
        assertThatThrownBy(TenantContext::require)
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(TenantErrorCodes.TENANT_CONTEXT_MISSING);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.INTERNAL_ERROR);
            });
    }

    @Test
    void runAs_andCallAs_setTheTenant_andRestoreThePreviousOne_evenWhenNestedOrFailing() {
        assertThat(TenantContext.callAs(7L, TenantContext::require)).isEqualTo(7L);
        assertThat(TenantContext.current()).isNull();

        TenantContext.set(1L);
        TenantContext.runAs(7L, () -> {
            assertThat(TenantContext.require()).isEqualTo(7L);
            TenantContext.runAs(8L, () -> assertThat(TenantContext.require()).isEqualTo(8L));
            assertThat(TenantContext.require()).isEqualTo(7L);
        });
        assertThat(TenantContext.require()).isEqualTo(1L);

        assertThatThrownBy(() -> TenantContext.runAs(9L, () -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");
        assertThat(TenantContext.require()).isEqualTo(1L);
    }

    @Test
    void set_rejectsNull() {
        assertThatThrownBy(() -> TenantContext.set(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TenantContext.runAs(null, () -> { })).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolver_toleratesNoTenantOnlyDuringBootstrap_andIsNeverRoot() {
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();
        assertThat(resolver.validateExistingCurrentSessions()).isFalse();
        assertThat(resolver.isRoot(TenantConstants.PLATFORM_TENANT_ID)).isFalse();

        assertThat(resolver.isBootstrapping()).isTrue();
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantIdentifierResolver.BOOTSTRAP_TENANT_ID);
        TenantContext.set(5L);
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(5L);
        TenantContext.clear();

        resolver.bootstrapComplete();
        assertThatThrownBy(resolver::resolveCurrentTenantIdentifier)
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(TenantErrorCodes.TENANT_CONTEXT_MISSING);
        assertThat(TenantContext.callAs(5L, resolver::resolveCurrentTenantIdentifier)).isEqualTo(5L);
    }
}
