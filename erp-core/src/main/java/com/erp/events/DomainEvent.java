package com.erp.events;

import com.erp.tenant.TenantContext;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base class of every domain event published on the erp-core event bus (erp-core step 08).
 *
 * <p><b>Contract.</b> An event is an immutable fact that already happened. Besides its own payload it
 * carries:
 * <ul>
 *   <li>{@link #getId()}: a random UUID, unique per event (idempotency key for listeners);</li>
 *   <li>{@link #getOccurredAt()}: when it was created;</li>
 *   <li>{@link #getTenantId()}: the tenant the fact belongs to. An asynchronous listener runs on a
 *       pooled thread and must do its tenant work inside
 *       {@code TenantContext.runAs(event.getTenantId(), ...)} (the executor also propagates the
 *       publisher's tenant, but the event's own value is the authoritative one);</li>
 *   <li>{@link #getActor()}: the username that caused it ({@code system} when there was no caller);</li>
 *   <li>{@link #getRealm()}: the caller's realm — {@value #REALM_STAFF}, {@value #REALM_CUSTOMER} or
 *       {@value #REALM_SYSTEM}.</li>
 * </ul>
 * Subclasses add only plain values (ids, codes) — never a JPA entity, a password, a raw token or
 * other secret: events are handed to arbitrary core and application listeners.
 *
 * <p>The protected no-context constructor {@link #DomainEvent()} captures tenant, actor and realm
 * from the publishing thread (tenant from {@link TenantContext}, actor and realm from the Spring
 * Security context: an authority {@value #CUSTOMER_AUTHORITY} means the customer realm, any other
 * authenticated caller the staff realm, no caller the system realm). The explicit constructor is for
 * events published on behalf of another tenant or realm.
 */
public abstract class DomainEvent {

    /** Realm of a back-office (staff) caller. */
    public static final String REALM_STAFF = "STAFF";

    /** Realm of a self-registered customer (storefront account). */
    public static final String REALM_CUSTOMER = "CUSTOMER";

    /** Realm of code running without a caller (startup runners, jobs, asynchronous workers). */
    public static final String REALM_SYSTEM = "SYSTEM";

    /** The single authority of a customer-realm caller (erp-core step 06 plan). */
    public static final String CUSTOMER_AUTHORITY = "ROLE_CUSTOMER";

    /** Actor recorded when there is no caller. */
    public static final String SYSTEM_ACTOR = "system";

    private final UUID id;
    private final Instant occurredAt;
    private final Long tenantId;
    private final String actor;
    private final String realm;

    /** Captures the current tenant, actor and realm of the publishing thread. */
    protected DomainEvent() {
        this(TenantContext.current(), currentActor(), currentRealm());
    }

    /** Explicit tenant, actor and realm (for an event published on behalf of someone else). */
    protected DomainEvent(Long tenantId, String actor, String realm) {
        this.id = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.tenantId = tenantId;
        this.actor = actor == null || actor.isBlank() ? SYSTEM_ACTOR : actor;
        this.realm = Objects.requireNonNullElse(realm, REALM_SYSTEM);
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** The tenant this fact belongs to; {@code null} only for an event published outside any tenant. */
    public Long getTenantId() {
        return tenantId;
    }

    public String getActor() {
        return actor;
    }

    public String getRealm() {
        return realm;
    }

    /** A stable event type name (the simple class name), e.g. {@code UserCreatedEvent}. */
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getType() + "[id=" + id + ", tenantId=" + tenantId + ", actor=" + actor + ", realm=" + realm
            + ", occurredAt=" + occurredAt + "]";
    }

    private static Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private static String currentActor() {
        Authentication authentication = currentAuthentication();
        return authentication == null ? SYSTEM_ACTOR : authentication.getName();
    }

    private static String currentRealm() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return REALM_SYSTEM;
        }
        boolean customer = authentication.getAuthorities().stream()
            .anyMatch(granted -> CUSTOMER_AUTHORITY.equals(granted.getAuthority()));
        return customer ? REALM_CUSTOMER : REALM_STAFF;
    }
}
