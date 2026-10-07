<!-- source: PHASE:F3 / SUB:F3-SCR-MDL-001 -->
<!-- context: F3-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-006, AC-MDL-007, AC-MDL-008, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-005, API-MDL-006, API-MDL-007, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-010, SCR-MDL-001, UXD-MDL-001 -->
<!-- SUB:F3-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-006,AC-MDL-007,AC-MDL-008,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-005,API-MDL-006,API-MDL-007 -->
### F3 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

Validation timing for this screen, declared once and holding for both entry surfaces: **on blur
for the unique key and the unique code, on submit for everything else.**

#### F3-FIELD — SCR-MDL-001 (type · create)
key             · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK · on blur
ownerModuleCode · REQUIRED · LENGTH (maxLength 10) · MEMBER_OF the UXD-MDL-001 list ·
                  BUSINESS_RULE (RULE-MDL-001) · on submit
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (type · edit)
key, ownerModuleCode · read-only — not inputs at all; the update request carries neither
nameAr, nameEn       · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (value · create)
code            · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK within the selected type
                  (RULE-MDL-002) · on blur
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

#### F3-FIELD — SCR-MDL-001 (value · edit)
code            · read-only — the update request does not carry it
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

UNIQUE_CHECK    : async, on blur — both backend filters are LIKE, never EQUALS (QR-MDL-013's
                  `key LIKE :key`, QR-MDL-005's `code LIKE :code`; PHASE 1's Search contract and
                  SRS §B2 agree), so the check requests the LIKE filter the backend actually
                  declares and then asserts exact string equality client-side over the returned
                  rows before showing the inline message: the type's `key` through API-MDL-001
                  (LIKE `key`, then filter the response for an exact match) — the value's `code`
                  through API-MDL-005 scoped to `lookupTypeId` (LIKE `code`, then filter the
                  response for an exact match), so the check's scope is the rule's scope. This
                  uses only the published surface and is correct whether or not the service ever
                  honours EQUALS (G6). Neither blocks submit on its own: `MDL-409-TYPE-DUP` and
                  `MDL-409-VALUE-DUP` from the server are the authority, routed inline to the
                  same field. On edit neither field is an input, so neither check runs.

#### F3-VALIDATION — RULE-MDL-001   traces=REQ-MDL-002,AC-MDL-002
Statement : The system shall reject a lookup type registration whose owner module code has no
            ModuleRegistry row in the security module.
Message   : catalog code `MDL-409-MODULE-NOT-REGISTERED` —
            ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
            en: "The owning module is not registered in the Security module"
Scope     : CREATE · Field : ownerModuleCode · kind BUSINESS_RULE · when submit
Shape     : the control is a select over the registered module codes loaded through UXD-MDL-001,
            so the common case cannot be typed wrong at all; the validator asserts that the
            submitted value is one the runtime-loaded list contains, never that it is one of a
            static set. The server stays the authority — a module deregistered between load and
            submit is caught there — and the catalog message routes to this field. When the
            foreign read is refused the select is empty and disabled and create is disabled
            behind it, rather than falling back to free text (ADR-MDL-013). The read-only
            master-list filter beside this control degrades differently — to the current search
            response's own values, never disabled — because nothing is submitted through a
            filter (ADR-MDL-016, G1).

#### F3-VALIDATION — RULE-MDL-002   traces=REQ-MDL-007,AC-MDL-007
Statement : The system shall reject a lookup value whose code already exists under the same
            lookup type.
Message   : catalog code `MDL-409-VALUE-DUP` — ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
            en: "This code is already used within this type"
Scope     : CREATE (and UPDATE, per the rule's trigger) · Field : the value's code ·
            kind UNIQUE_CHECK · when blur, and again on submit by the server
Shape     : uniqueness is scoped to the parent type, never globally — the same code under
            another type is legitimate, and a global check would reject a value the server
            accepts. The async check is bound to the selected parent id. On edit the field is
            read-only, so the client check cannot fire and the rule is the server's alone. A
            deactivated value under the same type can be the one holding a submitted code — no
            activate endpoint exists (ADR-MDL-005), so the code stays reserved — and the inline
            message names that possibility (G7, F2-QUERY VALUE CREATE).

#### F3-VALIDATION — RULE-MDL-003   traces=REQ-MDL-003,AC-MDL-003
Statement : The system shall prevent editing a lookup type's key after creation.
Message   : the rule's own text — ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» ·
            en: "A lookup type's key cannot be changed after creation"
Scope     : UPDATE · Field : key · kind BUSINESS_RULE · not a form check at all
Shape     : there is **nothing to validate** — `key` is not an input on edit, because the update
            request does not carry it. The rule is expressed by the absence of the field rather
            than by a message on a control that would refuse. The form still states the rule
            beside the read-only key, so an editor learns why it cannot be changed instead of
            meeting a disabled control with no explanation.

#### F3-VALIDATION — RULE-MDL-004   traces=REQ-MDL-004,AC-MDL-004
Statement : While a lookup type is inactive, the system shall exclude its values from consumer
            reads.
Message   : the rule's own text — ar: «هذا النوع معطّل حاليًا» ·
            en: "This lookup type is currently inactive"
Scope     : the deactivate action (API-MDL-004) · Field : none — a row action ·
            kind BUSINESS_RULE · when submit
Shape     : **not a validation this form performs** — it is a consequence the deactivate
            confirmation names before the act: every consuming module stops receiving this
            type's values, and — because no activate endpoint exists — the type's `key` stays
            reserved under the platform and cannot be reused by a later registration (G7). The
            rule's text is also the state label on an inactive type row, so the same words
            explain the row and the warning. Nothing on this screen is hidden by it: the
            manager's value list still shows the values, which is the difference between this
            screen and a consumer.

Business-code fields: `key` and `code` are client-chosen strings, not platform-numbered, and both
are read-only after create per the two update DTOs. Neither is generated or predicted on the
client (SRS §3.3 numbering).
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without CREATE, UPDATE or DELETE receives `ACCESS_DENIED`
on submit and the form shows the localized message; fields are not pre-emptively disabled,
because no published surface tells this screen which actions its caller holds (ADR-MDL-012,
PF-MDL-001).
<!-- SUB:F3-SCR-MDL-001:END -->
