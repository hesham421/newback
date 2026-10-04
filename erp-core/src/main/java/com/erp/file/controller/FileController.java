package com.erp.file.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.file.dto.AccessTokenResponse;
import com.erp.file.dto.FileMetadataResponse;
import com.erp.file.dto.UploadRequest;
import com.erp.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Thin controller for API-FILE-001..006 (FileDocument, SCR-FILE-002 File Browser). Upload is
 * multipart; download returns the raw binary body (not the ApiResponse envelope) with the stored
 * MIME and an attachment disposition. All JSON endpoints delegate through the shared response
 * helper. Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Documents", description = "File upload, download and lifecycle - رفع الملفات وتنزيلها ودورة حياتها")
public class FileController {

    private final FileService service;
    private final OperationCode operationCode;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "رفع ملف")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute UploadRequest request) {
        return operationCode.craftResponse(service.store(request, file));
    }

    @PostMapping("/{id}/access-token")
    @Operation(summary = "Issue file access token", description = "إصدار رمز وصول للملف")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> issueToken(@PathVariable Long id) {
        return operationCode.craftResponse(service.issueAccessToken(id));
    }

    @GetMapping("/download")
    @Operation(summary = "Download file by access token", description = "تنزيل ملف برمز الوصول")
    public ResponseEntity<byte[]> download(@RequestParam("token") String token) {
        FileService.FileDownload payload = service.retrieve(token);
        // RFC 6266 encoding via ContentDisposition — safely handles non-ASCII (e.g. Arabic) names and
        // prevents header injection/breakage from quotes or CRLF in the stored filename.
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(payload.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(payload.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(payload.content());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file metadata", description = "جلب بيانات الملف الوصفية")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> metadata(@PathVariable Long id) {
        return operationCode.craftResponse(service.getMetadata(id));
    }

    @GetMapping
    @Operation(summary = "List files by owner", description = "قائمة الملفات حسب المالك")
    public ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> listByOwner(
            @RequestParam Long ownerId,
            @RequestParam String ownerType,
            @RequestParam String moduleCode,
            @RequestParam(required = false) String fileTypeId,
            @RequestParam(required = false) String fileStatusId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return operationCode.craftResponse(service.listByOwner(
            ownerId, ownerType, moduleCode, fileTypeId, fileStatusId, page, size, sort));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive or soft-delete file", description = "أرشفة أو حذف منطقي للملف")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> archiveOrDelete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ARCHIVE") String action) {
        return operationCode.craftResponse(service.softDelete(id, action));
    }
}
