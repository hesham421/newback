package com.erp.hx.controller;

@RestController
@RequestMapping("/api/v1/hx/items")
public class HxController {

    private final HxService hxService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> create(@Valid @RequestBody ItemCreateRequest request) {
        return respond(hxService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ItemResponse>> move(@PathVariable Long id, @RequestParam String to) {
        return respond(hxService.move(id, to));
    }

    @GetMapping("/lookups/{key}")
    public ResponseEntity<ApiResponse<List<String>>> lookup(@PathVariable String key) {
        return respond(hxService.lookup(key));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ItemResponse>>> search(@RequestBody ItemSearchRequest request) {
        return respond(hxService.search(request));
    }
}
