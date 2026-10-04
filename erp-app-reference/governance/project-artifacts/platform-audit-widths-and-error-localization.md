# Platform — audit-column widths and `GlobalExceptionHandler` localization

Two issues surfaced during FIN's ALIGN-BE work on 2026-09-12 that are **not FIN's to own**.
Both sit in `com.erp.common.*` / `src/main/resources/db/migration/` and affect SEC, MDL, FIN,
CU, NOTIF and FILE identically. FIN's `execution-state.json` carries them as `api_doc_gaps[]`
entries so they are not lost from the module record, but neither can be closed by a module
session — hence this note.

Every number, string and line reference below was re-derived by opening the file on
2026-09-12. Nothing is carried from a prior document. Where a claim is quoted, the quote was
read at the cited line; where a count is given, the command that produced it is named.

Companion records:
- `governance/modules/FIN/execution-state.json` → `api_doc_gaps[]`, the entries whose
  `endpoint` begins `DB Alignment Manifest (§4)` and `common/web/GlobalExceptionHandler`.
- The `MANIFEST (§4)`, `ERROR ENVELOPE` and `PLATFORM I18N` lines of the ALIGN self-check in
  `governance/modules/FIN/packages/backend-execution/_SECTIONS.md` (mirrored verbatim in
  `governance/modules/FIN/P3_1/backend-execution-plan-fin.md`).

---

## Part 1 — `created_by` / `updated_by` are two different widths across the platform

### The split, as built

Counted by grepping `(created_by|updated_by)[[:space:]]+VARCHAR` across
`src/main/resources/db/migration/` and then discarding the eight `SEC_*` tables created by
`V2__sec_security_schema.sql`, every one of which
`V14__drop_legacy_security_schema.sql:9-19` drops. What remains is 22 live tables — exactly
the 22 classes returned by `grep -rl "extends AuditableEntity" src/main/java/com/erp/`.

**16 live tables at `VARCHAR(100)`** (SEC 5 · MDL 2 · FIN 9):

| Table | Migration | `created_by` : `updated_by` |
|---|---|---|
| `SEC_USER` | `V16__sec_schema.sql` | 52 : 54 |
| `SEC_ROLE` | `V16__sec_schema.sql` | 66 : 68 |
| `SEC_MODULE_REG` | `V16__sec_schema.sql` | 78 : 80 |
| `SEC_SCREEN_REG` | `V16__sec_schema.sql` | 113 : 115 |
| `SEC_ACTION_REG` | `V16__sec_schema.sql` | 127 : 129 |
| `MDL_LOOKUP_TYPE` | `V18__mdl_sequences.sql` | 37 : 39 |
| `MDL_LOOKUP_VALUE` | `V18__mdl_sequences.sql` | 54 : 56 |
| `FIN_DIMENSION` | `V22__fin_schema.sql` | 52 : 54 |
| `FIN_ACCOUNT` | `V22__fin_schema.sql` | 68 : 70 |
| `FIN_FISCAL_YEAR` | `V22__fin_schema.sql` | 81 : 83 |
| `FIN_EVENT_TYPE_RULE` | `V22__fin_schema.sql` | 93 : 95 |
| `FIN_RECURRING_TEMPLATE` | `V22__fin_schema.sql` | 109 : 111 |
| `FIN_ALLOCATION_RULE` | `V22__fin_schema.sql` | 121 : 123 |
| `FIN_DIMENSION_VALUE` | `V22__fin_schema.sql` | 137 : 139 |
| `FIN_FISCAL_PERIOD` | `V22__fin_schema.sql` | 154 : 156 |
| `FIN_JOURNAL_ENTRY` | `V22__fin_schema.sql` | 174 : 176 |

**6 live tables at `VARCHAR(255)`** (CU 1 · NOTIF 3 · FILE 2):

