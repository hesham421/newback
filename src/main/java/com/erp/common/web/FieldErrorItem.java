package com.erp.common.web;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldErrorItem {

    private final String field;
    private final String message;
}
