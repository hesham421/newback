package com.erp.common.search;

public final class HxInstantConverter implements FieldValueConverter {

    private final Set<String> fields;

    public HxInstantConverter(Set<String> fields) {
        this.fields = Set.copyOf(fields);
    }

    @Override
    public Object convert(String field, Object raw) {
        try {
            return Instant.parse(String.valueOf(raw));
        } catch (DateTimeParseException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    }
}
