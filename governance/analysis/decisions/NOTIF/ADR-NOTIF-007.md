# ADR-NOTIF-007 — RULE-NOTIF-007 widened: a recipient may receive notifications when ACTIVE or PENDING_VERIFICATION, in either realm

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — steps 06, 08, 14)
Status  : ACCEPTED (non-breaking)

## Context
RULE-NOTIF-007 (the consumer-side application of OQ-SEC-001) says NOTIF must not dispatch to a recipient
whose user account is inactive, keeping history; at analysis time "active" meant `USER_STATUS = ACTIVE` and
the only accounts were staff accounts. Step 06 added the CUSTOMER realm: a self-registered customer is
created `PENDING_VERIFICATION` and must receive the `CUSTOMER_VERIFY_EMAIL` mail before it can ever be
`ACTIVE`; the password-reset completion also verifies such an account. Step 14 made SEC's
`SecUserDirectoryApi.findContact` realm-neutral so that customers can be addressed at all.

## Decision
Recipient eligibility is SEC's `UserContact.active`, which is true for `ACTIVE` and for
`PENDING_VERIFICATION` and false for `PENDING` (bootstrap admin before activation) and `DISABLED`.
Recipients of both realms share one `SEC_USER` id space, so `NOTIF_LOG.RECIPIENT_ID` and
`NOTIF_INBOX.RECIPIENT_USER_ID` may name a staff or a customer account. NOTIF reads this through its own
`RecipientDirectory` port (`isActive`, `emailOf`, `currentRecipientId`); an unknown id is inactive. An
inactive or unknown recipient answers 200 with an empty `logIds` and writes no row. For
`dispatchIndependently` the check runs in the caller's transaction, so an account created in that
transaction is seen.

## Consequences
- A `PENDING_VERIFICATION` customer can receive any template a caller dispatches, not only the
  verification mail; the restriction to specific templates was considered unnecessary because dispatch is
  in-process for the customer flows (see ADR-NOTIF-005 for the HTTP exposure).
- A dispatcher cannot distinguish "recipient inactive" from "nothing requested" except by the empty id
  list; no error code is raised and nothing is logged in `NOTIF_LOG` (history of earlier rows is kept).
- The inbox is served to both realms by the same controller on two paths (`/api/v1/notif/inbox`,
  `/api/v1/customers/me/inbox`), each on its realm's security chain.
- The P1 body's "UserAccount (SEC) SOFT-READ" and XM-NOTIF-001 now point at `SEC_USER` with both realms.

## Traces
RULE-NOTIF-007, RULE-NOTIF-012, RULE-NOTIF-019 · XM-NOTIF-001 · DBF-0002 · srs.md addendum §2, §6 ·
code: `erp-core/src/main/java/com/erp/notif/crossmodule/RecipientDirectory.java`,
`SecRecipientDirectory.java:20-34`, `service/DispatchService.java:93-102, 118-124`,
`NotificationDispatchApiImpl.java:28-36`, `com/erp/sec/crossmodule/UserContact.java`,
`com/erp/sec/crossmodule/SecUserDirectoryApiImpl.java:119-126` · docs/steps/08-report.md ·
DEVIATIONS [06], [08], [14], [15]
