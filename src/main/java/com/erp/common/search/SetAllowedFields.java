package com.erp.common.search;

import java.util.Set;

public class SetAllowedFields {

    private final Set<String> allowedFields;

    public SetAllowedFields(Set<String> allowedFields) {
        this.allowedFields = allowedFields;
    }

    public boolean isAllowed(String field) {
        return field != null && allowedFields.contains(field);
    }

    public Set<String> getAllowedFields() {
        return allowedFields;
    }
}
