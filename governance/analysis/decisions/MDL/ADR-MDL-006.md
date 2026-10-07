# ADR-MDL-006 — `isActiveFl` is read-only on the value form, although SRS B3 lists it as an input

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`srs-mdl.md` SCR-REQ-MDL-001 §B3 reads: "Detail fields: code, nameAr, nameEn, sortOrder
(drag-to-reorder), isActiveFl (ENT-MDL-002)."

The published write DTOs carry neither `isActiveFl` nor, on update, `code`:

| DTO | Fields |
|---|---|
| `LookupValueCreateRequest` | code, nameAr, nameEn, sortOrder — all required |
| `LookupValueUpdateRequest` | nameAr, nameEn, sortOrder — all required |
| `LookupTypeCreateRequest` | key, ownerModuleCode, nameAr, nameEn — all required |
| `LookupTypeUpdateRequest` | nameAr, nameEn — all required |

So `isActiveFl` is settable by no form at either level; it is changed by the deactivate
endpoints (API-MDL-004, API-MDL-008) alone. `code` and `key` are absent from both update
requests — `key` by RULE-MDL-003 (immutable after creation) and `code` by the same shape,
though no MDL rule states it.

## Decision
`isActiveFl` is in the models as a **read-only** property at both levels, displayed as the
row's state and never rendered as a form control. `code` (value) and `key` and
`ownerModuleCode` (type) are editable on create and read-only on edit, matching the published
update requests exactly.

The SRS B3 line stays accurate about the other four fields and is narrowed, here and in
`ui-ux-spec-mdl.md` → SCR-MDL-001, for `isActiveFl`.

## Consequences
- The only way a row's active flag changes from this screen is the Deactivate affordance, which
  has no counterpart (ADR-MDL-005) — so the state is one-way and the confirmation says so.
- RULE-MDL-003's key immutability is expressed twice over: the field is not an input on edit,
  and the server would refuse it anyway. The screen states the rule rather than relying on the
  field's absence to imply it.
- `sortOrder` stays a real input on the value form **and** is what the drag-to-reorder gesture
  writes through API-MDL-009; the two paths write the same field and the form shows the value
  the reorder produced.
- No `REQ-*` changes.
