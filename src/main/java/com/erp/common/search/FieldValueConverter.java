package com.erp.common.search;

public interface FieldValueConverter {

    Object convert(String field, Object rawValue);
}
