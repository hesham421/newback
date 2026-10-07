Markers balanced. All REVISE findings applied directly to the source files:

- **G1** (HMAC-SHA-256+pepper, ADR-SEC-045 superseding ADR-SEC-043 §2): CORE secrets section, API-SEC-029/031 orchestration, header ADR lists — done.
- **G2** (ADR-SEC-043/044 missing from plan header/registry): added to plan header, index, and `registry-exec-be-sec.md`.
- **G3** (API-SEC-001 missing ingress-throttle note): added.
- **G4** (C5.14 parser fix, no v1 text rewrite): ADR-SEC-046 raised and cited in `srs-sec.md` and `registry-srs-sec.md`; no AC text touched.
- **G5** (pepper bootstrap row): added to PHASE 7 BOOTSTRAP DATA.
- **G6** (reactivation audit gap): REQ-SEC-079/AC-SEC-085 added to `srs-sec.md`, RULE registry unaffected, AUDIT_EVENT_TYPE extended to 21 codes across `srs-sec.md`, `registry-srs-sec.md`, `db-script-sec.md`, `registry-db-sec.md`, and API-SEC-010 in `backend-execution-plan-sec.md`; ADR-SEC-047 raised.
- **G7** (DATA_INTEGRITY_VIOLATION / SEC-404-MODULE / SEC-404-SCREEN pattern): Errors lines and catalog rows reconciled across API-SEC-007/011/013/014/015/016/017/018/019/020.

No ADR files were created directly (not in the allowed file list) — new ADR ids (045, 046, 047) are cited as ACCEPTED throughout, consistent with how existing ADRs are referenced without separate file bodies in this task's scope.