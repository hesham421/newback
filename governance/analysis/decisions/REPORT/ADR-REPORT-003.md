# ADR-REPORT-003 — Bounded `byte[]` export with cap + 1 probe, CSV BOM and formula-injection guard

Module  : REPORT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
The step file said the export "streams `text/csv`", that the page "may be unpaged for export", and
that an exported CSV with Arabic headers must "open correctly in Excel"
(`erp-core-plan/11-STEP-reporting-minimal.md`; `docs/DEVIATIONS.md` [11] "Export"). Three questions
had to be settled by the implementation:
1. **How to bound an export** without loading the whole result: an unpaged query would load
   everything before the cap could be checked; a count query per report would force every provider
   to implement one.
2. **How to send the body**: a `StreamingResponseBody` runs on an async dispatch that re-enters the
   security and tenant filter chain (`JwtAuthenticationFilter`, `TenantResolutionFilter`), while the
   provider's rows are already in memory anyway.
3. **How to write CSV safely**: Excel detects UTF-8 only with a byte-order mark (Arabic headers
   otherwise show as garbage); a cell starting with `=`, `+`, `-`, `@` is evaluated as a formula by
   spreadsheets (CSV injection); a CSV library would be a new dependency outside ArchUnit rule 1's
   allowed list.

## Decision
**A cap + 1 probe, a bounded `byte[]` body, a hand-written RFC 4180 writer with BOM and a formula
guard** (as built):
- the export asks the provider for ONE page: `PageRequest.of(0, maxRows + 1)` (the cap itself when the
  cap is `Integer.MAX_VALUE`); `ReportRunDomain.assertWithinExportCap(returnedRows, totalRows,
  maxRows)` refuses with 422 `REPORT_EXPORT_TOO_LARGE` (`Status.BUSINESS_RULE_VIOLATION`, argument the
  cap) when the rows returned, or the provider's `totalRows`, exceed the cap; the cap is
  `erp.core.report.max-export-rows` (`@Positive`, default 100 000)
  (`erp-core/src/main/java/com/erp/report/service/ReportService.java:114-117`;
  `report/domain/ReportRunDomain.java:51-61`; `autoconfigure/ErpCoreProperties.java:379-384`);
  providers must therefore honour `page.getPageSize()` (`report/ReportProvider.java:42-50`);
- the CSV or JSON is written into a `byte[]` bounded by the cap and returned as
  `ResponseEntity<byte[]>` with `Content-Disposition: attachment; filename="<code>.csv|json"` (UTF-8
  filename) and the format's content type — FILE download's pattern, no streaming
  (`report/controller/ReportController.java:62-75`; `report/service/ReportService.java:119-124`);
- `CsvReportWriter` writes the UTF-8 BOM `EF BB BF`, a header row of the Arabic or English labels
  (request locale `ar` → Arabic, `LocaleContextHolder`), CRLF records, RFC 4180 quoting (comma, quote,
  CR, LF or leading/trailing blank → quoted, quotes doubled), plain numbers
  (`BigDecimal.toPlainString`), ISO-8601 dates and times, `true`/`false`, empty for null, and prefixes
  a `STRING` cell starting with `=`, `+`, `-`, `@`, TAB or CR with an apostrophe
  (`report/export/CsvReportWriter.java:21-34`, `:47-106`); totals are JSON-only;
- the format is `csv` (default) or `json` (case-insensitive); anything else is 400
  `REPORT_PARAM_INVALID` on field `format` (`report/domain/ReportRunDomain.java:19-38`, `:63-75`);
  JSON is the `ReportRunResponse` of the whole capped result without the envelope, serialized with
  Spring's `JsonMapper`.

Reasons, from the step report and the deviations:
1. **One page of cap + 1 detects an overflow for free** — no count query, no full load, and the same
   page contract the run endpoint already imposes (`docs/DEVIATIONS.md` [11] "Export query").
2. **Buffering changes no memory bound**: the provider's rows are in memory either way; a streaming
   body would only add the async re-entry of the filters (`docs/DEVIATIONS.md` [11] "streams").
3. **Excel needs the BOM**; verified byte-for-byte over HTTP since no Excel was available on the
   execution machine (`docs/steps/11-report.md` Acceptance; `docs/DEVIATIONS.md` [11] last entry).
4. **A hand-written writer** keeps the dependency list unchanged (ArchUnit rule 1) and makes the
   formula guard explicit.

## Consequences
- An export is never larger than `max-export-rows` rows plus headers; raising the cap raises the
  memory a single request may use — the operator sets it per deployment.
- A provider that ignores the page size (returns everything) still gets the right answer (the rows
  are counted) but defeats the probe's purpose; `RULE-REPORT-012` documents the contract.
- A value whose first character is `=`, `+`, `-`, `@` in a `STRING` column arrives in Excel with a
  leading apostrophe; numeric columns (`INTEGER`, `DECIMAL`) are untouched, so a negative number is
  not prefixed.
- Totals are not in the CSV; a client that needs them uses the run endpoint or the JSON export.
- No OpenAPI group `report` was added; the endpoints appear in the aggregate document
  (`docs/DEVIATIONS.md` [11] "Property and OpenAPI").

## Traces
ENT-REPORT-002 · REQ-REPORT-008, REQ-REPORT-009, REQ-REPORT-010, REQ-REPORT-011, REQ-REPORT-016 ·
RULE-REPORT-006, RULE-REPORT-007, RULE-REPORT-008, RULE-REPORT-009, RULE-REPORT-010, RULE-REPORT-012 ·
POL-REPORT-007, POL-REPORT-008 · docs/DEVIATIONS.md [11] (entries 4, 5, 6, 11, 13); docs/steps/11-report.md
