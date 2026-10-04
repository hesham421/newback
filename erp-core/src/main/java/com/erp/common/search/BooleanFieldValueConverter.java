package com.erp.common.search;

import java.util.Set;

public final class BooleanFieldValueConverter implements FieldValueConverter {

    private final Set<String> booleanFields;

    public BooleanFieldValueConverter(Set<String> booleanFields) {
        this.booleanFields = booleanFields;
    }

    @Override
    public Object convert(String field, Object rawValue) {
        if (rawValue == null || !booleanFields.contains(field)) {
            return rawValue;
        }
        if (rawValue instanceof Boolean) {
            return rawValue;
        }
        return Boolean.parseBoolean(String.valueOf(rawValue));
    }
}
