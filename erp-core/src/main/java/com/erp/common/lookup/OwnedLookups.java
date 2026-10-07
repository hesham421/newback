package com.erp.common.lookup;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Serves the lookups (LOVs) a module owns but MDL stores. The module only fronts its own keys — never
 * a generic pass-through for arbitrary MDL keys — and MDL's own not-found (an unseeded or
 * deactivated type) is reported with the module's error code, so MDL's codes never leak out of the
 * module's API (cross-module rule).
 */
public final class OwnedLookups {

    private OwnedLookups() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * Reads the options of {@code rawKey} (trimmed and upper-cased) through {@code reader}.
     *
     * @param ownedKeys           the keys the module answers for; any other key is refused before
     *                            {@code reader} is called
     * @param unknownKeyErrorCode the module's 404 code, with {@code rawKey} as its argument — thrown
     *                            for a key outside {@code ownedKeys} and for a NOT_FOUND from
     *                            {@code reader}; any other exception propagates unchanged
     * @param reader              reads the active options of a normalized key (typically
     *                            {@code MdlLookupApi.readActiveValuesByKey} plus a mapping)
     */
    public static List<LookupOptionResponse> read(String rawKey, Set<String> ownedKeys, String unknownKeyErrorCode,
                                                  Function<String, List<LookupOptionResponse>> reader) {
        String normalized = rawKey == null ? null : rawKey.trim().toUpperCase();
        if (normalized == null || !ownedKeys.contains(normalized)) {
            throw new LocalizedException(Status.NOT_FOUND, unknownKeyErrorCode, rawKey);
        }
        try {
            return reader.apply(normalized);
        } catch (LocalizedException ex) {
            if (ex.getStatus() == Status.NOT_FOUND) {
                throw new LocalizedException(Status.NOT_FOUND, unknownKeyErrorCode, rawKey);
            }
            throw ex;
        }
    }
}
