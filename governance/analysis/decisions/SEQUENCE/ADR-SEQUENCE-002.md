# ADR-SEQUENCE-002 — One row per (code, period) with an immutable reset policy; the pattern must carry the period

Module  : SEQUENCE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P2 (Database) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
A series may restart at 1 every year or every month (`RESET_POLICY` NEVER | YEARLY | MONTHLY,
`V14__sequence_and_settings.sql:37`, `:59`). The step-09 file named the columns but not how a reset is
represented, nor what keeps a yearly series from issuing `INV-000001` twice (docs/DEVIATIONS.md [09]
"Number-series row model unspecified beyond the columns", "Pattern validation beyond unknown token").

Two models were available for the counter across periods:
- **One row per code, counter reset in place** — the row would carry "current period" and "counter"; a
  reset is an `UPDATE` that must detect the period change under the lock; previous periods' final values
  are lost unless copied elsewhere; the unique key is (tenant, code).
- **One row per (code, period)** — every period has its own counter row; a period change is an `INSERT` of
  a new row at 1; the history of every period stays readable; the unique key is (tenant, code, period).

Independently, nothing in the column list stops an administrator from configuring `YEARLY` with the
pattern `{PREFIX}-{SEQ:6}`, which would render the same numbers every year.

## Decision
1. **One row per (code, period)**: `UQ_CORE_NUMBER_SERIES_CODE_PERIOD (TENANT_ID, CODE, PERIOD_KEY)`
   (`V14__sequence_and_settings.sql:57`); `PERIOD_KEY` is `''` for NEVER, `YYYY` for YEARLY, `YYYY-MM` for
   MONTHLY (`erp-core/src/main/java/com/erp/sequence/domain/ResetPolicy.java:19-25`). The allocation creates
   the new period's row from the anchor's configuration with `nextValue = 1`
   (`sequence/mapper/NumberSeriesMapper.java:30-43`; `sequence/service/NumberAllocationService.java:67-73`).
2. **Code, reset policy and period key are immutable** (`updatable = false`,
   `sequence/entity/NumberSeries.java:61`, `:76`, `:81`); the update body carries only `prefix` and
   `pattern` (`sequence/dto/NumberSeriesUpdateRequest.java:11-13`). Changing the policy of a live series
   would make existing period rows meaningless.
3. **Configuration is kept identical across the rows of a code**: update, activate and deactivate address
   any row and apply to every row of the code (`sequence/service/NumberSeriesService.java:101-141`;
   `NumberSeriesRepository.findAllByCodeOrderByIdAsc`).
4. **The pattern must carry the period**: `NumberPattern.assertDistinctUnder(policy)` — YEARLY needs
   `{YYYY}` or `{YY}`, MONTHLY needs a year token and `{MM}` — checked on create and on every pattern
   update against the immutable policy (`sequence/domain/NumberPattern.java:110-125`;
   `sequence/domain/NumberSeriesDomain.java:37-43`, `:52-54`); the failure is `SEQUENCE_PATTERN_INVALID`,
   whose message names the rule (`erp-core/src/main/resources/i18n/messages.properties:167`).

The default policy is YEARLY and the default pattern `{PREFIX}-{YYYY}-{SEQ:6}` carries `{YYYY}`, so the
defaults satisfy the rule (`sequence/entity/NumberSeries.java:51`, `:78`).

## Consequences
- The admin API shows **period rows**, not series: `GET /{id}` and `POST /search` return one row per
  period (`periodKey`, `nextValue`), and the response of an update is the addressed row
  (`docs/api-docs/sequence/endpoints/number-series-management.md`). A UI that wants "the series" groups by
  `code`.
- A tenant provisioned later receives only each code's **anchor row** (the lowest id), with the counter at
  1, not every historical period (`sequence/tenant/SequenceTenantProvisioningContributor.java:34-42`;
  ADR-SEQUENCE-003).
- A new period's row is `CREATE`-audited like any row (the counter itself is not), so the audit log shows
  when a series rolled over (docs/DEVIATIONS.md [10] rebase entry).
- Adding a reset policy (e.g. DAILY) is a core schema change (`CHK_CORE_NUMBER_SERIES_RESET`), an enum
  constant, a period-key width check (`PERIOD_KEY VARCHAR(7)`) and a distinctness rule — by design, never a
  data-only change.
- A NEVER series has exactly one row for ever; its `PERIOD_KEY` is the empty string, never NULL, so the
  unique key holds.

## Traces
ENT-SEQUENCE-001 · REQ-SEQUENCE-001, REQ-SEQUENCE-003, REQ-SEQUENCE-008, REQ-SEQUENCE-009,
REQ-SEQUENCE-011 · RULE-SEQUENCE-002, RULE-SEQUENCE-004, RULE-SEQUENCE-010, RULE-SEQUENCE-014 ·
POL-SEQUENCE-002, POL-SEQUENCE-003 · DBF-SEQUENCE-003, DBF-SEQUENCE-005, DBF-SEQUENCE-006,
DBF-SEQUENCE-007 · docs/DEVIATIONS.md [09] (row model; extra pattern validation); docs/steps/09-report.md
"Decisions & deviations" 3, 4
