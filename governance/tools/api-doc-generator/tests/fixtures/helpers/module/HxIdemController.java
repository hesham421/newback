package com.erp.hx.controller;

@RestController
@RequestMapping("/api/v1/hx/idem")
public class HxIdemController {

    private final HxService hxService;
    private final HxResponder responder;
    private final HxIdempotentResponses idempotentResponses;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> create(
            @Parameter(schema = @Schema(pattern = "^[A-Za-z0-9._:-]{1,64}$")) @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ItemCreateRequest request) {
        return idempotentResponses.craftResponse(key, request, () -> hxService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> plain(@PathVariable Long id) {
        return responder.craftResponse(hxService.move(id, "X"));
    }
}