| Table | Migration | `CREATED_BY` : `UPDATED_BY` |
|---|---|---|
| `CU_APP_CONFIGURATION` | `V1__cu_app_configuration_schema.sql` | 25 : 27 |
| `NOTIF_TEMPLATE` | `V6__notif_schema.sql` | 30 : 32 |
| `NOTIF_CHANNEL_CONFIG` | `V6__notif_schema.sql` | 41 : 43 |
| `NOTIF_LOG` | `V6__notif_schema.sql` | 62 : 64 |
| `FILE_CATEGORY` | `V8__file_schema.sql` | 26 : 28 |
| `FILE_DOCUMENT` | `V8__file_schema.sql` | 47 : 49 |

The 100-width group is `NOT NULL`; the 255-width group is nullable. The 255 group is also the
older one (V1/V6/V8, the Legacy Path modules), the 100 group the newer (V16/V18/V22).

### What was changed on 2026-09-12, and what was not

`src/main/java/com/erp/common/domain/AuditableEntity.java` previously declared both columns
`length = 255`, which contradicted 16 of the 22 physical tables. It was narrowed to match the
**narrowest** live width:

- `AuditableEntity.java:26` — `@Column(name = "CREATED_BY", updatable = false, length = 100)`
- `AuditableEntity.java:32` — `@Column(name = "UPDATED_BY", length = 100)`

with an in-file comment recording the reasoning so it is not "tidied" back to 255.

**No migration was written and the physical schema was not touched.** The split above is
exactly as it was before the change. This is a declaration fix only.

### Why the change is runtime-inert

`src/main/resources/application.properties:21` reads:

```
spring.jpa.hibernate.ddl-auto=none
```

immediately under the comment at line 20, *"Schema is owned by Flyway, so Hibernate never
generates DDL (ddl-auto=none)."* With `none`, Hibernate neither generates DDL from
`@Column(length = ...)` nor validates it against the live column, and it does not truncate
values on write. The annotation is therefore documentation-of-intent at runtime; changing 255
to 100 cannot alter observable behaviour on any environment. (It *would* start mattering the
day anyone switched to `validate` — at which point the 6 wide tables would become the
mismatch, not the 16 narrow ones.)

### Why 100 is the true upper bound anyway

The value written into these columns is chased from the listener down to the schema:

1. `com/erp/common/audit/AuditEntityListener.java` — `@PrePersist`/`@PreUpdate` set both
   columns from `SecurityContextHelper.getCurrentUsername()`.
2. `com/erp/common/util/SecurityContextHelper.java` — returns
   `authentication.getName()`, or the literal `"system"` when there is no authenticated
   principal.
3. `com/erp/sec/security/JwtAuthenticationFilter.java:79` takes `claims.getSubject()` as
   `username`, and `:85` requires `userRepository.findByUsername(username)` to resolve before
   `:99`/`:102` construct the `UsernamePasswordAuthenticationToken` with that same `username`
   as its principal. So `getName()` is never an arbitrary token claim — it is always a row's
   `SEC_USER.username`.
4. `src/main/resources/db/migration/V16__sec_schema.sql:44` —
   `username       VARCHAR(100)  NOT NULL`.

So every value is either a `SEC_USER.username` (≤ 100 by schema) or the 6-character literal
`"system"`. The 255-width columns are over-provisioned; they are not carrying wider data.

### What a human still owes

Nothing is broken and nothing is urgent. The open question is whether to reconcile the
physical schema, and it is a platform call because it spans three modules that no FIN, SEC or
MDL session owns:

- **Option A — leave it.** Runtime-correct today, costs nothing, but `ddl-auto=validate` stays
  unavailable and the inconsistency will keep being re-discovered by every alignment sweep.
- **Option B — one forward migration** narrowing the 12 CU/NOTIF/FILE columns (6 tables ×
  `created_by` + `updated_by`) to `VARCHAR(100)`. Safe by the chain above (no stored value can exceed 100), but it rewrites
  columns in three modules' tables and needs whoever owns CU/NOTIF/FILE to agree.

Do **not** resolve it by widening `AuditableEntity` back to 255 — that re-breaks the 16-table
majority to accommodate 6 over-provisioned columns.

### Re-verification

Every figure in Part 1 was re-measured on 2026-09-12 by the final governance-state pass and none
of it moved: the same 16 tables at `VARCHAR(100)` and 6 at `VARCHAR(255)` at the same migration
line numbers, `AuditableEntity.java:26` and `:32` both still `length = 100`,
`application.properties:21` still `ddl-auto=none`, and
`grep -rl "extends AuditableEntity" src/main/java/com/erp/` still returning exactly 22 classes.
No migration has been written for the split, and none is proposed here. Part 1 stands as written.

