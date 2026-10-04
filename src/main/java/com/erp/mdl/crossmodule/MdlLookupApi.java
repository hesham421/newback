package com.erp.mdl.crossmodule;

import java.util.List;

/**
 * MDL's inbound cross-module surface for XM-INBOUND-STUB-2 (INT-R.md): a narrow read of one
 * lookup type's active values, reached the same way SEC's own consumers reach SEC — direct
 * Spring interface injection, never loopback HTTP (see {@code com.erp.sec.crossmodule
 * .SecUserDirectoryApi}'s javadoc, which states that doctrine explicitly). Anticipated first real
 * consumers, per the later NOTIF/FILE dispatch: NOTIF reading {@code NOTIF_CHANNEL}/
 * {@code NOTIF_STATUS} and FILE reading {@code FILE_FILE_STATUS}/{@code FILE_FILE_TYPE} in place
 * of their current hardcoded Java {@code Map}s. Returns only the narrow
 * {@link LookupOptionView} read-model — never the internal {@code LookupValueResponse} DTO or the
 * {@code LookupValue} entity.
 */
public interface MdlLookupApi {

    /**
     * RULE-MDL-004 — the same rule API-MDL-011's own {@code LookupConsumerService.readByKey}
     * enforces: an unknown {@code typeKey}, or one that resolves to an inactive
     * {@code LookupType}, both throw {@code LocalizedException(Status.NOT_FOUND,
     * MdlErrorCodes.MDL_404_TYPE_KEY, typeKey)} — the identical exception the HTTP path surfaces
     * — rather than returning an empty list. Keeping the cross-module contract identical to the
     * HTTP one lets each consumer decide its own translation (e.g. falling back to a hardcoded
     * default vs. propagating a 404) instead of MDL silently masking a configuration error (a
     * NOTIF/FILE deploy that races ahead of the V20 seed migration, or a typo'd key) as a valid,
     * empty answer. Values are ordered by {@code sortOrder} (QR-MDL-011); an active type with
     * zero active values legitimately returns an empty list.
     *
     * @param typeKey the {@code LookupType}'s natural key, e.g. {@code "NOTIF_CHANNEL"}
     * @return the type's active {@link LookupOptionView}s, ordered by {@code sortOrder}
     */
    List<LookupOptionView> readActiveValuesByKey(String typeKey);
}
