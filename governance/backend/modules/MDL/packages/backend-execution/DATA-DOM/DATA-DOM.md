<!-- source: PHASE:DATA-DOM -->
<!-- traces: REQ-MDL-001, REQ-MDL-006 -->
<!-- PHASE:DATA-DOM:START traces=REQ-MDL-001,REQ-MDL-006 -->
## PHASE 2 — DATA-DOM

Entity count is 2 — below the engine's self-check split threshold; no SUB is opened, both
entities are written flat in profile.vocabulary order (LookupType first, as the master).

#### ENT-MDL-001 — LookupType      kind: master
BINDINGS: table `MDL_LOOKUP_TYPE` · PK `lookupTypePk` (DBF-MDL-001) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none (§3.3 test: no)
DEFAULT FIELDS (profile.conventions.entity_defaults.master): nameAr, nameEn, code, isActiveFl,
createdBy, createdAt, updatedBy, updatedAt — here `key` plays the role of `code` (SRS A3 note); `ownerModuleCode` is an addition beyond the default set, required by the plan's namespacing rule (POL-MDL-002).
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-MDL-001 | lookupTypePk | lookup_type_pk | Long | NOT NULL | Yes | PK_MDL_LOOKUP_TYPE | معرّف نوع اللوكب / LookupType id |
| DBF-MDL-002 | key | key | String | NOT NULL | create-only | UQ_MDL_LOOKUP_TYPE_KEY | المفتاح / Key |
| DBF-MDL-003 | ownerModuleCode | owner_module_code | String | NOT NULL | create-only | — (XM-MDL-001 app-level check) | رمز الوحدة المالكة / Owner module code |
| DBF-MDL-004 | nameAr | name_ar | String | NOT NULL | No | — | الاسم (عربي) / Name (Arabic) |
| DBF-MDL-005 | nameEn | name_en | String | NOT NULL | No | — | الاسم (إنجليزي) / Name (English) |
| DBF-MDL-006 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-MDL-007..010 | createdBy/createdAt/updatedBy/updatedAt | created_by/… | String/Instant | see db-script | Yes | — | audit |
DTO MEMBERSHIP: create-request `{key, ownerModuleCode, nameAr, nameEn}`; update-request `{nameAr, nameEn}` only (key and ownerModuleCode immutable — RULE-MDL-003); response includes all.
LOOKUP FIELDS: none (LookupType is not itself lookup-backed).
DOMAIN RULES:
**RULE-MDL-001** — Scope ENT-MDL-001 · Trigger: on create · Statement: "The system shall reject a lookup type registration whose owner module code has no ModuleRegistry row in SEC." · Message ar: "الوحدة المالكة غير مسجّلة في وحدة الأمان" / en: "The owning module is not registered in the Security module" · DB enforcement: application layer (service, via QR-MDL-012, XM-MDL-001) · owner layer: service.
**RULE-MDL-003** — Scope ENT-MDL-001 · Trigger: on update · Statement: "The system shall prevent editing a lookup type's key after creation." · Message ar: "لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه" / en: "A lookup type's key cannot be changed after creation" · DB enforcement: application layer (enforced by omission — `key` is absent from the update DTO entirely) · owner layer: service/controller (DTO shape).
**RULE-MDL-004** — Scope ENT-MDL-001 · Trigger: on evaluate (consumer read, API-MDL-011) · Statement: "While a lookup type is inactive, the system shall exclude its values from consumer reads." · Message ar: "هذا النوع معطّل حاليًا" / en: "This lookup type is currently inactive" · DB enforcement: application layer (service, via QR-MDL-015 + QR-MDL-011's join filter) · owner layer: service.
STATE MACHINE: `isActiveFl` binary only — not applicable (SRS A7).
CROSS-MODULE: XM-MDL-001 (SOFT-READ → SEC_MODULE_REG, status ACTIVE) touches `ownerModuleCode`.
REPOSITORY OPS → QR-MDL-001 (FIND_BY_CRITERIA), QR-MDL-002 (SAVE), QR-MDL-003 (UPDATE), QR-MDL-004 (UPDATE, deactivate), QR-MDL-010 (FIND_BY_CRITERIA, grouped), QR-MDL-012 (EXISTS, cross-module), QR-MDL-013 (EXISTS, uniqueness), QR-MDL-015 (FIND_ONE, by key).

#### ENT-MDL-002 — LookupValue      kind: lookup
BINDINGS: table `MDL_LOOKUP_VALUE` · PK `lookupValuePk` (DBF-MDL-011) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none
DEFAULT FIELDS (profile.conventions.entity_defaults.lookup): code, nameAr, nameEn, sortOrder, isActiveFl — matched exactly, plus PK/FK/audit.
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-MDL-011 | lookupValuePk | lookup_value_pk | Long | NOT NULL | Yes | PK_MDL_LOOKUP_VALUE | معرّف قيمة اللوكب / LookupValue id |
| DBF-MDL-012 | lookupTypeId | lookup_type_id | Long | NOT NULL | create-only | FK_LOOKUP_VALUE_TYPE | نوع اللوكب / Lookup type |
| DBF-MDL-013 | code | code | String | NOT NULL | create-only | UQ_MDL_LOOKUP_VALUE_TYPE_CODE | الرمز / Code |
| DBF-MDL-014 | nameAr | name_ar | String | NOT NULL | No | — | الاسم (عربي) / Name (Arabic) |
| DBF-MDL-015 | nameEn | name_en | String | NOT NULL | No | — | الاسم (إنجليزي) / Name (English) |
| DBF-MDL-016 | sortOrder | sort_order | Integer | NOT NULL | No | — | ترتيب العرض / Sort order |
| DBF-MDL-017 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-MDL-018..021 | createdBy/createdAt/updatedBy/updatedAt | … | — | see db-script | Yes | — | audit |
DTO MEMBERSHIP: create-request `{lookupTypeId, code, nameAr, nameEn, sortOrder}`; update-request `{nameAr, nameEn, sortOrder}` (lookupTypeId, code immutable); response includes all.
LOOKUP FIELDS: none — LookupValue rows are themselves the values other modules resolve; they hold no lookup-backed field of their own.
DOMAIN RULES: **RULE-MDL-002** — Scope ENT-MDL-002 · Trigger: on create · Statement: "The system shall reject a lookup value whose code already exists under the same lookup type." · Message ar: "هذا الرمز مستخدم بالفعل ضمن هذا النوع" / en: "This code is already used within this type" · DB enforcement: `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` (structural) + service pre-check (QR-MDL-014, friendly error before the DB would reject it) · owner layer: service + database.
STATE MACHINE: `isActiveFl` binary only — not applicable.
CROSS-MODULE: none.
REPOSITORY OPS → QR-MDL-005 (FIND_BY_CRITERIA), QR-MDL-006 (SAVE), QR-MDL-007 (UPDATE), QR-MDL-008 (UPDATE, deactivate), QR-MDL-009 (UPDATE batch, reorder), QR-MDL-011 (FIND_BY_CRITERIA, consumer read), QR-MDL-014 (EXISTS, uniqueness).
<!-- PHASE:DATA-DOM:END -->
