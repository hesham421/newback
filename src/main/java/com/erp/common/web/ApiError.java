package com.erp.common.web;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiError {

    private final String code;
    private final String message;
    private final List<FieldErrorItem> fieldErrors;
}
