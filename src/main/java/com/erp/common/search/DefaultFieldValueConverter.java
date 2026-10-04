package com.erp.common.search;

public final class DefaultFieldValueConverter implements FieldValueConverter {

    public static final DefaultFieldValueConverter INSTANCE = new DefaultFieldValueConverter();

    private DefaultFieldValueConverter() {
    }

    @Override
    public Object convert(String field, Object rawValue) {
        return rawValue;
    }
}
