# ADR-SEC-037 — How a service-account token is issued, carried and re-checked on every request
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : REQ-SEC-046, REQ-SEC-047, REQ-SEC-048, REQ-SEC-049, REQ-SEC-050, REQ-SEC-054, REQ-SEC-059, REQ-SEC-060, REQ-SEC-066, RULE-SEC-009, ENT-SEC-014, DBF-SEC-107, DBF-SEC-110

## Context
ADR-SEC-012 chose the OAuth 2.0 client-credentials grant. ADR-SEC-014 required a live check of the credential and the
account on every request, including requests that bear a previously issued access token, and left the mechanism to
this stage. The SRS fixes the rest:
- no session row (REQ-SEC-046);
- no refresh token (REQ-SEC-049);
- a 3600-second token (DEFAULT D1);
- the username as client identifier (DEFAULT D2);
- one-way storage of the secret (REQ-SEC-054);
- one message on every failure (RULE-SEC-009);
- a SERVICE_AUTH_FAILED entry on every rejection (REQ-SEC-066).

The v1 platform already issues a signed JWT at login and validates each request against an ActiveSession row. The SRS
names four facts it does not settle, and each needs a choice:
- the wire shape of the token call;
- how a request is tied back to its credential;
- what "unsupported grant type" answers;
- how a rejection's audit entry survives the rejection.

## Decision
1. **Token call.** `POST /api/v1/sec/auth/token` (API-SEC-031) takes a JSON body `{grantType, clientId, clientSecret}`
   and answers `ApiResponse<TokenResponse>{accessToken, tokenType: "Bearer", expiresIn: 3600}`. This is the platform's
   envelope, the same as API-SEC-001's login. The parameters keep RFC 6749 §4.4.2's meanings. They are not sent as
   `application/x-www-form-urlencoded`, and the response is not RFC 6749 §5.1's bare JSON: the only caller is the
   platform's own daemon, and one envelope across every SEC endpoint is worth more than off-the-shelf OAuth client
   compatibility. `Cache-Control: no-store` is set, as RFC 6749 §5.1 requires.
2. **Token contents.** The access token is the platform's signed JWT with the same lifetime mechanism as the login
   token. Its subject is the account's userPk, its principal-type claim is SERVICE, and a credential claim holds the
   PK of the credential that matched.
3. **Per-request check (the mechanism ADR-SEC-014 left open).** For a SERVICE token, the platform JWT filter reads no
   ActiveSession row. It loads the credential by the claim (QR-SEC-044) and the account by the subject (QR-SEC-040),
   and accepts the request only when:
   - the credential exists, belongs to that account and has an empty revocation time;
   - the account is ACTIVE and of principal type SERVICE.
   The result is never cached, because a cache would reopen the trust window ADR-SEC-014 closes. This costs two
   primary-key reads per machine request. Revocation (REQ-SEC-059) and deactivation (REQ-SEC-060) therefore take
   effect on the next request, and reactivation restores the never-revoked credentials (REQ-SEC-062) with nothing
   written to them.
4. **Secrets.** A secret is 32 bytes from `SecureRandom`, base64url without padding. Only its hash, from the platform
   `PasswordEncoder` that hashes passwords, is stored. The secret appears in the issuance response only. The request
   bodies of API-SEC-029 and API-SEC-031 are excluded from request logging. For an unknown client identifier, one hash
   verification still runs against a fixed dummy hash, so response timing does not reveal whether the account exists.
5. **Unsupported grant type.** A `grantType` other than `client_credentials` answers 400
   `SEC-400-UNSUPPORTED-GRANT-TYPE` (ar "نوع المنحة غير مدعوم" · en "Unsupported grant type"), a PLATFORM-STD row under
   ADR-SEC-002. This follows RFC 6749 §5.2 `unsupported_grant_type`. A credential in another module's path, or an
   unknown credential id, answers 404 `SEC-404-CREDENTIAL` (ar "بيانات الاعتماد غير موجودة" · en "Credential not
   found"), also PLATFORM-STD.
6. **Rejection-path audit.** A rejection is raised as a `LocalizedException`, which rolls the request's transaction
   back. The SERVICE_AUTH_FAILED entry, and v1 login's LOGIN_FAILED entry, are therefore appended through QR-SEC-048 in
   `REQUIRES_NEW`. That is the one non-default transaction in SEC.

## Consequences
- Each machine request costs two indexed PK reads more than a human one. At one daemon this is negligible. If the
  platform later hosts many machine callers, a short cache with explicit invalidation on revoke and deactivate is the
  lever. Adopting it needs a new ADR, because it re-opens the trust window.
- The daemon re-authenticates with its secret when `expiresIn` elapses or on any 401. This is published on API-SEC-031
  (DOC phase).
- Non-breaking: every choice fills a gap the SRS leaves, and none contradicts a REQ or a locked decision.
