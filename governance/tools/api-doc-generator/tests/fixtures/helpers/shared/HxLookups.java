package com.erp.common.lookup;

public final class HxLookups {

    public static List<String> read(String rawKey, Set<String> ownedKeys, String unknownKeyErrorCode,
                                    Function<String, List<String>> reader) {
        if (!ownedKeys.contains(rawKey)) {
            throw new LocalizedException(Status.NOT_FOUND, unknownKeyErrorCode, rawKey);
        }
        return reader.apply(rawKey);
    }
}
