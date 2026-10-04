package com.erp.file.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.file.dto.LookupOptionResponse;
import com.erp.file.service.FileLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller for API-FILE-008 — the FILE runtime LOV provider (LOV-FILE-001 FILE_FILE_TYPE /
 * LOV-FILE-002 FILE_FILE_STATUS). Any authenticated caller may read the code lists (gated by the
 * service's {@code isAuthenticated()}); an unknown key yields 404. Pure delegation — zero logic.
 */
@RestController
@RequestMapping("/api/v1/files/lookups")
@RequiredArgsConstructor
@Tag(name = "File Lookups", description = "File runtime value lists - قوائم قيم الملفات")
public class FileLookupController {

    private final FileLookupService service;
    private final OperationCode operationCode;

    @GetMapping("/{lookupKey}")
    @Operation(summary = "Get file lookup values by key", description = "إرجاع قائمة قيم الملفات حسب المفتاح")
    public ResponseEntity<ApiResponse<List<LookupOptionResponse>>> get(@PathVariable String lookupKey) {
        return operationCode.craftResponse(service.get(lookupKey));
    }
}