---

## Part 2 — `GlobalExceptionHandler` localizes five codes out of six

All line references are `src/main/java/com/erp/common/web/GlobalExceptionHandler.java`, re-read
at its state on 2026-09-12 **after** the localization work landed. An earlier revision of this
section described the state before that work and said "two codes out of five"; it was accurate
when written and is superseded here rather than left standing.

### Current state, handler by handler

| Handler | Lines | Code on the wire | Message source | Bundle entry (en / ar) |
|---|---|---|---|---|
| `LocalizedException` | 32-52 | the exception's own `errorCode` | **`resolveMessage(...)` → `MessageSource`** | per-code, e.g. all 40 `FIN-*` keys |
| `MethodArgumentNotValidException` | 54-68 | `CommonErrorCodes.VALIDATION_ERROR` | **`resolveMessage(...)` → `MessageSource`** | `messages.properties:17` / `messages_ar.properties:14` |
| `HttpMessageNotReadableException` | 79-87 | `CommonErrorCodes.VALIDATION_ERROR` | **hardcoded** `"The request body is malformed or does not match the expected structure"` | — shares the `VALIDATION_ERROR` key, which carries the *other* message |
| `DataIntegrityViolationException` | 89-97 | `CommonErrorCodes.DATA_INTEGRITY_VIOLATION` | **`resolveMessage(...)` → `MessageSource`** | `messages.properties:18` / `messages_ar.properties:15` |
| `AccessDeniedException` | 99-106 | `CommonErrorCodes.ACCESS_DENIED` | **`resolveMessage(...)` → `MessageSource`** | `messages.properties:16` / `messages_ar.properties:13` |
| `Exception` (catch-all) | 108-116 | `CommonErrorCodes.INTERNAL_ERROR` | **`resolveMessage(...)` → `MessageSource`** | `messages.properties:15` / `messages_ar.properties:12` |

`resolveMessage(String, Object[])` is the private helper at `:118-126`: it reads
`LocaleContextHolder.getLocale()`, calls `messageSource.getMessage(...)`, and on
`NoSuchMessageException` logs a warning and falls back to returning the code itself.

All four codes now exist as constants — `CommonErrorCodes.java:9` `VALIDATION_ERROR`, `:10`
`INTERNAL_ERROR`, `:11` `ACCESS_DENIED`, `:12` `DATA_INTEGRITY_VIOLATION` — and every handler uses
the constant rather than a string literal. The "cosmetic" note in the earlier revision is closed.

The practical consequence: an Arabic-locale caller now gets Arabic for a `LocalizedException`, for
a 403, for a 409 constraint violation, for a 500, and for a bean-validation 400 — and English for
exactly one response, a malformed request body.

### What changed on 2026-09-12

Four handlers, in two waves.

- **`AccessDeniedException`** moved first, from `.code("ACCESS_DENIED")` /
  `.message("You do not have permission to perform this operation")` to the constant plus
  `resolveMessage(...)`. The English bundle text is **byte-identical** to the string it replaced,
  so no English-facing response moved; only Arabic callers saw a difference. That property is what
  made it safe to land without asking anyone.
