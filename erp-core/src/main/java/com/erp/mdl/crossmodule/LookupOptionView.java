package com.erp.mdl.crossmodule;

/**
 * Read-model for {@link MdlLookupApi#readActiveValuesByKey} — the only ENT-MDL-002 fields MDL
 * exposes outside the module: the value's own natural {@code code}, its two display labels, and
 * {@code sortOrder} for the consumer to preserve MDL's own ordering (QR-MDL-011). Never the
 * {@code lookupValuePk}, the owning {@code LookupType} association, {@code isActiveFl} (already
 * filtered by the producing side), or any audit column — same anti-corruption-boundary posture as
 * {@code com.erp.sec.crossmodule.UserContact}.
 */
public record LookupOptionView(
    String code,
    String labelAr,
    String labelEn,
    Integer sortOrder) {
}
