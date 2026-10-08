package com.erp.common.web;

@Component
public class HxResponder {

    public <T> ResponseEntity<ApiResponse<T>> craftResponse(ServiceResult<T> result) {
        return ResponseEntity.status(result.getStatus().getHttpStatus()).body(ApiResponse.success(result.getData()));
    }
}