- **`MethodArgumentNotValidException`** and **`DataIntegrityViolationException`** followed. Neither
  had a bundle entry in either language before; keys were minted in both
  (`VALIDATION_ERROR` = "Validation failed", `DATA_INTEGRITY_VIOLATION` = "The request could not be
  completed because it violates a data constraint"), each matching the string the handler had been
  hardcoding, so again no English text moved.
- **`Exception` (catch-all)** also followed, and **this one did move English text** — see below.

### The `INTERNAL_ERROR` wording conflict: decided, in favour of the bundle

The earlier revision left this open as the one item a human owed, because the handler hardcoded
`An unexpected error occurred` while `messages.properties:15` said
`An unexpected error occurred. Please try again later.` — two different English strings under one
code, so routing it through `resolveMessage` could not be a no-op.

**It was decided as option (i): adopt the bundle text.** `handleUnexpected` now resolves through
`MessageSource`, so a 500 body in English reads *"An unexpected error occurred. Please try again
later."* where it previously read *"An unexpected error occurred."*

Recorded plainly rather than glossed: **English 500 bodies changed.** That is a deliberate
divergence from the byte-identical precedent the `ACCESS_DENIED` change set, and anyone comparing
500 responses across that date should expect the difference rather than treat it as drift. The
Arabic (`messages_ar.properties:12`) was already in place and is unchanged.

### The one remaining residue: a malformed request body

`handleMalformedRequestBody` (`:79-87`) still hardcodes English. Its own javadoc at `:70-78` now
states why, and the reason is structural rather than an omission:

> it emits the SAME wire code as `handleValidation` (`VALIDATION_ERROR`) but a DIFFERENT message,
> and one bundle key cannot carry two texts.

The `VALIDATION_ERROR` key is registered with the generic `"Validation failed"` text that
`handleValidation` needs. Localizing this second sentence would mean either overwriting that text
or giving this response its own wire code.

**So closing it is a contract change, not a localization change.** The two options:

- (i) mint a distinct code — e.g. `MALFORMED_REQUEST_BODY` — with its own `en`/`ar` pair. Cleanest,
  but any client branching on `VALIDATION_ERROR` for a 400 now sees a second code.
- (ii) collapse the two responses onto one message, accepting that a malformed body and a failed
  field validation read identically.

Until one is chosen, an Arabic caller receives English for a malformed request body, and only for
that. This is the `PLATFORM I18N` residue in FIN's ALIGN self-check.

### Part 2a — a FIN 403 no longer looks like a platform 403

A change that post-dates the original note and belongs here because it changes the wire:
`src/main/java/com/erp/fin/security/FinForbiddenAdvisor.java` is new.

It is a `DefaultPointcutAdvisor` registered as an infrastructure-role `@Component` at
`Ordered.HIGHEST_PRECEDENCE`. Its pointcut matches any target class whose name starts with
`com.erp.fin.service.`; its interceptor catches `AccessDeniedException` out of
`invocation.proceed()` and re-throws
`new LocalizedException(Status.FORBIDDEN, FinErrorCodes.FIN_403_FORBIDDEN)`, which the
`LocalizedException` handler above then renders through `MessageSource` like any other FIN code.

So the statement in the earlier revision — that `FIN-403-FORBIDDEN` "reaches no caller — it exists
in no `FinErrorCodes` constant and in neither bundle" — **is no longer true**. All three now exist:
`FinErrorCodes.java:400`, `messages.properties:173`, `messages_ar.properties:170`. A denial on a FIN
service answers `FIN-403-FORBIDDEN`, localized; a denial on any module without an advisor of its own
still answers the platform `ACCESS_DENIED`.

Those three line numbers are **not** the ones an earlier revision of this section carried
(`:384` / `:172` / `:169`). Each shifted later on 2026-09-12 when two further FIN codes were
registered — `FIN-409-NOT-ACTIVE` (`FinErrorCodes.java:441`, the run gate on a deactivated
recurring template or allocation rule) and `FIN-422-INVALID-PERCENTAGE-VALUE`
(`FinErrorCodes.java:357`) — taking FIN from 38 registered codes to 40, matched key-for-key in
both bundles. They are restated here at the values read today; the old ones are recorded rather
than silently overwritten, because carrying a line reference forward without re-opening the file
is the specific failure this note exists to avoid.

Three boundaries, named because the change is real but not unlimited:

1. **Only service-layer denials.** A Spring Security *filter-chain* denial never reaches an AOP
   proxy or `GlobalExceptionHandler` at all — it is written directly by `SecSecurityErrorHandler`,
   which `SecurityConfig.java:59` wires as `.accessDeniedHandler(securityErrorHandler)` and which
   answers `SEC-403-FORBIDDEN` (`SecSecurityErrorHandler.java:42`). That path is currently
   unreachable for FIN: `SecurityConfig.java:56` authorizes with `.anyRequest().authenticated()`
   and declares no FIN authority rule, so every FIN permission denial is in fact a `@PreAuthorize`
   denial on a FIN service. Add one URL-level authority rule for a FIN path and that stops being
   true.
2. **`FIN-403-SOD-VIOLATION` is unaffected by the advisor** — but for a different reason than
   an earlier revision of this section gave, and that reason is now the whole story. It used to
   say the code "is thrown as a `LocalizedException` directly
   (`FinSeparationOfDutiesService.java:109`, `FiscalPeriodDomain.java:112`), never as an
   `AccessDeniedException`". **Neither throw site exists any more.** On 2026-09-12
   `FinSeparationOfDutiesService` was deleted by a recorded human decision, taking
   `FiscalPeriodDomain.assertCanHardClose(...)` with it: the service had enforced global
   *user-set disjointness* — refusing the period close for every caller whenever any single user
   in the system held both `PERM_FIN_PERIODS_CLOSE_APPROVE` and
   `PERM_FIN_JOURNAL_ENTRIES_CREATE` — where RULE-FIN-015 asks only that the two *permissions* be
   distinct, which the delivered `@PreAuthorize` already satisfies. Re-measured today:
   `grep -rn 'FIN_403_SOD_VIOLATION' src/main/java/` returns exactly two hits, the constant at
   `FinErrorCodes.java:78` and a `{@link}` javadoc reference at `:392` — **no `throw` anywhere**.
   So the advisor still never sees the code, but only because nothing raises it: it is
   unreachable, its Error Catalog row has been struck, and its constant plus both bundle entries
   (`messages.properties:142`, `messages_ar.properties:139`) were deliberately left in place.
   That mismatch is tracked as its own `api_doc_gaps` entry in FIN's `execution-state.json` and
   as the `SOD DEAD CODE` line of FIN's ALIGN self-check. Nothing in this note authorises
   restoring the check — `FiscalPeriodDomain.java:110-117` forbids it explicitly.
3. **This is a per-module opt-in, not a platform change.** `com.erp.sec.security.SecForbiddenAdvisor`
   is the precedent FIN copied; MDL, CU, NOTIF and FILE have no such advisor, so their 403s are
   still `ACCESS_DENIED`. Whether every module should get one — or whether the shared handler should
   do it centrally — is still an open platform question, just no longer a FIN-blocking one.

### Re-verification

Every figure in Parts 2 and 2a was re-measured on 2026-09-12 by the final governance-state pass,
against a `GlobalExceptionHandler.java` of 127 lines. **The handler table is unmoved**: the six
`@ExceptionHandler` methods still begin at `:32`, `:54`, `:79`, `:89`, `:99` and `:108`, and
`resolveMessage` is still the private helper at `:118-126`. Five of the six still resolve through
`MessageSource`; `handleMalformedRequestBody` still hardcodes its English sentence at `:84` while
emitting `CommonErrorCodes.VALIDATION_ERROR` at `:83`, so the residue is unchanged and remains a
contract question, not a localization one. The four platform keys are still at
`messages.properties:15-18` and `messages_ar.properties:12-15`, and the four constants still at
`CommonErrorCodes.java:9-12`.

**What did move is entirely inside Part 2a, and only line references**: the two new FIN codes
pushed `FIN-403-FORBIDDEN` down three lines in `FinErrorCodes.java` and one line in each bundle,
and the `FIN-403-SOD-VIOLATION` throw sites named under boundary 2 ceased to exist. Both are
corrected above rather than left standing. No handler was added, removed or re-routed; the
`FinForbiddenAdvisor` mechanism described in Part 2a is byte-for-byte the one in the tree today
(pointcut prefix at `:40`, the re-throw at `:54`, `Ordered.HIGHEST_PRECEDENCE` at `:57`).

### Scope note

None of Part 2 is FIN-specific. `GlobalExceptionHandler` is a single `@RestControllerAdvice` in
`com.erp.common.web`, so it affects SEC, MDL, FIN, CU, NOTIF and FILE identically, and no
module-level ALIGN session can close the remaining residue. Part 2a is the exception that proves
it: FIN could only change its *own* 403 by adding a module-local advisor, precisely because it
could not change the shared handler.
