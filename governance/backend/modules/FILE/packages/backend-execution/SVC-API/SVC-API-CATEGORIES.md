<!-- Source: PHASE:SVC-API / SUB:SVC-API-CATEGORIES -->
<!-- Context: see SVC-API-HEADER.md for phase-level strategy, registry table, and intro -->

<!-- API:API-FILE-007:START -->
### API-FILE-007 — Categories CRUD
POST/GET/PUT/DELETE /api/v1/files/categories(/{id}) | FileCategoryController → FileCategoryService
REQUEST CategoryCreate/UpdateRequest{categoryCode(create-only), nameAr, nameEn, maxSizeBytes?, allowedContentTypes?, isActiveFl}
RESPONSE 201/200 CategoryResponse; search → Page<CategoryResponse>
VALIDATIONS: RULE-FILE-007 (unique categoryCode — Message-AR: رمز الفئة مستخدَم مسبقاً.); LOC (nameAr,nameEn required)
ERRORS: ERR-0005 → RULE-FILE-007 dup code → 409; ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0007..0011 (find_one/search/save/update/EXISTS) — mixed — Sequence SEQ_FILE_CATEGORY | ALLOWED_SORT_FIELDS={categoryCode,nameAr,createdAt}
SECURITY: SCR-FILE-001 (VIEW/CREATE/UPDATE/DELETE — FILE_ADMIN).
<!-- API:API-FILE-007:END -->
