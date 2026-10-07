package com.erp.common.util;

import tools.jackson.databind.json.JsonMapper;

/**
 * A default {@link JsonMapper}, deliberately independent of the application's JSON customisations —
 * for JSON text erp-core stores itself (notification variables, audit change sets), whose format must
 * not change with the application's Jackson configuration. Thread-safe.
 */
public final class PlainJson {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    private PlainJson() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }
}
