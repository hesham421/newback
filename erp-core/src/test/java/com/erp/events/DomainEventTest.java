package com.erp.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit test (erp-core step 08): what a {@link DomainEvent} captures from the publishing thread. */
class DomainEventTest {

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void capturesTenantActorAndStaffRealm_fromTheCurrentThread() {
        TenantContext.set(5L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "bob", null, List.of(new SimpleGrantedAuthority("PERM_SEC_USERS_VIEW"))));

        UserCreatedEvent event = new UserCreatedEvent(10L, "carol");

        assertThat(event.getTenantId()).isEqualTo(5L);
        assertThat(event.getActor()).isEqualTo("bob");
        assertThat(event.getRealm()).isEqualTo(DomainEvent.REALM_STAFF);
        assertThat(event.getId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getType()).isEqualTo("UserCreatedEvent");
        assertThat(event.getUserId()).isEqualTo(10L);
    }

    @Test
    void aCustomerAuthorityMeansTheCustomerRealm_andNoCallerTheSystemRealm() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "shopper", null, List.of(new SimpleGrantedAuthority(DomainEvent.CUSTOMER_AUTHORITY))));
        assertThat(new CustomerVerifiedEvent(3L).getRealm()).isEqualTo(DomainEvent.REALM_CUSTOMER);

        SecurityContextHolder.clearContext();
        CustomerVerifiedEvent anonymous = new CustomerVerifiedEvent(3L);
        assertThat(anonymous.getRealm()).isEqualTo(DomainEvent.REALM_SYSTEM);
        assertThat(anonymous.getActor()).isEqualTo(DomainEvent.SYSTEM_ACTOR);
        assertThat(anonymous.getTenantId()).isNull();
    }

    @Test
    void tenantCreatedEvent_belongsToTheNewTenant() {
        TenantContext.set(1L);
        TenantCreatedEvent event = new TenantCreatedEvent(8L, "ACME", "platform-op");
        assertThat(event.getTenantId()).isEqualTo(8L);
        assertThat(event.getActor()).isEqualTo("platform-op");
        assertThat(event.getTenantCode()).isEqualTo("ACME");
    }
}
