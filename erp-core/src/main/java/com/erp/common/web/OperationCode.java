package com.erp.common.web;

import com.erp.common.domain.status.ServiceResult;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class OperationCode {

    public <T> ResponseEntity<ApiResponse<T>> craftResponse(ServiceResult<T> result) {
        return ResponseEntity
            .status(result.getStatus().getHttpStatus())
            .body(ApiResponse.success(result.getData()));
    }
}
