package com.erp.tenant;

import java.time.Instant;

/**
 * tenant-maturity C12 (RULE-TENANT-023, ADR-TENANT-002) — the tenant ({@code tid}) and issue instant ({@code iat}) of
 * the request's signature-valid bearer token. SEC's JWT filter stores it as request attribute {@link #REQUEST_ATTRIBUTE}
 * whether or not the token authenticated; the tenant filter compares it with the tenant's token cut-off.
 */
public record TenantTokenFacts(Long tenantId, Instant issuedAt) {

    public static final String REQUEST_ATTRIBUTE = TenantTokenFacts.class.getName();
}
