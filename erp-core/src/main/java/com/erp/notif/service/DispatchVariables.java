package com.erp.notif.service;

import com.erp.common.util.PlainJson;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * (De)serialises dispatch variables to/from {@code NOTIF_LOG.VARIABLES_JSON} (erp-core step 08): the
 * asynchronous worker — possibly after a restart, through the requeue job — renders the message from
 * the persisted variables, not from memory. A private mapper: the value is always a flat
 * {@code Map<String, String>}, independent of the application's JSON customisations.
 */
final class DispatchVariables {

    private static final JsonMapper MAPPER = PlainJson.MAPPER;
    private static final TypeReference<LinkedHashMap<String, String>> TYPE = new TypeReference<>() { };

    private DispatchVariables() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** JSON object text, or {@code null} for no variables. */
    static String write(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        return MAPPER.writeValueAsString(variables);
    }

    /** The variables, never {@code null}. */
    static Map<String, String> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return MAPPER.readValue(json, TYPE);
    }
}
