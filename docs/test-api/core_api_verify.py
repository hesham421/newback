#!/usr/bin/env python3
"""erp-core 1.0.0 — API verification of the governed test plan docs/test-api/core-test-plan.md.

Intended for Dev/Test environments only. Stdlib only (Python 3.10+).

Method (skill `api-verify`, with the phase-D overrides): translation, not derivation. Every test
function below implements one TC-CORE-* case of the plan, declared with @tc("TC-CORE-..."). The request,
precondition and expectation of each one is the plan's own cell; nothing is invented. Order = plan §3.

Contract facts used (derived from docs/api-docs/*/index.md and the source, in place of
api-verify-config.md, which does not apply here):
  * Base path /api/v1/** (docs/api-docs/README.md); auth `Authorization: Bearer <JWT>`; the staff tenant
    comes from `X-Tenant-Code` (TenantResolutionFilter).
  * Success envelope com.erp.common.web.ApiResponse {success:true, data, timestamp}.
  * Failure envelope ApiResponse {success:false, error: ApiError{code, message, fieldErrors[{field,message}]},
    timestamp}; codes are UPPER_SNAKE (`TENANT_REQUIRED`) or SEC hyphenated (`SEC-401-INVALID-CREDENTIALS`)
    (GlobalExceptionHandler, SecSecurityErrorHandler, RealmEnforcementFilter).
  * Paging: `data` is a Spring Page {content[], totalElements, number, size}.
  * Languages: en (default) and ar via `Accept-Language` (i18n/messages*.properties).

Usage:
  python docs/test-api/core_api_verify.py --base http://localhost:7272 --admin-password <pw>
  python docs/test-api/core_api_verify.py --profile P-MAIL --base http://localhost:7295 --smtp-port 1025 ...
  python docs/test-api/core_api_verify.py --report docs/test-api/results/<a>.json [<b>.json ...]
The admin password may also come from env ERP_BOOTSTRAP_ADMIN_PASSWORD (never written to any output).
"""
from __future__ import annotations

import argparse
import base64
import csv
import datetime as dt
import glob
import hashlib
import http.client
import io
import json
import os
import re
import struct
import sys
import tempfile
import time
import traceback
import urllib.parse
import uuid
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

# =============================================================================================
# registry
# =============================================================================================
TESTS: list = []          # [(func, ids, profile)]


def tc(*ids, profile="P-LIVE"):
    def deco(f):
        f._tc_ids = ids
        f._profile = profile
        TESTS.append((f, ids, profile))
        return f
    return deco


class Blocked(Exception):
    pass


class Ctx(dict):
    """Shared run state (tokens, ids). Attribute access for brevity."""

    def __getattr__(self, k):
        try:
            return self[k]
        except KeyError:
            raise Blocked(f"precondition value `{k}` missing (an earlier case did not produce it)")

    def __setattr__(self, k, v):
        self[k] = v

    def has(self, *keys):
        return all(k in self and self[k] is not None for k in keys)


RESULTS: dict = {}        # tc id -> record
CUR = {"tc": None}


def _rec(tc_id=None):
    tid = tc_id or CUR["tc"]
    return RESULTS[tid]


def check(ok, what, expected="", observed="", req=None, tc_id=None):
    _rec(tc_id)["checks"].append({
        "ok": bool(ok), "what": what, "expected": str(expected),
        "observed": str(observed)[:600], "request": req or ""})
    return bool(ok)


# =============================================================================================
# http
# =============================================================================================
class Resp:
    def __init__(self, status, headers, body, req):
        self.status = status
        self.headers = {k.lower(): v for k, v in headers}
        self.body = body
        self.req = req
        self.json = None
        if body:
            try:
                self.json = json.loads(body.decode("utf-8"))
            except Exception:
                self.json = None

    @property
    def data(self):
        return self.json.get("data") if isinstance(self.json, dict) else None

    @property
    def error(self):
        return (self.json.get("error") or {}) if isinstance(self.json, dict) else {}

    @property
    def code(self):
        return self.error.get("code") if self.error else None

    @property
    def field_errors(self):
        return self.error.get("fieldErrors") or []

    @property
    def content(self):
        d = self.data
        return d.get("content", []) if isinstance(d, dict) else []

    def excerpt(self, n=300):
        try:
            t = self.body.decode("utf-8")
        except Exception:
            t = repr(self.body[:n])
        t = re.sub(r'"accessToken"\s*:\s*"[^"]+"', '"accessToken":"<redacted>"', t)
        t = re.sub(r'"token"\s*:\s*"[^"]+"', '"token":"<redacted>"', t)
        return t[:n]


BASE = {"url": "http://localhost:7272"}


def api(method, path, t=None, tc=None, body=None, lang=None, headers=None, raw=None,
        content_type=None, multipart=None, base=None):
    url = urllib.parse.urlsplit(base or BASE["url"])
    hdrs = {}
    if t:
        hdrs["Authorization"] = f"Bearer {t}"
    if tc is not None:
        hdrs["X-Tenant-Code"] = tc
    if lang:
        hdrs["Accept-Language"] = lang
    data = None
    if multipart is not None:
        boundary = "----erpverify" + uuid.uuid4().hex
        data = _multipart(multipart, boundary)
        hdrs["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        hdrs["Content-Type"] = "application/json"
    elif raw is not None:
        data = raw
        if content_type:
            hdrs["Content-Type"] = content_type
    if headers:
        hdrs.update(headers)
    req_desc = f"{method} {path}" + (" [token]" if t else " [no token]") + \
        (f" X-Tenant-Code:{tc!r}" if tc is not None else "") + (f" Accept-Language:{lang}" if lang else "")
    if body is not None:
        shown = {k: ("<redacted>" if "password" in k.lower() else v) for k, v in body.items()} if isinstance(body, dict) else body
        req_desc += " " + json.dumps(shown, ensure_ascii=False)[:250]
    for attempt in range(3):
        try:
            conn = http.client.HTTPConnection(url.hostname, url.port or 80, timeout=60)
            conn.request(method, path, body=data, headers=hdrs)
            r = conn.getresponse()
            payload = r.read()
            res = Resp(r.status, r.getheaders(), payload, req_desc)
            conn.close()
            return res
        except (ConnectionError, http.client.HTTPException, OSError) as e:
            if attempt == 2:
                return Resp(0, [], f"CONNECTION ERROR {e}".encode(), req_desc)
            time.sleep(1)


def _multipart(fields, boundary):
    out = io.BytesIO()
    for name, val in fields:
        out.write(f"--{boundary}\r\n".encode())
        if isinstance(val, tuple):
            fname, ctype, content = val
            out.write(f'Content-Disposition: form-data; name="{name}"; filename="{fname}"\r\n'.encode())
            out.write(f"Content-Type: {ctype}\r\n\r\n".encode())
            out.write(content)
        else:
            out.write(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode())
            out.write(str(val).encode("utf-8"))
        out.write(b"\r\n")
    out.write(f"--{boundary}--\r\n".encode())
    return out.getvalue()


def observe(what, observed, req=""):
    """Stage-E style observation: recorded next to the case, never part of its pass/fail."""
    _rec().setdefault("observations", []).append({"what": what, "observed": str(observed)[:600], "request": req})


def st(r, status, code=None, what=None, tc_id=None):
    """Assert status (+ envelope error code when given)."""
    ok = r.status == status and (code is None or r.code == code)
    if ok and 200 <= status < 300 and isinstance(r.json, dict) and "success" in r.json:
        ok = r.json.get("success") is True
    exp = f"{status}" + (f" E({code})" if code else "")
    obs = f"{r.status}" + (f" E({r.code})" if r.code else "") + f" · {r.excerpt()}"
    return check(ok, what or r.req, exp, obs, r.req, tc_id)


def eq(actual, expected, what, tc_id=None):
    return check(actual == expected, what, repr(expected), repr(actual), tc_id=tc_id)


def jwt_claims(token):
    try:
        seg = token.split(".")[1]
        seg += "=" * (-len(seg) % 4)
        return json.loads(base64.urlsafe_b64decode(seg))
    except Exception:
        return {}


def poll(fn, pred, timeout=10.0, interval=0.3):
    deadline = time.time() + timeout
    last = fn()
    while not pred(last) and time.time() < deadline:
        time.sleep(interval)
        last = fn()
    return last


def png_bytes(seed=0):
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    raw = b"\x00" + bytes([seed % 256, 0, 0])
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


def changes_of(row):
    ch = row.get("changes")
    if isinstance(ch, str):
        try:
            ch = json.loads(ch)
        except Exception:
            return []
    return ch or []


def has_arabic(s):
    return bool(re.search(r"[؀-ۿ]", s or ""))


# =============================================================================================
# common fixture steps (shared by P-LIVE cases and the profile setups)
# =============================================================================================
PW = "Passw0rd!Tc1"


def login_staff(tenant, username, password):
    return api("POST", "/api/v1/sec/auth/login", tc=tenant, body={"username": username, "password": password})


def tenant_body(ctx, code, admin, name_en):
    return {"code": code, "nameAr": "مستأجر أ", "nameEn": name_en, "adminUsername": admin,
            "adminEmail": f"{admin}-{ctx.run}@t.test", "adminPassword": PW,
            "adminFullNameAr": "مدير", "adminFullNameEn": "Admin A"}


def user_body(username, email, ar, en, password=PW):
    return {"username": username, "email": email, "fullNameAr": ar, "fullNameEn": en, "password": password}


def upload(ctx, token, fname, ctype, content, cat=None):
    fields = [("file", (fname, ctype, content)), ("ownerId", 4711), ("ownerType", "PRODUCT"),
              ("moduleCode", "SHOP")]
    if cat is not None:
        fields.append(("fileCategoryFk", cat))
    return api("POST", "/api/v1/files", t=token, multipart=fields)


def audit(token, **params):
    q = urllib.parse.urlencode(params)
    return api("GET", f"/api/v1/audit/events?{q}", t=token)


def dispatch_body(recipient, channels, ref_type="TC_REF"):
    return {"recipientId": recipient, "templateCode": "PASSWORD_RESET", "channelHint": channels,
            "moduleCode": "TEST", "referenceType": ref_type, "referenceId": 1,
            "variables": {"actionLink": "http://x/", "expiresAt": "soon"}}


def get_log(token, log_id):
    return api("GET", f"/api/v1/notifications/logs/{log_id}", t=token)


# =============================================================================================
# Phase 0 — anonymous
# =============================================================================================
@tc("TC-CORE-CORE-001")
def test_core_001_health_public(ctx):
    r = api("GET", "/actuator/health")
    st(r, 200)
    j = r.json if isinstance(r.json, dict) else {}
    eq(j.get("status"), "UP", "status")
    check("success" not in j, "plain actuator JSON, not the envelope", "no `success` key", list(j)[:5])


@tc("TC-CORE-CORE-002")
def test_core_002_openapi_no_fin(ctx):
    r = api("GET", "/v3/api-docs")
    st(r, 200)
    paths = list((r.json or {}).get("paths", {}).keys())
    seg_fin = [p for p in paths if any(s == "fin" or s.startswith("fin") and s != "find" for s in p.split("/"))]
    check(not seg_fin, "no `fin` path (path segment)", "[]", seg_fin)
    literal = [p for p in paths if "fin" in p]
    ctx.setdefault("plan_notes", []).append(
        f"CORE-002: the literal reading 'no key contains `fin`' matches {literal} — "
        f"`/api/v1/report/definitions` (required by the same case) contains the substring 'fin'; "
        f"the check uses the path-segment reading.")
    for p in ["/api/v1/platform/tenants", "/api/v1/public/customers/register", "/api/v1/customers/me",
              "/api/v1/files/{id}/visibility", "/api/v1/public/files/{tenantCode}/{publicSlug}",
              "/api/v1/notif/inbox", "/api/v1/customers/me/inbox", "/api/v1/sequence/series",
              "/api/v1/audit/events", "/api/v1/report/definitions"]:
        check(p in paths, f"paths contains {p}", "present", "present" if p in paths else "absent")


@tc("TC-CORE-CORE-003")
def test_core_003_openapi_customer_groups(ctx):
    need = ["/api/v1/public/customers/register", "/api/v1/public/customers/verify",
            "/api/v1/public/customers/login", "/api/v1/public/customers/password-reset/request",
            "/api/v1/public/customers/password-reset/complete", "/api/v1/customers/me"]
    for g in ("customers", "sec"):
        r = api("GET", f"/v3/api-docs/{g}")
        st(r, 200)
        paths = (r.json or {}).get("paths", {})
        missing = [p for p in need if p not in paths]
        check(not missing, f"group {g} lists the customer endpoints", "none missing", missing)


@tc("TC-CORE-CORE-004")
def test_core_004_swagger_groups(ctx):
    r = api("GET", "/v3/api-docs/swagger-config")
    st(r, 200)
    urls = sorted(u.get("url") for u in (r.json or {}).get("urls", []))
    exp = sorted(f"/v3/api-docs/{g}" for g in ["cu", "customers", "file", "mdl", "notif", "sec"])
    eq(urls, exp, "urls[*].url")
    check(not [u for u in urls if "fin" in u], "no fin group", "none", urls)


@tc("TC-CORE-CORE-005")
def test_core_005_actuator_info(ctx):
    r = api("GET", "/actuator/info")
    st(r, 200)
    check(isinstance(r.json, dict), "JSON object", "object", r.excerpt())


@tc("TC-CORE-CORE-006")
def test_core_006_protected_needs_token(ctx):
    st(api("GET", "/api/v1/sec/users/1"), 401, "SEC-401-INVALID-CREDENTIALS")
    st(api("GET", "/api/v1/sec/users/1", tc="PLATFORM"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-CORE-007")
def test_core_007_unknown_path_401(ctx):
    st(api("GET", "/api/v1/does-not-exist"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-CORE-008")
def test_core_008_malformed_bearer(ctx):
    st(api("GET", "/api/v1/sec/menu", headers={"Authorization": "Bearer not.a.jwt"}), 401,
       "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-TENANT-001")
def test_tenant_001_login_without_header(ctx):
    st(api("POST", "/api/v1/sec/auth/login", body={"username": "admin", "password": ctx.ADMIN_PW}),
       400, "TENANT_REQUIRED")


@tc("TC-CORE-TENANT-002")
def test_tenant_002_login_unknown_tenant(ctx):
    st(login_staff(f"NOPE_{ctx.RUN}", "admin", ctx.ADMIN_PW), 404, "TENANT_NOT_FOUND")


@tc("TC-CORE-TENANT-003")
def test_tenant_003_header_trimmed_uppercased(ctx):
    r = login_staff(" platform ", "admin", ctx.ADMIN_PW)
    st(r, 200)
    check(bool((r.data or {}).get("accessToken")), "data.accessToken non-empty", "non-empty",
          "present" if (r.data or {}).get("accessToken") else "missing")


@tc("TC-CORE-TENANT-004")
def test_tenant_004_public_staff_paths_need_header(ctx):
    st(api("POST", "/api/v1/sec/auth/signup", body={"email": "x@t.test", "fullNameAr": "س", "fullNameEn": "X"}),
       400, "TENANT_REQUIRED")
    st(api("POST", "/api/v1/sec/auth/password-reset/request", body={"email": "x@t.test"}), 400, "TENANT_REQUIRED")


@tc("TC-CORE-PLATFORM-001")
def test_platform_001_anonymous_refused(ctx):
    for m, p, b in [("GET", "/api/v1/platform/tenants", None), ("POST", "/api/v1/platform/tenants", {}),
                    ("GET", "/api/v1/platform/tenants/1", None), ("POST", "/api/v1/platform/tenants/search", {}),
                    ("PATCH", "/api/v1/platform/tenants/1/status", {"statusCode": "ACTIVE"})]:
        st(api(m, p, body=b), 401, "SEC-401-INVALID-CREDENTIALS")


# =============================================================================================
# Phase 1 — PLATFORM bootstrap
# =============================================================================================
@tc("TC-CORE-SEC-001")
def test_sec_001_bootstrap_admin_login(ctx):
    r = login_staff("PLATFORM", "admin", ctx.ADMIN_PW)
    st(r, 200)
    d = r.data or {}
    if d.get("accessToken"):
        ctx.T_PLAT = d["accessToken"]
    eq(d.get("tokenType"), "Bearer", "data.tokenType")
    c = jwt_claims(d.get("accessToken", ""))
    eq(c.get("tid"), 1, "JWT tid")
    eq(c.get("realm"), "STAFF", "JWT realm")
    eq(c.get("sub"), "admin", "JWT sub")


@tc("TC-CORE-SEC-002")
def test_sec_002_admin_admin_rejected(ctx):
    check(ctx.ADMIN_PW != "admin", "precondition $ADMIN_PW != admin", "true", ctx.ADMIN_PW != "admin")
    st(login_staff("PLATFORM", "admin", "admin"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-SEC-003")
def test_sec_003_permission_catalog_synchronized(ctx):
    exp = {"AUDIT_EVENTS": ["AUDIT:EVENT:READ"],
           "SEQUENCE_SERIES": ["PERM_SEQUENCE_SERIES_VIEW", "PERM_SEQUENCE_SERIES_MANAGE"],
           "FILE_BROWSER": ["FILE:DOCUMENT:PUBLISH"],
           "PLATFORM_SETTINGS": ["PERM_PLATFORM_SETTINGS_VIEW", "PLATFORM_SETTINGS_MANAGE"],
           "SEC_REPORTS": ["PERM_SEC_REPORTS_VIEW", "SEC:REPORT:SEC_USER_LIST"],
           "AUDIT_REPORTS": ["AUDIT:REPORT:AUDIT_EVENT_LIST"],
           "NOTIF_REPORTS": ["NOTIF:REPORT:NOTIF_LOG_SUMMARY"],
           "APP_REPORTS": ["APP:REPORT:APP_SMOKE_REPORT"]}
    for page, perms in exp.items():
        r = api("POST", "/api/v1/sec/registry/search", t=ctx.T_PLAT, body={"pageCode": page, "size": 50})
        st(r, 200)
        codes = []
        for mod in r.content:
            for s in mod.get("screens") or []:
                if s.get("pageCode") == page:
                    codes += [a.get("permissionCode") for a in s.get("actions") or []]
        missing = [p for p in perms if p not in codes]
        check(not missing, f"screen {page} actions contain {perms}", "none missing", f"missing={missing} got={codes}")


@tc("TC-CORE-SEC-004")
def test_sec_004_platform_super_role_holds_all(ctx):
    st(api("GET", "/api/v1/audit/events", t=ctx.T_PLAT), 200)
    st(api("POST", "/api/v1/sequence/series/search", t=ctx.T_PLAT, body={}), 200)
    r = api("GET", "/api/v1/report/definitions", t=ctx.T_PLAT)
    st(r, 200)
    eq(sorted(x.get("code") for x in r.data or []),
       sorted(["SEC_USER_LIST", "AUDIT_EVENT_LIST", "NOTIF_LOG_SUMMARY", "APP_SMOKE_REPORT"]),
       "definitions data[*].code = the 4 codes of REPORT-001")
    st(api("POST", "/api/v1/common/configurations/search?scope=PLATFORM", t=ctx.T_PLAT, body={}), 200)


@tc("TC-CORE-SEQ-001")
def test_seq_001_create_platform_series(ctx):
    r = api("POST", "/api/v1/sequence/series", t=ctx.T_PLAT, body={"code": f"tc_inv_{ctx.run}", "prefix": "INV"})
    st(r, 201)
    d = r.data or {}
    ctx.SEQ_PLAT_ID = d.get("id")
    ctx.SEQ_PLAT_PATTERN = d.get("pattern")
    eq(d.get("code"), f"TC_INV_{ctx.RUN}", "data.code")
    eq(d.get("pattern"), "{PREFIX}-{YYYY}-{SEQ:6}", "data.pattern")
    eq(d.get("resetPolicy"), "YEARLY", "data.resetPolicy")
    eq(str(d.get("periodKey")), str(dt.date.today().year), "data.periodKey")
    eq(d.get("nextValue"), 1, "data.nextValue")
    eq(d.get("isActive"), True, "data.isActive")


@tc("TC-CORE-SETTINGS-001")
def test_settings_001_create_platform_default(ctx):
    r = api("POST", "/api/v1/common/configurations?scope=PLATFORM", t=ctx.T_PLAT,
            body={"configKey": f"TC_DEF_{ctx.RUN}", "configValue": "default-1"})
    st(r, 201)
    d = r.data or {}
    ctx.CFG_PLAT_DEF_ID = d.get("id")
    eq(d.get("scope"), "PLATFORM", "data.scope")
    eq(d.get("configKey"), f"TC_DEF_{ctx.RUN}", "data.configKey")
    eq(d.get("configValue"), "default-1", "data.configValue")
    eq(d.get("isActive"), True, "data.isActive")


@tc("TC-CORE-SETTINGS-002")
def test_settings_002_duplicate_platform_default(ctx):
    st(api("POST", "/api/v1/common/configurations?scope=PLATFORM", t=ctx.T_PLAT,
           body={"configKey": f"TC_DEF_{ctx.RUN}", "configValue": "default-1"}), 409, "APP_CONFIGURATION_KEY_DUPLICATE")


@tc("TC-CORE-SETTINGS-003")
def test_settings_003_tenant_scope_separate(ctx):
    k = f"TC_DEF_{ctx.RUN}"
    r = api("POST", "/api/v1/common/configurations", t=ctx.T_PLAT, body={"configKey": k, "configValue": "platform-override"})
    st(r, 201)
    ctx.CFG_PLAT_OVR_ID = (r.data or {}).get("id")
    eq((r.data or {}).get("scope"), "TENANT", "data.scope")
    r = api("GET", f"/api/v1/common/configurations/{k}?scope=PLATFORM", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("configValue"), "default-1", "scope=PLATFORM configValue")
    r = api("GET", f"/api/v1/common/configurations/{k}", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("configValue"), "platform-override", "default scope configValue")


# =============================================================================================
# Phase 2 — provisioning
# =============================================================================================
@tc("TC-CORE-TENANT-005")
def test_tenant_005_provision_a(ctx):
    r = api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT, body=tenant_body(ctx, ctx.TA, "ta-admin", "Tenant A"))
    st(r, 201)
    d = r.data or {}
    ctx.A_ID = d.get("id")
    eq(d.get("code"), ctx.TA, "data.code")
    eq(d.get("statusCode"), "ACTIVE", "data.statusCode")
    eq((d.get("nameAr"), d.get("nameEn")), ("مستأجر أ", "Tenant A"), "nameAr/nameEn echoed")


@tc("TC-CORE-TENANT-006")
def test_tenant_006_tenant_admin_login(ctx):
    r = login_staff(ctx.TA, "ta-admin", PW)
    st(r, 200)
    d = r.data or {}
    if d.get("accessToken"):
        ctx.T_A = d["accessToken"]
    eq(d.get("tokenType"), "Bearer", "data.tokenType")
    check((d.get("expiresIn") or 0) > 0, "data.expiresIn > 0", ">0", d.get("expiresIn"))
    c = jwt_claims(d.get("accessToken", ""))
    eq(c.get("tid"), ctx.A_ID, "JWT tid")
    eq(c.get("realm"), "STAFF", "JWT realm")


@tc("TC-CORE-TENANT-007")
def test_tenant_007_provision_b_and_c(ctx):
    for code, admin, key_id, key_t, name in [(ctx.TB, "tb-admin", "B_ID", "T_B", "Tenant B"),
                                            (ctx.TC, "tc-admin", "C_ID", "T_C", "Tenant C")]:
        r = api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT, body=tenant_body(ctx, code, admin, name))
        st(r, 201)
        ctx[key_id] = (r.data or {}).get("id")
        r = login_staff(code, admin, PW)
        st(r, 200)
        if (r.data or {}).get("accessToken"):
            ctx[key_t] = r.data["accessToken"]


@tc("TC-CORE-TENANT-008")
def test_tenant_008_duplicate_code(ctx):
    b = tenant_body(ctx, ctx.TA, "dup-admin", "Dup")
    st(api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT, body=b), 409, "TENANT_CODE_DUPLICATE")


def tenant_codes(ctx):
    r = api("GET", "/api/v1/platform/tenants?size=200", t=ctx.T_PLAT)
    return r, [x.get("code") for x in r.content]


@tc("TC-CORE-TENANT-009")
def test_tenant_009_invalid_code(ctx):
    for code in ("AB", "BAD-CODE"):
        st(api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT, body=tenant_body(ctx, code, "x-admin", "X")),
           400, "TENANT_CODE_INVALID")
    r, codes = tenant_codes(ctx)
    st(r, 200)
    check("AB" not in codes and "BAD-CODE" not in codes, "list shows neither code", "absent",
          [c for c in codes if c in ("AB", "BAD-CODE")])


@tc("TC-CORE-TENANT-010")
def test_tenant_010_missing_fields(ctx):
    r = api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT, body={})
    st(r, 400, "VALIDATION_ERROR")
    fields = {f.get("field") for f in r.field_errors}
    need = {"code", "nameAr", "nameEn", "adminUsername", "adminEmail", "adminPassword", "adminFullNameAr", "adminFullNameEn"}
    check(need <= fields, "fieldErrors name all required fields", sorted(need), sorted(fields))


@tc("TC-CORE-TENANT-011")
def test_tenant_011_list_get_search_unknown(ctx):
    r = api("GET", "/api/v1/platform/tenants?page=0&size=200", t=ctx.T_PLAT)
    st(r, 200)
    codes = {x.get("code") for x in r.content}
    need = {"PLATFORM", ctx.TA, ctx.TB, ctx.TC}
    check(need <= codes, "content[*].code ⊇ {PLATFORM,TA,TB,TC}", sorted(need),
          f"missing={sorted(need - codes)} totalElements={(r.data or {}).get('totalElements')} size={(r.data or {}).get('size')}")
    r = api("GET", f"/api/v1/platform/tenants/{ctx.A_ID}", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("code"), ctx.TA, "data.code")
    r = api("POST", "/api/v1/platform/tenants/search", t=ctx.T_PLAT,
            body={"filters": [{"field": "code", "operator": "EQUALS", "value": ctx.TA}]})
    st(r, 200)
    eq([x.get("code") for x in r.content], [ctx.TA], "search content")
    st(api("GET", "/api/v1/platform/tenants/999999999", t=ctx.T_PLAT), 404, "TENANT_NOT_FOUND")


@tc("TC-CORE-TENANT-012")
def test_tenant_012_reference_catalog_copied(ctx):
    r = api("POST", "/api/v1/notifications/templates/search", t=ctx.T_A, body={"size": 100})
    st(r, 200)
    tcodes = {x.get("templateCode") for x in r.content}
    need = {"PASSWORD_RESET", "ACCOUNT_ACTIVATION", "CUSTOMER_VERIFY_EMAIL", "CUSTOMER_PASSWORD_RESET"}
    check(need <= tcodes, "templates ⊇ 4 codes", sorted(need), sorted(tcodes))
    r = api("POST", "/api/v1/notifications/channels/search", t=ctx.T_A, body={"size": 100})
    st(r, 200)
    ch = {x.get("channelTypeId") for x in r.content}
    check({"EMAIL", "IN_APP"} <= ch, "channels ⊇ {EMAIL, IN_APP}", "EMAIL, IN_APP", sorted(map(str, ch)))
    r = api("POST", "/api/v1/sec/roles/search", t=ctx.T_A, body={"size": 100})
    st(r, 200)
    sa = [x for x in r.content if x.get("code") == "SYS_ADMIN"]
    if sa:
        ctx.A_SYS_ADMIN_ID = sa[0].get("rolePk")
    check(bool(sa) and sa[0].get("isSuper") is True, "SYS_ADMIN isSuper=true", "true", sa[:1])


@tc("TC-CORE-SEQ-002")
def test_seq_002_provisioning_copies_series(ctx):
    r = api("POST", "/api/v1/sequence/series/search", t=ctx.T_A,
            body={"filters": [{"field": "code", "operator": "EQUALS", "value": f"TC_INV_{ctx.RUN}"}]})
    st(r, 200)
    rows = r.content
    eq(len(rows), 1, "exactly one row")
    if rows:
        check(rows[0].get("id") != ctx.SEQ_PLAT_ID, "id != $SEQ_PLAT_ID", f"!= {ctx.SEQ_PLAT_ID}", rows[0].get("id"))
        eq(rows[0].get("nextValue"), 1, "nextValue")
        eq(rows[0].get("pattern"), ctx.SEQ_PLAT_PATTERN, "pattern equals PLATFORM's")


# =============================================================================================
# Phase 3 — users and isolation
# =============================================================================================
@tc("TC-CORE-TENANT-013")
def test_tenant_013_staff_user_fixtures(ctx):
    r = api("POST", "/api/v1/sec/users", t=ctx.T_A,
            body=user_body(f"alice-{ctx.run}", f"alice-{ctx.run}@t.test", "أليس", "Alice"))
    st(r, 201)
    d = r.data or {}
    ctx.ALICE_ID = d.get("userPk")
    eq(d.get("realm"), "STAFF", "alice data.realm")
    eq(d.get("roles"), [], "alice data.roles empty")
    r = api("POST", "/api/v1/sec/users", t=ctx.T_B, body=user_body(f"bob-{ctx.run}", f"bob-{ctx.run}@t.test", "بوب", "Bob"))
    st(r, 201)
    ctx.BOB_ID = (r.data or {}).get("userPk")
    eq((r.data or {}).get("realm"), "STAFF", "bob data.realm")
    r = login_staff(ctx.TA, f"alice-{ctx.run}", PW)
    st(r, 200)
    if (r.data or {}).get("accessToken"):
        ctx.T_ALICE = r.data["accessToken"]


def a_usernames(ctx, token, extra_header=None):
    r = api("POST", "/api/v1/sec/users/search", t=token, tc=extra_header, body={"size": 100})
    return r, [x.get("username") for x in r.content], [x.get("userPk") for x in r.content]


@tc("TC-CORE-TENANT-014")
def test_tenant_014_search_never_returns_b(ctx):
    r, names, ids = a_usernames(ctx, ctx.T_A)
    st(r, 200)
    eq(sorted(names), sorted(["ta-admin", f"alice-{ctx.run}"]), "content[*].username")
    check(ctx.BOB_ID not in ids and "tb-admin" not in names, "no B rows", "absent", names)


@tc("TC-CORE-TENANT-015")
def test_tenant_015_cross_tenant_get_404(ctx):
    st(api("GET", f"/api/v1/sec/users/{ctx.BOB_ID}", t=ctx.T_A), 404, "SEC-404-USER")
    r = api("GET", f"/api/v1/sec/users/{ctx.BOB_ID}", t=ctx.T_B)
    st(r, 200)
    eq((r.data or {}).get("username"), f"bob-{ctx.run}", "data.username")


@tc("TC-CORE-TENANT-016")
def test_tenant_016_header_cannot_switch_tenant(ctx):
    r, names, ids = a_usernames(ctx, ctx.T_A, ctx.TB)
    st(r, 200)
    eq(sorted(names), sorted(["ta-admin", f"alice-{ctx.run}"]), "same rows as TENANT-014")


@tc("TC-CORE-TENANT-017")
def test_tenant_017_username_unique_per_tenant(ctx):
    st(api("POST", "/api/v1/sec/users", t=ctx.T_B,
           body=user_body(f"alice-{ctx.run}", f"alice-b-{ctx.run}@t.test", "أليس", "Alice B")), 201)


@tc("TC-CORE-TENANT-018")
def test_tenant_018_login_scoped_to_header(ctx):
    st(login_staff(ctx.TB, "ta-admin", PW), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-SEC-005")
def test_sec_005_tenant_super_role_no_platform_perms(ctx):
    st(api("GET", "/api/v1/audit/events", t=ctx.T_A), 200)
    st(api("POST", "/api/v1/sequence/series/search", t=ctx.T_A, body={}), 200)
    st(api("POST", "/api/v1/files/categories/search", t=ctx.T_A, body={}), 200)
    st(api("POST", "/api/v1/common/configurations?scope=PLATFORM", t=ctx.T_A,
           body={"configKey": f"NO_{ctx.RUN}", "configValue": "x"}), 403, "ACCESS_DENIED")


@tc("TC-CORE-SEC-006")
def test_sec_006_no_permission_user_refused(ctx):
    r = api("POST", "/api/v1/sec/users", t=ctx.T_PLAT,
            body=user_body(f"p-noperm-{ctx.run}", f"p-noperm-{ctx.run}@t.test", "ب", "No Perm"))
    st(r, 201)
    r = login_staff("PLATFORM", f"p-noperm-{ctx.run}", PW)
    st(r, 200)
    if (r.data or {}).get("accessToken"):
        ctx.T_NOPERM = r.data["accessToken"]
    st(api("POST", "/api/v1/sec/users/search", t=ctx.T_NOPERM, body={}), 403, "SEC-403-FORBIDDEN")


@tc("TC-CORE-PLATFORM-002")
def test_platform_002_tenant_super_admin_refused(ctx):
    st(api("GET", "/api/v1/platform/tenants", t=ctx.T_A), 403, "SEC-403-FORBIDDEN")
    st(api("POST", "/api/v1/platform/tenants", t=ctx.T_A, body=tenant_body(ctx, f"EVIL{ctx.RUN}", "evil-admin", "Evil")),
       403, "SEC-403-FORBIDDEN")
    r = api("POST", "/api/v1/platform/tenants/search", t=ctx.T_PLAT,
            body={"filters": [{"field": "code", "operator": "EQUALS", "value": f"EVIL{ctx.RUN}"}]})
    check(r.status == 200 and r.content == [], f"no tenant EVIL{ctx.RUN} afterwards", "[]", r.excerpt(), r.req)


@tc("TC-CORE-PLATFORM-003")
def test_platform_003_platform_user_without_authority(ctx):
    st(api("GET", "/api/v1/platform/tenants", t=ctx.T_NOPERM), 403, "SEC-403-FORBIDDEN")


@tc("TC-CORE-APP-002")
def test_app_002_dev_reset_fixture_needs_auth(ctx):
    st(api("POST", "/api/v1/sec/dev/password-reset-token", body={"email": "x@t.test"}), 401, "SEC-401-INVALID-CREDENTIALS")
    st(api("POST", "/api/v1/sec/dev/password-reset-token", t=ctx.T_A, body={"email": f"nobody-{ctx.run}@t.test"}),
       404, "SEC-404-USER")


# =============================================================================================
# Phase 4 — number series
# =============================================================================================
def registry_ids(ctx, token, page_code, perm_codes):
    """(module id, screen id, {permissionCode: action id}) of one registry screen (REPORT-011's lookup)."""
    r = api("POST", "/api/v1/sec/registry/search", t=token, body={"pageCode": page_code, "size": 50})
    st(r, 200, what=f"registry search {page_code}")
    mod_id = scr_id = None
    actions = {}
    for m in r.content:
        for s in m.get("screens") or []:
            if s.get("pageCode") == page_code:
                mod_id, scr_id = m.get("moduleRegPk"), s.get("screenRegPk")
                for a in s.get("actions") or []:
                    if a.get("permissionCode") in perm_codes:
                        actions[a.get("permissionCode")] = a.get("actionRegPk")
    check(mod_id and scr_id and set(actions) == set(perm_codes), f"registry ids of {page_code}",
          f"module, screen, {sorted(perm_codes)}", (mod_id, scr_id, actions))
    return mod_id, scr_id, actions


def limited_user(ctx):
    """`lim-{run}` in A: a role with SEQUENCE_SERIES VIEW and FILE_BROWSER VIEW + CREATE (upload), nothing
    else — in particular neither PERM_SEQUENCE_SERIES_MANAGE, FILE:DOCUMENT:PUBLISH nor AUDIT:EVENT:READ.
    Built once (SEQ-014) and reused by FILE-021 and AUDIT-008 (plan §2.3 `T_LIM`)."""
    if ctx.has("T_LIM"):
        return ctx.T_LIM
    r = api("POST", "/api/v1/sec/roles", t=ctx.T_A, body={"code": f"TC_LIM_{ctx.RUN}", "nameAr": "محدود", "nameEn": "Limited"})
    st(r, 201, what="create role TC_LIM_{RUN}")
    role = (r.data or {}).get("rolePk")
    granted_modules = set()
    for page, perms in (("SEQUENCE_SERIES", ["PERM_SEQUENCE_SERIES_VIEW"]),
                        ("FILE_BROWSER", ["PERM_FILE_BROWSER_VIEW", "PERM_FILE_BROWSER_CREATE"])):
        mod_id, scr_id, actions = registry_ids(ctx, ctx.T_A, page, perms)
        if mod_id not in granted_modules:
            st(api("POST", f"/api/v1/sec/roles/{role}/modules", t=ctx.T_A, body={"moduleId": mod_id}), 201)
            granted_modules.add(mod_id)
        st(api("POST", f"/api/v1/sec/roles/{role}/screens", t=ctx.T_A, body={"screenId": scr_id}), 201)
        for p in perms:
            st(api("POST", f"/api/v1/sec/roles/{role}/actions", t=ctx.T_A, body={"actionId": actions.get(p)}), 201)
    r = api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(f"lim-{ctx.run}", f"lim-{ctx.run}@t.test", "محدود", "Limited"))
    st(r, 201)
    uid = (r.data or {}).get("userPk")
    st(api("PUT", f"/api/v1/sec/users/{uid}/roles", t=ctx.T_A, body={"roleIds": [role]}), 200)
    r = login_staff(ctx.TA, f"lim-{ctx.run}", PW)
    st(r, 200, what="login lim-{run}")
    t = (r.data or {}).get("accessToken")
    if not t:
        raise Blocked("no T_LIM")
    ctx.T_LIM = t
    return t


def series(ctx, body, token=None):
    return api("POST", "/api/v1/sequence/series", t=token or ctx.T_PLAT, body=body)


@tc("TC-CORE-SEQ-003")
def test_seq_003_duplicate_code(ctx):
    st(series(ctx, {"code": f"TC_INV_{ctx.RUN}"}), 409, "NUMBER_SERIES_CODE_DUPLICATE")


@tc("TC-CORE-SEQ-004")
def test_seq_004_unknown_token(ctx):
    st(series(ctx, {"code": f"TC_BAD1_{ctx.RUN}", "pattern": "{DAY}-{SEQ:3}"}), 400, "SEQUENCE_PATTERN_INVALID")


@tc("TC-CORE-SEQ-005")
def test_seq_005_pattern_carries_period(ctx):
    st(series(ctx, {"code": f"TC_BAD2_{ctx.RUN}", "pattern": "{PREFIX}-{SEQ:3}", "resetPolicy": "YEARLY"}),
       400, "SEQUENCE_PATTERN_INVALID")
    st(series(ctx, {"code": f"TC_BAD3_{ctx.RUN}", "pattern": "{PREFIX}-{YYYY}-{SEQ:3}", "resetPolicy": "MONTHLY"}),
       400, "SEQUENCE_PATTERN_INVALID")


@tc("TC-CORE-SEQ-006")
def test_seq_006_seq_rules_and_braces(ctx):
    for i, p in enumerate(["{PREFIX}", "{SEQ:3}-{SEQ:3}", "{SEQ:0}", "{SEQ:19}", "{PREFIX-{SEQ:3}"]):
        st(series(ctx, {"code": f"TC_B6{i}_{ctx.RUN}", "pattern": p, "resetPolicy": "NEVER"}), 400, "SEQUENCE_PATTERN_INVALID")


@tc("TC-CORE-SEQ-007")
def test_seq_007_code_format(ctx):
    r = series(ctx, {"code": "has space"})
    st(r, 400, "VALIDATION_ERROR")
    eq((r.field_errors[:1] or [{}])[0].get("field"), "code", "fieldErrors[0].field")


@tc("TC-CORE-SEQ-008")
def test_seq_008_never_series_get_unknown(ctx):
    r = series(ctx, {"code": f"TC_NEV_{ctx.RUN}", "prefix": "P", "pattern": "{PREFIX}-{SEQ:4}", "resetPolicy": "NEVER"})
    st(r, 201)
    ctx.SEQ_NEV_ID = (r.data or {}).get("id")
    r = api("GET", f"/api/v1/sequence/series/{ctx.SEQ_NEV_ID}", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("resetPolicy"), "NEVER", "data.resetPolicy")
    eq((r.data or {}).get("periodKey"), "", "data.periodKey")
    st(api("GET", "/api/v1/sequence/series/987654321", t=ctx.T_PLAT), 404, "NUMBER_SERIES_NOT_FOUND")


@tc("TC-CORE-SEQ-009")
def test_seq_009_search_and_tenantid_filter(ctx):
    r = api("POST", "/api/v1/sequence/series/search", t=ctx.T_PLAT,
            body={"filters": [{"field": "code", "operator": "EQUALS", "value": f"TC_NEV_{ctx.RUN}"}]})
    st(r, 200)
    eq([x.get("code") for x in r.content], [f"TC_NEV_{ctx.RUN}"], "content[*].code")
    r = api("POST", "/api/v1/sequence/series/search", t=ctx.T_PLAT,
            body={"filters": [{"field": "tenantId", "operator": "EQUALS", "value": "2"}]})
    st(r, 400, "VALIDATION_ERROR")
    fe = (r.field_errors[:1] or [{}])[0]
    eq(fe.get("field"), "tenantId", "fieldErrors[0].field")
    allowed = ['This search does not support filtering on "tenantId"', 'هذا البحث لا يدعم التصفية على الحقل "tenantId"']
    check(fe.get("message") in allowed, "fieldErrors[0].message localized from UNSUPPORTED_FILTER_FIELD", allowed, fe.get("message"))


@tc("TC-CORE-SEQ-010")
def test_seq_010_update_invalid_unknown(ctx):
    r = api("PUT", f"/api/v1/sequence/series/{ctx.SEQ_PLAT_ID}", t=ctx.T_PLAT,
            body={"prefix": "NEW", "pattern": "{PREFIX}/{YY}/{SEQ:5}"})
    st(r, 200)
    eq((r.data or {}).get("prefix"), "NEW", "data.prefix")
    eq((r.data or {}).get("pattern"), "{PREFIX}/{YY}/{SEQ:5}", "data.pattern")
    st(api("PUT", f"/api/v1/sequence/series/{ctx.SEQ_PLAT_ID}", t=ctx.T_PLAT, body={"pattern": "{PREFIX}-{SEQ:5}"}),
       400, "SEQUENCE_PATTERN_INVALID")
    st(api("PUT", "/api/v1/sequence/series/987654321", t=ctx.T_PLAT, body={"pattern": "{SEQ:3}"}), 404, "NUMBER_SERIES_NOT_FOUND")


@tc("TC-CORE-SEQ-011")
def test_seq_011_immutable_fields(ctx):
    r = api("PUT", f"/api/v1/sequence/series/{ctx.SEQ_NEV_ID}", t=ctx.T_PLAT,
            body={"prefix": "P2", "pattern": "{PREFIX}-{SEQ:4}", "code": "HIJACK", "resetPolicy": "YEARLY", "nextValue": 999})
    st(r, 200)
    d = r.data or {}
    eq(d.get("prefix"), "P2", "data.prefix")
    eq(d.get("code"), f"TC_NEV_{ctx.RUN}", "data.code unchanged")
    eq(d.get("resetPolicy"), "NEVER", "data.resetPolicy unchanged")
    eq(d.get("nextValue"), 1, "data.nextValue unchanged")


@tc("TC-CORE-SEQ-012")
def test_seq_012_deactivate_activate_unknown(ctx):
    r = api("PUT", f"/api/v1/sequence/series/{ctx.SEQ_NEV_ID}/deactivate", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("isActive"), False, "deactivate data.isActive")
    r = api("PUT", f"/api/v1/sequence/series/{ctx.SEQ_NEV_ID}/activate", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("isActive"), True, "activate data.isActive")
    st(api("PUT", "/api/v1/sequence/series/987654321/deactivate", t=ctx.T_PLAT), 404, "NUMBER_SERIES_NOT_FOUND")


@tc("TC-CORE-SEQ-013")
def test_seq_013_tenant_isolated(ctx):
    st(api("GET", f"/api/v1/sequence/series/{ctx.SEQ_PLAT_ID}", t=ctx.T_A), 404, "NUMBER_SERIES_NOT_FOUND")
    r = series(ctx, {"code": f"TC_OWN_{ctx.RUN}", "pattern": "{SEQ:3}", "resetPolicy": "NEVER"}, ctx.T_A)
    st(r, 201)
    ctx.SEQ_A_ID = (r.data or {}).get("id")
    st(api("GET", f"/api/v1/sequence/series/{ctx.SEQ_A_ID}", t=ctx.T_PLAT), 404)


@tc("TC-CORE-SEQ-014")
def test_seq_014_auth_and_permission(ctx):
    st(api("POST", "/api/v1/sequence/series/search", body={}), 401, "SEC-401-INVALID-CREDENTIALS")
    st(api("POST", "/api/v1/sequence/series/search", t=ctx.T_NOPERM, body={}), 403, "ACCESS_DENIED")
    st(series(ctx, {"code": f"X_{ctx.RUN}"}, ctx.T_ALICE), 403, "ACCESS_DENIED")
    # a role holding the gateway PERM_SEQUENCE_SERIES_VIEW but not PERM_SEQUENCE_SERIES_MANAGE
    t = limited_user(ctx)
    st(api("POST", "/api/v1/sequence/series/search", t=t, body={}), 200, what="T_LIM search (VIEW) → 200")
    st(series(ctx, {"code": f"X2_{ctx.RUN}"}, t), 403, "ACCESS_DENIED", what="T_LIM create (no MANAGE) → 403")


# =============================================================================================
# Phase 5 — settings
# =============================================================================================
CFG = "/api/v1/common/configurations"


@tc("TC-CORE-SETTINGS-004")
def test_settings_004_tenant_cannot_touch_platform(ctx):
    k = f"TC_DEF_{ctx.RUN}"
    st(api("POST", f"{CFG}?scope=PLATFORM", t=ctx.T_A, body={"configKey": f"NO2_{ctx.RUN}", "configValue": "x"}), 403, "ACCESS_DENIED")
    st(api("GET", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_A), 403, "ACCESS_DENIED")
    st(api("PUT", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_A, body={"configValue": "hijack"}), 403, "ACCESS_DENIED")
    st(api("DELETE", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_A), 403, "ACCESS_DENIED")
    r = api("GET", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("configValue"), "default-1", "platform default unchanged")


@tc("TC-CORE-SETTINGS-005")
def test_settings_005_tenant_override_own_rows(ctx):
    k = f"TC_DEF_{ctx.RUN}"
    r = api("POST", CFG, t=ctx.T_A, body={"configKey": k, "configValue": "tenant-value"})
    st(r, 201)
    ctx.CFG_A_ID = (r.data or {}).get("id")
    eq((r.data or {}).get("scope"), "TENANT", "data.scope")
    r = api("POST", f"{CFG}/search", t=ctx.T_A, body={"size": 1000})
    st(r, 200)
    mine = [x for x in r.content if x.get("configKey") == k]
    eq(len(mine), 1, f"{k} listed once")
    if mine:
        eq(mine[0].get("configValue"), "tenant-value", "value")
    foreign = [x for x in r.content if x.get("id") in (ctx.get("CFG_PLAT_DEF_ID"), ctx.get("CFG_PLAT_OVR_ID")) or x.get("scope") == "PLATFORM"]
    check(not foreign, "no platform-default row and no other tenant's row", "[]", foreign[:3])


@tc("TC-CORE-SETTINGS-006")
def test_settings_006_body_cannot_choose_owner(ctx):
    k = f"TC_OWN_{ctx.RUN}"
    r = api("POST", CFG, t=ctx.T_A, body={"configKey": k, "configValue": "v", "tenantId": 1, "scope": "PLATFORM"})
    st(r, 201)
    eq((r.data or {}).get("scope"), "TENANT", "data.scope")
    st(api("GET", f"{CFG}/{k}", t=ctx.T_PLAT), 404, "APP_CONFIGURATION_NOT_FOUND")
    st(api("GET", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_PLAT), 404, "APP_CONFIGURATION_NOT_FOUND")
    st(api("GET", f"{CFG}/{k}", t=ctx.T_A), 200)


@tc("TC-CORE-SETTINGS-007")
def test_settings_007_update_visible(ctx):
    k = f"TC_DEF_{ctx.RUN}"
    r = api("PUT", f"{CFG}/{k}", t=ctx.T_A, body={"configValue": "tenant-value-2"})
    st(r, 200)
    eq((r.data or {}).get("configValue"), "tenant-value-2", "PUT data.configValue")
    r = api("GET", f"{CFG}/{k}", t=ctx.T_A)
    st(r, 200)
    eq((r.data or {}).get("configValue"), "tenant-value-2", "GET data.configValue")


@tc("TC-CORE-SETTINGS-008")
def test_settings_008_unknown_scope(ctx):
    r = api("POST", f"{CFG}/search?scope=GLOBAL", t=ctx.T_PLAT, body={})
    st(r, 400, "VALIDATION_ERROR")
    eq((r.field_errors[:1] or [{}])[0].get("field"), "scope", "fieldErrors[0].field")


@tc("TC-CORE-SETTINGS-009")
def test_settings_009_update_deactivate_platform_default(ctx):
    k = f"TC_DEF_{ctx.RUN}"
    r = api("PUT", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_PLAT, body={"configValue": "default-2"})
    st(r, 200)
    eq((r.data or {}).get("configValue"), "default-2", "configValue")
    r = api("DELETE", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_PLAT)
    check(r.status == 204 and not r.body, "DELETE → 204 no body", "204, empty", f"{r.status} {r.excerpt()}", r.req)
    r = api("GET", f"{CFG}/{k}?scope=PLATFORM", t=ctx.T_PLAT)
    st(r, 200)
    eq((r.data or {}).get("isActive"), False, "data.isActive")


@tc("TC-CORE-SETTINGS-010")
def test_settings_010_overrides_isolated(ctx):
    st(api("GET", f"{CFG}/TC_DEF_{ctx.RUN}", t=ctx.T_B), 404, "APP_CONFIGURATION_NOT_FOUND")


# =============================================================================================
# Phase 6 — customer realm
# =============================================================================================
CUST = "/api/v1/public/customers"


def register(tenant, email, password=PW, name="Customer"):
    return api("POST", f"{CUST}/register", tc=tenant, body={"email": email, "password": password, "fullName": name})


def cust_login(tenant, email, password):
    return api("POST", f"{CUST}/login", tc=tenant, body={"email": email, "password": password})


@tc("TC-CORE-SEC-007")
def test_sec_007_customer_register(ctx):
    r = register(ctx.TA, f"c1-{ctx.run}@shop.test", name="Customer One")
    st(r, 201)
    d = r.data or {}
    ctx.C1_ID = d.get("id")
    eq(d.get("email"), f"c1-{ctx.run}@shop.test", "data.email")
    eq(d.get("statusCode"), "PENDING_VERIFICATION", "data.statusCode")
    eq(d.get("realm"), "CUSTOMER", "data.realm")


@tc("TC-CORE-SEC-008")
def test_sec_008_register_without_tenant(ctx):
    st(register(None, f"c1-{ctx.run}@shop.test"), 400, "TENANT_REQUIRED")


@tc("TC-CORE-SEC-009")
def test_sec_009_register_unknown_tenant(ctx):
    st(register(f"NOPE_{ctx.RUN}", f"c1-{ctx.run}@shop.test"), 404, "TENANT_NOT_FOUND")


@tc("TC-CORE-SEC-010")
def test_sec_010_register_validation(ctx):
    r = api("POST", f"{CUST}/register", tc=ctx.TA, body={"email": "not-an-email", "password": "short", "fullName": ""})
    st(r, 400, "VALIDATION_ERROR")
    f = {x.get("field") for x in r.field_errors}
    check({"email", "password", "fullName"} <= f, "fieldErrors ⊇ email,password,fullName", "email,fullName,password", sorted(f))


@tc("TC-CORE-SEC-011")
def test_sec_011_duplicate_email_per_tenant(ctx):
    st(register(ctx.TA, f"c1-{ctx.run}@shop.test", name="Customer One"), 409, "CUSTOMER_EMAIL_TAKEN")
    st(register(ctx.TB, f"c1-{ctx.run}@shop.test", name="Customer One"), 201)


@tc("TC-CORE-SEC-012")
def test_sec_012_same_email_staff_and_customer(ctx):
    e = f"c1-{ctx.run}@shop.test"
    r = api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(e, e, "موظف", "Staff", "StaffPass!9"))
    st(r, 201)
    eq((r.data or {}).get("realm"), "STAFF", "data.realm")
    r = login_staff(ctx.TA, e, "StaffPass!9")
    st(r, 200)
    eq(jwt_claims((r.data or {}).get("accessToken", "")).get("realm"), "STAFF", "staff JWT realm")
    st(cust_login(ctx.TA, e, "StaffPass!9"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-SEC-013")
def test_sec_013_unverified_cannot_login(ctx):
    st(cust_login(ctx.TA, f"c1-{ctx.run}@shop.test", PW), 403, "CUSTOMER_NOT_VERIFIED")


@tc("TC-CORE-SEC-028")
def test_sec_028_staff_get_does_not_expose_customer(ctx):
    r = api("GET", f"/api/v1/sec/users/{ctx.C1_ID}", t=ctx.T_A)
    st(r, 404, "SEC-404-USER")


@tc("TC-CORE-SEC-014")
def test_sec_014_verify_unknown_token(ctx):
    st(api("POST", f"{CUST}/verify", tc=ctx.TA, body={"token": "not-a-token"}), 409, "VERIFY_TOKEN_INVALID")


@tc("TC-CORE-SEC-015")
def test_sec_015_customer_login_rate_limited(ctx):
    e = f"rl-{ctx.run}@shop.test"
    obs = []
    for i in range(10):
        r = cust_login(ctx.TA, e, "wrong-password")
        obs.append((r.status, r.code))
    check(all(o == (401, "SEC-401-INVALID-CREDENTIALS") for o in obs), "calls 1–10 → 401 SEC-401-INVALID-CREDENTIALS",
          "10 × 401", obs, f"10 × POST {CUST}/login X-Tenant-Code:{ctx.TA} {{email:{e}}}")
    st(cust_login(ctx.TA, e, "wrong-password"), 429, "CUSTOMER_LOGIN_RATE_LIMITED")
    st(cust_login(ctx.TA, f"other.rl-{ctx.run}@shop.test", "wrong-password"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-SEC-016")
def test_sec_016_reset_request_same_answer(ctx):
    r1 = api("POST", f"{CUST}/password-reset/request", tc=ctx.TA, body={"email": f"c1-{ctx.run}@shop.test"})
    r2 = api("POST", f"{CUST}/password-reset/request", tc=ctx.TA, body={"email": f"nobody-{ctx.run}@shop.test"})
    st(r1, 200)
    st(r2, 200)
    eq((r1.data or {}).get("messageEn"), "If that email is registered, a reset link has been sent", "data.messageEn")
    check(bool((r1.data or {}).get("messageAr")), "data.messageAr set", "non-empty", (r1.data or {}).get("messageAr"))
    eq(r2.data, r1.data, "identical data for both")


@tc("TC-CORE-SEC-017")
def test_sec_017_reset_complete_unknown_token(ctx):
    st(api("POST", f"{CUST}/password-reset/complete", tc=ctx.TA, body={"token": "nope", "newPassword": PW}),
       409, "SEC-409-RESET-TOKEN-INVALID")


@tc("TC-CORE-SEC-018")
def test_sec_018_staff_token_refused_on_customer_endpoint(ctx):
    r = api("POST", "/api/v1/sec/dev/password-reset-token", t=ctx.T_A, body={"email": f"alice-{ctx.run}@t.test"})
    st(r, 200)
    tok = (r.data or {}).get("token")
    check(bool(tok) and bool((r.data or {}).get("expiresAt")), "data.token, data.expiresAt", "present", r.excerpt())
    if not tok:
        raise Blocked("no reset token")
    body = {"token": tok, "newPassword": "NewPass!77"}
    st(api("POST", f"{CUST}/password-reset/complete", tc=ctx.TA, body=body), 409, "SEC-409-RESET-TOKEN-INVALID")
    st(api("POST", "/api/v1/sec/auth/password-reset/complete", tc=ctx.TA, body=body), 200)
    r = login_staff(ctx.TA, f"alice-{ctx.run}", "NewPass!77")
    st(r, 200)
    if (r.data or {}).get("accessToken"):
        ctx.T_ALICE = r.data["accessToken"]
        ctx.ALICE_PW = "NewPass!77"


@tc("TC-CORE-SEC-019")
def test_sec_019_staff_token_on_customer_api(ctx):
    st(api("GET", "/api/v1/customers/me", t=ctx.T_A), 403, "REALM_MISMATCH")
    st(api("PATCH", "/api/v1/customers/me", t=ctx.T_A, body={"fullNameEn": "X"}), 403, "REALM_MISMATCH")


@tc("TC-CORE-SEC-020")
def test_sec_020_customer_api_needs_token(ctx):
    st(api("GET", "/api/v1/customers/me"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-SEC-029")
def test_sec_029_staff_update_refuses_customer(ctx):
    e = f"c5-{ctx.run}@shop.test"
    r = register(ctx.TA, e)
    st(r, 201)
    eq((r.data or {}).get("statusCode"), "PENDING_VERIFICATION", "data.statusCode")
    c5 = (r.data or {}).get("id")
    ctx.C5_ID = c5
    st(api("PUT", f"/api/v1/sec/users/{c5}", t=ctx.T_A, body={"email": e, "fullNameAr": "س", "fullNameEn": "Hijack"}),
       404, "SEC-404-USER")
    st(api("PUT", f"/api/v1/sec/users/{c5}/roles", t=ctx.T_A, body={"roleIds": [ctx.A_SYS_ADMIN_ID]}), 404, "SEC-404-USER")
    g = api("GET", f"/api/v1/sec/users/{c5}", t=ctx.T_A)
    roles = (g.data or {}).get("roles") if g.status == 200 else []
    check(not roles, "the customer keeps no roles", "[] (or not visible: 404)", f"{g.status} roles={roles}", g.req)


@tc("TC-CORE-SEC-030")
def test_sec_030_deactivate_reactivate_no_bypass(ctx):
    e = f"c4-{ctx.run}@shop.test"
    r = register(ctx.TA, e)
    st(r, 201)
    c4 = (r.data or {}).get("id")
    ctx.C4_ID = c4
    st(api("DELETE", f"/api/v1/sec/users/{c4}", t=ctx.T_A), 404, "SEC-404-USER")
    st(api("PATCH", f"/api/v1/sec/users/{c4}", t=ctx.T_A), 404, "SEC-404-USER")
    r = cust_login(ctx.TA, e, PW)
    st(r, 403, "CUSTOMER_NOT_VERIFIED")
    check(r.status != 200, "login must never answer 200", "!= 200", r.status, r.req)


@tc("TC-CORE-SEC-031")
def test_sec_031_staff_search_excludes_customers(ctx):
    r = api("POST", "/api/v1/sec/users/search", t=ctx.T_A, body={"size": 200})
    st(r, 200)
    realms = [x.get("realm") for x in r.content]
    ids_ = [x.get("userPk") for x in r.content]
    check(r.content and set(realms) == {"STAFF"}, "every row realm = STAFF (non-empty)", "{'STAFF'}", sorted(set(map(str, realms))))
    cust = [ctx.get(k) for k in ("C1_ID", "C4_ID", "C5_ID") if ctx.get(k)]
    check(cust and not [c for c in cust if c in ids_], "no customer id ($C1_ID, $C4_ID, $C5_ID) listed", f"none of {cust}", ids_)
    names = [x.get("username") for x in r.content]
    check(f"c1-{ctx.run}@shop.test" in names and "ta-admin" in names, "the staff c1 (SEC-012) and ta-admin are listed",
          "both present", names)
    r = api("POST", "/api/v1/sec/users/search", t=ctx.T_A,
            body={"filters": [{"field": "email", "operator": "EQUALS", "value": f"c1-{ctx.run}@shop.test"}]})
    st(r, 200)
    eq([x.get("realm") for x in r.content], ["STAFF"], "search by c1's e-mail → only the STAFF row")


@tc("TC-CORE-SEC-033")
def test_sec_033_dashboard_counts_staff_only(ctx):
    r = api("GET", "/api/v1/sec/dashboard", t=ctx.T_A)
    st(r, 200)
    uo = (r.data or {}).get("usersOverview") or {}
    s = api("POST", "/api/v1/sec/users/search", t=ctx.T_A, body={"size": 200})
    st(s, 200)
    staff_total = (s.data or {}).get("totalElements")
    eq(uo.get("total"), staff_total, "usersOverview.total = staff user search totalElements (A has 3 customers: c1, c4, c5)")
    act = len([x for x in s.content if x.get("statusCode") == "ACTIVE"])
    dis = len([x for x in s.content if x.get("statusCode") == "DISABLED"])
    eq((uo.get("active"), uo.get("disabled")), (act, dis), "usersOverview.active/disabled = staff rows by status")


@tc("TC-CORE-NOTIF-001")
def test_notif_001_register_queues_verify_mail(ctx):
    body = {"filters": [{"field": "recipientId", "operator": "EQUALS", "value": ctx.C1_ID},
                        {"field": "referenceType", "operator": "EQUALS", "value": "SEC_CUSTOMER_VERIFY_TOKEN"}]}
    r = poll(lambda: api("POST", "/api/v1/notifications/logs/search", t=ctx.T_A, body=body),
             lambda r: r.status == 200 and r.content and r.content[0].get("notificationStatusId") not in ("PENDING", "QUEUED"),
             timeout=10)
    st(r, 200)
    eq(len(r.content), 1, "exactly one row")
    if r.content:
        x = r.content[0]
        eq(x.get("channelTypeId"), "EMAIL", "channelTypeId")
        eq(x.get("notificationStatusId"), "SKIPPED_NO_PROVIDER", "notificationStatusId")
        eq(x.get("attempts"), 1, "attempts")
        eq(x.get("lastError"), "NOTIF_CHANNEL_UNAVAILABLE", "lastError")
        eq(x.get("sentAt"), None, "sentAt")


# =============================================================================================
# Phase 7 — files
# =============================================================================================
PNG = png_bytes(7)


@tc("TC-CORE-FILE-001")
def test_file_001_public_category(ctx):
    r = api("POST", "/api/v1/files/categories", t=ctx.T_A,
            body={"categoryCode": f"TC_PUB_{ctx.RUN}", "nameAr": "فئة", "nameEn": f"Public {ctx.RUN}", "allowPublic": True})
    st(r, 201)
    ctx.CAT_PUB = (r.data or {}).get("id")
    eq((r.data or {}).get("allowPublic"), True, "data.allowPublic")


@tc("TC-CORE-FILE-002")
def test_file_002_category_default_private(ctx):
    r = api("POST", "/api/v1/files/categories", t=ctx.T_A,
            body={"categoryCode": f"TC_PRV_{ctx.RUN}", "nameAr": "فئة", "nameEn": f"Private {ctx.RUN}"})
    st(r, 201)
    ctx.CAT_PRV = (r.data or {}).get("id")
    eq((r.data or {}).get("allowPublic"), False, "data.allowPublic")


def _file_003(ctx, provider):
    r = upload(ctx, ctx.T_A, "logo.png", "image/png", PNG, ctx.CAT_PUB)
    st(r, 201)
    d = r.data or {}
    ctx.DOC1 = d.get("id")
    eq(d.get("storageProvider"), provider, "data.storageProvider")
    eq(d.get("visibility"), "PRIVATE", "data.visibility")
    eq(d.get("publicUrl"), None, "data.publicUrl")
    eq(d.get("contentType"), "image/png", "data.contentType")
    eq(d.get("fileCategoryId"), ctx.CAT_PUB, "data.fileCategoryId")


@tc("TC-CORE-FILE-003")
def test_file_003_upload_db_private(ctx):
    _file_003(ctx, "DB")


def access_token(ctx, doc, token=None):
    return api("POST", f"/api/v1/files/{doc}/access-token", t=token or ctx.T_A)


def download(tok, bearer):
    return api("GET", "/api/v1/files/download?token=" + urllib.parse.quote(tok, safe=""), t=bearer)


def _file_004(ctx):
    r = access_token(ctx, ctx.DOC1)
    st(r, 200)
    tok = (r.data or {}).get("accessToken")
    check(bool(tok) and bool((r.data or {}).get("expiresAt")), "data.accessToken, data.expiresAt", "present", r.excerpt())
    if not tok:
        raise Blocked("no access token")
    ctx.DL_TOKEN = tok
    d = download(tok, ctx.T_A)
    check(d.status == 200 and d.body == PNG, "download 200 with the uploaded bytes", "200 + bytes",
          f"{d.status} len={len(d.body)} equal={d.body == PNG}", d.req)
    check(d.headers.get("content-type", "").startswith("image/png"), "Content-Type image/png", "image/png", d.headers.get("content-type"))


@tc("TC-CORE-FILE-004")
def test_file_004_private_download_token(ctx):
    _file_004(ctx)


def _file_005(ctx):
    st(download(ctx.DL_TOKEN, ctx.T_A), 401, "FILE_ACCESS_TOKEN_INVALID")


@tc("TC-CORE-FILE-005")
def test_file_005_token_single_use(ctx):
    _file_005(ctx)


@tc("TC-CORE-FILE-006")
def test_file_006_token_bound_to_caller(ctx):
    r = access_token(ctx, ctx.DOC1)
    st(r, 200)
    tok = (r.data or {}).get("accessToken") or "x"
    st(download(tok, ctx.T_ALICE), 401, "FILE_ACCESS_TOKEN_INVALID")
    d = download(tok, ctx.T_A)
    check(d.status == 200 and d.body == PNG, "owner download 200 with the bytes", "200 + bytes", f"{d.status} {d.excerpt(120)}", d.req)


@tc("TC-CORE-FILE-007")
def test_file_007_download_needs_auth(ctx):
    st(api("GET", "/api/v1/files/download?token=x"), 401, "SEC-401-INVALID-CREDENTIALS")


def publish(ctx, doc, vis="PUBLIC", token=None):
    return api("PATCH", f"/api/v1/files/{doc}/visibility", t=token or ctx.T_A, body={"visibility": vis})


def _file_008(ctx):
    r = publish(ctx, ctx.DOC1)
    st(r, 200)
    d = r.data or {}
    eq(d.get("visibility"), "PUBLIC", "data.visibility")
    url = d.get("publicUrl") or ""
    check(re.fullmatch(rf"/api/v1/public/files/{re.escape(ctx.TA)}/[A-Za-z0-9_-]{{32}}", url), "publicUrl pattern",
          f"^/api/v1/public/files/{ctx.TA}/[A-Za-z0-9_-]{{32}}$", url)
    ctx.URL1 = url


@tc("TC-CORE-FILE-008")
def test_file_008_publish_slug_url(ctx):
    _file_008(ctx)


def _public_headers_ok(resp, inline=True, ctype="image/png", body=None):
    h = resp.headers
    check(h.get("content-type", "").startswith(ctype), "Content-Type", ctype, h.get("content-type"))
    check(h.get("x-content-type-options") == "nosniff", "X-Content-Type-Options", "nosniff", h.get("x-content-type-options"))
    check(h.get("content-security-policy") == "sandbox; default-src 'none'", "Content-Security-Policy",
          "sandbox; default-src 'none'", h.get("content-security-policy"))
    disp = h.get("content-disposition", "")
    want = "inline" if inline else "attachment"
    check(disp.startswith(want), "Content-Disposition", f"{want}; …", disp)
    if body is not None:
        check(h.get("cache-control") == "max-age=86400, public", "Cache-Control", "max-age=86400, public", h.get("cache-control"))
        etag = '"' + hashlib.sha256(body).hexdigest() + '"'
        check(h.get("etag") == etag, "ETag", etag, h.get("etag"))


def _file_009(ctx):
    r = api("GET", ctx.URL1)
    check(r.status == 200 and r.body == PNG, "public GET 200 with the bytes", "200 + bytes", f"{r.status} {r.excerpt(120)}", r.req)
    _public_headers_ok(r, True, "image/png", PNG)


@tc("TC-CORE-FILE-009")
def test_file_009_public_get_headers(ctx):
    _file_009(ctx)


@tc("TC-CORE-FILE-010")
def test_file_010_public_get_head_only(ctx):
    r = api("HEAD", ctx.URL1)
    check(r.status == 200 and not r.body, "HEAD 200 without body", "200, no body", f"{r.status} len={len(r.body)}", r.req)
    _public_headers_ok(r, True, "image/png", PNG)
    st(api("POST", ctx.URL1), 401, "SEC-401-INVALID-CREDENTIALS")
    st(api("DELETE", ctx.URL1), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-FILE-011")
def test_file_011_republish_keeps_slug(ctx):
    r = publish(ctx, ctx.DOC1)
    st(r, 200)
    eq((r.data or {}).get("publicUrl"), ctx.URL1, "data.publicUrl")


@tc("TC-CORE-FILE-012")
def test_file_012_tokens_irrelevant_on_public(ctx):
    for tok, who in ((ctx.T_B, "T_B"), (ctx.T_A, "T_A")):
        r = api("GET", ctx.URL1, t=tok)
        check(r.status == 200 and r.body == PNG, f"GET $URL1 with {who} → 200 bytes", "200 + bytes", f"{r.status} {r.excerpt(150)}", r.req)


@tc("TC-CORE-FILE-013")
def test_file_013_html_svg_attachments(ctx):
    html = b"<html><body><script>alert(1)</script></body></html>"
    svg = b'<?xml version="1.0"?><svg xmlns="http://www.w3.org/2000/svg" onload="alert(1)"></svg>'
    for fname, ctype, content, exp_ct in (("x.html", "text/html", html, "text/html"),
                                          ("x.svg", "image/svg+xml", svg, "application/xml")):
        r = upload(ctx, ctx.T_A, fname, ctype, content, ctx.CAT_PUB)
        st(r, 201)
        p = publish(ctx, (r.data or {}).get("id"))
        st(p, 200)
        url = (p.data or {}).get("publicUrl")
        if not url:
            check(False, f"{fname} publicUrl", "set", p.excerpt())
            continue
        g = api("GET", url)
        st(g, 200, what=f"GET publicUrl of {fname}")
        _public_headers_ok(g, False, exp_ct)


@tc("TC-CORE-FILE-014")
def test_file_014_b_cannot_read_a_slug(ctx):
    slug = ctx.URL1.rsplit("/", 1)[-1]
    st(api("GET", f"/api/v1/public/files/{ctx.TB}/{slug}"), 404, "FILE_DOCUMENT_NOT_FOUND")


@tc("TC-CORE-FILE-015")
def test_file_015_b_cannot_publish_or_see(ctx):
    st(publish(ctx, ctx.DOC1, "PRIVATE", ctx.T_B), 404, "FILE_DOCUMENT_NOT_FOUND")
    st(api("GET", f"/api/v1/files/{ctx.DOC1}", t=ctx.T_B), 404, "FILE_DOCUMENT_NOT_FOUND")
    st(api("GET", ctx.URL1), 200, what="$DOC1 still PUBLIC (GET $URL1 → 200)")


@tc("TC-CORE-FILE-016")
def test_file_016_unknown_tenant_or_slug(ctx):
    st(api("GET", f"/api/v1/public/files/NOPE{ctx.RUN}/x"), 404, "TENANT_NOT_FOUND")
    st(api("GET", f"/api/v1/public/files/{ctx.TA}/doesNotExist"), 404, "FILE_DOCUMENT_NOT_FOUND")


def _file_017(ctx):
    r = upload(ctx, ctx.T_A, "p.png", "image/png", png_bytes(17), ctx.CAT_PRV)
    st(r, 201)
    ctx.DOC_PRV = (r.data or {}).get("id")
    r = upload(ctx, ctx.T_A, "n.png", "image/png", png_bytes(18))
    st(r, 201)
    ctx.DOC_NOCAT = (r.data or {}).get("id")
    for d in (ctx.DOC_PRV, ctx.DOC_NOCAT):
        st(publish(ctx, d), 409, "FILE_PUBLIC_NOT_ALLOWED")


@tc("TC-CORE-FILE-017")
def test_file_017_publish_needs_allowing_category(ctx):
    _file_017(ctx)


@tc("TC-CORE-FILE-018")
def test_file_018_visibility_failure_paths(ctx):
    st(publish(ctx, ctx.DOC1, "WORLD"), 400, "VALIDATION_ERROR")
    st(publish(ctx, 987654321), 404, "FILE_DOCUMENT_NOT_FOUND")
    st(api("PATCH", f"/api/v1/files/{ctx.DOC1}/visibility", body={"visibility": "PUBLIC"}), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-FILE-024")
def test_file_024_metadata_storage_visibility(ctx):
    r = api("GET", f"/api/v1/files/{ctx.DOC1}", t=ctx.T_A)
    st(r, 200)
    d = r.data or {}
    eq(d.get("storageProvider"), "DB", "data.storageProvider")
    eq(d.get("visibility"), "PUBLIC", "data.visibility")
    eq(d.get("publicUrl"), ctx.URL1, "data.publicUrl")


def _file_019(ctx):
    r = publish(ctx, ctx.DOC1, "PRIVATE")
    st(r, 200)
    eq((r.data or {}).get("visibility"), "PRIVATE", "data.visibility")
    eq((r.data or {}).get("publicUrl"), None, "data.publicUrl")
    st(api("GET", ctx.URL1), 404, "FILE_DOCUMENT_NOT_FOUND")


@tc("TC-CORE-FILE-019")
def test_file_019_unpublish(ctx):
    _file_019(ctx)


@tc("TC-CORE-FILE-020")
def test_file_020_withdraw_allow_public(ctx):
    r = upload(ctx, ctx.T_A, "e.png", "image/png", png_bytes(20), ctx.CAT_PUB)
    st(r, 201)
    ctx.DOC2 = (r.data or {}).get("id")
    p = publish(ctx, ctx.DOC2)
    st(p, 200)
    ctx.URL2 = (p.data or {}).get("publicUrl")
    r = api("PUT", f"/api/v1/files/categories/{ctx.CAT_PUB}", t=ctx.T_A,
            body={"nameAr": "فئة", "nameEn": f"Public {ctx.RUN}", "allowPublic": False})
    st(r, 200)
    eq((r.data or {}).get("allowPublic"), False, "PUT data.allowPublic")
    st(api("GET", ctx.URL2 or "/api/v1/public/files/x/y"), 404, "FILE_DOCUMENT_NOT_FOUND")
    r = api("GET", f"/api/v1/files/{ctx.DOC2}", t=ctx.T_A)
    st(r, 200)
    eq((r.data or {}).get("publicUrl"), None, "metadata data.publicUrl")


@tc("TC-CORE-FILE-021")
def test_file_021_publish_needs_permission(ctx):
    st(publish(ctx, ctx.DOC1, "PUBLIC", ctx.T_ALICE), 403, "ACCESS_DENIED")
    # a role with FILE_BROWSER VIEW + CREATE but without FILE:DOCUMENT:PUBLISH: reads, cannot publish
    t = limited_user(ctx)
    r = api("GET", f"/api/v1/files/{ctx.DOC1}", t=t)
    st(r, 200, what="T_LIM GET /api/v1/files/$DOC1 (PERM_FILE_BROWSER_VIEW) → 200")
    eq((r.data or {}).get("id"), ctx.DOC1, "data.id")
    st(publish(ctx, ctx.DOC1, "PUBLIC", t), 403, "ACCESS_DENIED", what="T_LIM PATCH visibility (no FILE:DOCUMENT:PUBLISH) → 403")
    g = api("GET", f"/api/v1/files/{ctx.DOC1}", t=ctx.T_A)
    eq((g.data or {}).get("visibility"), "PRIVATE", "$DOC1 unchanged (still PRIVATE after FILE-019)")


@tc("TC-CORE-FILE-023")
def test_file_023_archived_not_served(ctx):
    r = api("PUT", f"/api/v1/files/categories/{ctx.CAT_PUB}", t=ctx.T_A,
            body={"nameAr": "فئة", "nameEn": f"Public {ctx.RUN}", "allowPublic": True})
    st(r, 200, what="precondition: allowPublic true again")
    r = upload(ctx, ctx.T_A, "d.png", "image/png", png_bytes(23), ctx.CAT_PUB)
    st(r, 201)
    doc = (r.data or {}).get("id")
    p = publish(ctx, doc)
    st(p, 200)
    url = (p.data or {}).get("publicUrl") or "/api/v1/public/files/x/y"
    st(api("GET", url), 200, what="anonymous GET $URL4 before archive")
    st(api("DELETE", f"/api/v1/files/{doc}?action=ARCHIVE", t=ctx.T_A), 200)
    st(api("GET", url), 404, "FILE_DOCUMENT_NOT_FOUND")


# =============================================================================================
# Phase 8 — notifications
# =============================================================================================
def _dispatch_and_poll(ctx, body, final_pred, timeout=15):
    r = api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=body)
    st(r, 200)
    ids = (r.data or {}).get("logIds") or []
    eq(len(ids), 1, "data.logIds has one id")
    if not ids:
        raise Blocked("no log id")
    log = poll(lambda: get_log(ctx.T_A, ids[0]), lambda x: x.status == 200 and final_pred(x.data or {}), timeout=timeout)
    return ids[0], log


@tc("TC-CORE-NOTIF-002")
def test_notif_002_email_skipped_no_provider(ctx):
    lid, log = _dispatch_and_poll(ctx, dispatch_body(ctx.ALICE_ID, ["EMAIL"]),
                                  lambda d: d.get("notificationStatusId") not in ("PENDING", "QUEUED"))
    ctx.NOTIF2_LOG = lid
    d = log.data or {}
    eq(d.get("notificationStatusId"), "SKIPPED_NO_PROVIDER", "notificationStatusId")
    eq(d.get("attempts"), 1, "attempts")
    eq(d.get("lastError"), "NOTIF_CHANNEL_UNAVAILABLE", "lastError")
    time.sleep(2)
    again = get_log(ctx.T_A, lid)
    check((again.data or {}).get("notificationStatusId") != "FAILED", "never reaches FAILED", "!= FAILED",
          (again.data or {}).get("notificationStatusId"))


@tc("TC-CORE-NOTIF-004")
def test_notif_004_sms_no_provider(ctx):
    r = api("POST", "/api/v1/notifications/channels", t=ctx.T_A, body={"channelTypeId": "SMS", "isEnabledFl": True})
    st(r, 201)
    eq((r.data or {}).get("channelTypeId"), "SMS", "data.channelTypeId")
    lid, log = _dispatch_and_poll(ctx, dispatch_body(ctx.ALICE_ID, ["SMS"]),
                                  lambda d: d.get("notificationStatusId") not in ("PENDING", "QUEUED"))
    d = log.data or {}
    eq(d.get("notificationStatusId"), "SKIPPED_NO_PROVIDER", "notificationStatusId")
    eq(d.get("lastError"), "NOTIF_CHANNEL_UNAVAILABLE", "lastError")


@tc("TC-CORE-NOTIF-005")
def test_notif_005_unconfigured_channel_disabled(ctx):
    r = api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=dispatch_body(ctx.ALICE_ID, ["PUSH"]))
    st(r, 200)
    ids = (r.data or {}).get("logIds") or []
    eq(len(ids), 1, "one log id")
    if ids:
        log = get_log(ctx.T_A, ids[0])
        st(log, 200)
        eq((log.data or {}).get("notificationStatusId"), "CHANNEL_DISABLED", "notificationStatusId")
        eq((log.data or {}).get("attempts"), 0, "attempts")


@tc("TC-CORE-NOTIF-007")
def test_notif_007_in_app_inbox(ctx):
    lid, log = _dispatch_and_poll(ctx, dispatch_body(ctx.ALICE_ID, ["IN_APP"], "TEST_REF"),
                                  lambda d: d.get("notificationStatusId") not in ("PENDING", "QUEUED"))
    eq((log.data or {}).get("notificationStatusId"), "SENT", "log status")
    r = api("GET", "/api/v1/notif/inbox", t=ctx.T_ALICE)
    st(r, 200)
    first = (r.content[:1] or [{}])[0]
    ctx.INBOX1 = first.get("id")
    eq(first.get("recipientUserId"), ctx.ALICE_ID, "content[0].recipientUserId")
    eq(first.get("read"), False, "content[0].read")
    eq(first.get("readAt"), None, "content[0].readAt")
    eq(first.get("referenceType"), "TEST_REF", "content[0].referenceType")
    eq(first.get("referenceId"), 1, "content[0].referenceId")
    check(bool(first.get("titleEn")) and bool(first.get("bodyEn")), "titleEn/bodyEn set", "set", first)


@tc("TC-CORE-NOTIF-008")
def test_notif_008_mark_read_idempotent(ctx):
    r1 = api("PATCH", f"/api/v1/notif/inbox/{ctx.INBOX1}/read", t=ctx.T_ALICE)
    r2 = api("PATCH", f"/api/v1/notif/inbox/{ctx.INBOX1}/read", t=ctx.T_ALICE)
    for r in (r1, r2):
        st(r, 200)
        eq((r.data or {}).get("read"), True, "data.read")
        check(bool((r.data or {}).get("readAt")), "data.readAt set", "set", (r.data or {}).get("readAt"))
    eq((r2.data or {}).get("readAt"), (r1.data or {}).get("readAt"), "same readAt on both calls")
    r = api("GET", "/api/v1/notif/inbox?unreadOnly=true", t=ctx.T_ALICE)
    st(r, 200)
    check(ctx.INBOX1 not in [x.get("id") for x in r.content], "unread list excludes $INBOX1", "absent", [x.get("id") for x in r.content])
    r = api("GET", "/api/v1/notif/inbox", t=ctx.T_ALICE)
    st(r, 200)
    it = [x for x in r.content if x.get("id") == ctx.INBOX1]
    check(bool(it) and it[0].get("read") is True, "full list contains it with read=true", "read=true", it)


@tc("TC-CORE-NOTIF-009")
def test_notif_009_only_owner(ctx):
    st(api("PATCH", f"/api/v1/notif/inbox/{ctx.INBOX1}/read", t=ctx.T_A), 404, "INBOX_ITEM_NOT_FOUND")
    r = api("GET", "/api/v1/notif/inbox", t=ctx.T_A)
    st(r, 200)
    check(ctx.INBOX1 not in [x.get("id") for x in r.content], "ta-admin's inbox lacks $INBOX1", "absent", [x.get("id") for x in r.content])
    st(api("PATCH", "/api/v1/notif/inbox/987654321/read", t=ctx.T_ALICE), 404, "INBOX_ITEM_NOT_FOUND")
    st(api("GET", "/api/v1/notif/inbox"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-NOTIF-010")
def test_notif_010_inbox_tenant_isolated(ctx):
    st(api("PATCH", f"/api/v1/notif/inbox/{ctx.INBOX1}/read", t=ctx.T_B), 404, "INBOX_ITEM_NOT_FOUND")


@tc("TC-CORE-NOTIF-011")
def test_notif_011_unknown_recipient_no_log(ctx):
    r = api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=dispatch_body(987654321, ["EMAIL"]))
    st(r, 200)
    eq((r.data or {}).get("logIds"), [], "unknown recipient: data.logIds")
    # an inactive recipient: a DISABLED staff user of A
    r = api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(f"inact-{ctx.run}", f"inact-{ctx.run}@t.test", "معطل", "Inactive"))
    st(r, 201)
    uid = (r.data or {}).get("userPk")
    d = api("DELETE", f"/api/v1/sec/users/{uid}", t=ctx.T_A)
    st(d, 200)
    eq((d.data or {}).get("statusCode"), "DISABLED", "deactivated: data.statusCode")
    r = api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=dispatch_body(uid, ["EMAIL", "IN_APP"]))
    st(r, 200)
    eq((r.data or {}).get("logIds"), [], "inactive (DISABLED) recipient: data.logIds")
    s = api("POST", "/api/v1/notifications/logs/search", t=ctx.T_A,
            body={"filters": [{"field": "recipientId", "operator": "EQUALS", "value": uid}]})
    st(s, 200)
    eq(s.content, [], "no log row for the inactive recipient")


@tc("TC-CORE-NOTIF-013")
def test_notif_013_staff_token_on_customer_inbox(ctx):
    st(api("GET", "/api/v1/customers/me/inbox", t=ctx.T_ALICE), 403, "REALM_MISMATCH")
    st(api("PATCH", f"/api/v1/customers/me/inbox/{ctx.INBOX1}/read", t=ctx.T_ALICE), 403, "REALM_MISMATCH")


@tc("TC-CORE-NOTIF-016")
def test_notif_016_logs_tenant_isolated(ctx):
    st(get_log(ctx.T_B, ctx.NOTIF2_LOG), 404, "NOTIF_LOG_NOT_FOUND")


# =============================================================================================
# Phase 9 — audit
# =============================================================================================
@tc("TC-CORE-AUDIT-001")
def test_audit_001_user_create_row(ctx):
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="CREATE")
    st(r, 200)
    eq(len(r.content), 1, "exactly one row")
    if not r.content:
        return
    row = r.content[0]
    eq(row.get("actor"), "ta-admin", "actor")
    eq(row.get("actorRealm"), "STAFF", "actorRealm")
    check(bool(row.get("ip")), "ip set", "set", row.get("ip"))
    ch = changes_of(row)
    fields = {c.get("field") for c in ch}
    need = {"username", "email", "fullNameAr", "fullNameEn", "statusCode", "realm", "isActiveFl"}
    check(need <= fields, "changes fields ⊇ required", sorted(need), sorted(fields))
    bad = {"passwordHash", "lastLoginAt", "createdBy", "createdAt", "updatedBy", "updatedAt", "version", "tenantId"} & fields
    check(not bad, "no excluded field", "none", sorted(bad))
    blob = json.dumps(ch)
    check(PW not in blob and "$2a$" not in blob, "no value contains $PW or $2a$", "absent", "present" if (PW in blob or "$2a$" in blob) else "absent")


@tc("TC-CORE-AUDIT-002")
def test_audit_002_update_two_fields(ctx):
    r = api("PUT", f"/api/v1/sec/users/{ctx.ALICE_ID}", t=ctx.T_A,
            body={"email": f"alice-{ctx.run}@t.test", "fullNameAr": "اسم جديد", "fullNameEn": "New Name"})
    st(r, 200)
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="UPDATE")
    st(r, 200)
    eq(len(r.content), 1, "exactly one UPDATE row")
    if r.content:
        ch = changes_of(r.content[0])
        eq(sorted(c.get("field") for c in ch), ["fullNameAr", "fullNameEn"], "changes[*].field")
        en = [c for c in ch if c.get("field") == "fullNameEn"]
        eq((en[0].get("old"), en[0].get("new")) if en else None, ("Alice", "New Name"), "fullNameEn old/new")


@tc("TC-CORE-AUDIT-003")
def test_audit_003_login_row(ctx):
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="LOGIN")
    st(r, 200)
    check(len(r.content) >= 1, "≥ 1 LOGIN row", ">=1", len(r.content))
    if r.content:
        row = r.content[0]
        eq(row.get("actor"), f"alice-{ctx.run}", "actor")
        eq(row.get("actorRealm"), "STAFF", "actorRealm")
        eq(row.get("actorUserId"), ctx.ALICE_ID, "actorUserId")
    u = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="UPDATE")
    bad = [x for x in u.content if any(c.get("field") == "lastLoginAt" for c in changes_of(x))]
    check(not bad, "no UPDATE row has a lastLoginAt change", "none", len(bad))


@tc("TC-CORE-AUDIT-004")
def test_audit_004_logout_row(ctx):
    st(api("POST", "/api/v1/sec/auth/logout", t=ctx.T_ALICE), 200)
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="LOGOUT")
    st(r, 200)
    eq(len(r.content), 1, "one LOGOUT row")
    if r.content:
        eq(r.content[0].get("actor"), f"alice-{ctx.run}", "actor")
    r = login_staff(ctx.TA, f"alice-{ctx.run}", "NewPass!77")
    st(r, 200, what="log alice in again with NewPass!77")
    if (r.data or {}).get("accessToken"):
        ctx.T_ALICE = r.data["accessToken"]


@tc("TC-CORE-AUDIT-005")
def test_audit_005_password_reset_row(ctx):
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="PASSWORD_RESET")
    st(r, 200)
    eq(len(r.content), 1, "one row")
    if r.content:
        eq(r.content[0].get("actor"), f"alice-{ctx.run}", "actor")


@tc("TC-CORE-AUDIT-007")
def test_audit_007_tenant_isolated(ctx):
    r = audit(ctx.T_B, entityType="SEC_USER", entityId=ctx.ALICE_ID)
    st(r, 200)
    eq(r.content, [], "content (alice from B)")
    r = audit(ctx.T_B, actor="ta-admin", size=100)
    st(r, 200)
    eq(r.content, [], "content (actor ta-admin from B)")


@tc("TC-CORE-AUDIT-008")
def test_audit_008_permission_and_token(ctx):
    st(api("GET", "/api/v1/audit/events", t=ctx.T_NOPERM), 403, "ACCESS_DENIED")
    st(api("GET", "/api/v1/audit/events", t=ctx.T_ALICE), 403, "ACCESS_DENIED")
    st(api("GET", "/api/v1/audit/events"), 401, "SEC-401-INVALID-CREDENTIALS")
    # a user whose role grants other screens (SEQUENCE_SERIES, FILE_BROWSER) but not AUDIT:EVENT:READ
    t = limited_user(ctx)
    st(api("POST", "/api/v1/sequence/series/search", t=t, body={}), 200, what="T_LIM holds its own grants (series search 200)")
    st(api("GET", "/api/v1/audit/events", t=t), 403, "ACCESS_DENIED", what="T_LIM GET /api/v1/audit/events → 403")


@tc("TC-CORE-AUDIT-010")
def test_audit_010_dates_filters_order(ctx):
    st(api("GET", "/api/v1/audit/events?from=yesterday", t=ctx.T_A), 400, "VALIDATION_ERROR")
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID)
    st(r, 200)
    occ = [x.get("occurredAt") for x in r.content]
    check(occ == sorted(occ, reverse=True), "occurredAt non-increasing", "newest first", occ[:6])
    acts = [x.get("action") for x in r.content]
    check("UPDATE" in acts and "CREATE" in acts and acts.index("UPDATE") < acts.index("CREATE"),
          "both rows present and the UPDATE before the CREATE", "UPDATE … CREATE", acts)
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, action="CREATE", **{"from": "2000-01-01T00:00:00Z"})
    st(r, 200)
    check(r.content and all(x.get("action") == "CREATE" for x in r.content), "only CREATE", "CREATE only", [x.get("action") for x in r.content])
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.ALICE_ID, **{"from": "2999-01-01T00:00:00Z"})
    st(r, 200)
    eq(r.content, [], "content for from=2999")


@tc("TC-CORE-AUDIT-011")
def test_audit_011_tenant_create_audited_under_platform(ctx):
    r = audit(ctx.T_PLAT, entityType="CORE_TENANT", entityId=ctx.A_ID, action="CREATE")
    st(r, 200)
    eq(len(r.content), 1, "one row")
    if r.content:
        eq(r.content[0].get("actor"), "admin", "actor")
        code = [c for c in changes_of(r.content[0]) if c.get("field") == "code"]
        check(bool(code) and code[0].get("new") == ctx.TA, "changes include code = $TA", ctx.TA, code)
    r = audit(ctx.T_A, entityType="CORE_TENANT", entityId=ctx.A_ID, action="CREATE")
    st(r, 200)
    eq(r.content, [], "T_A content")


@tc("TC-CORE-AUDIT-012")
def test_audit_012_file_audit(ctx):
    r = audit(ctx.T_A, entityType="FILE_DOCUMENT", entityId=ctx.DOC1)
    st(r, 200)
    acts = [x.get("action") for x in r.content]
    check("CREATE" in acts, "a CREATE row", "present", acts)
    vis = [c for x in r.content if x.get("action") == "UPDATE" for c in changes_of(x)
           if c.get("field") == "visibility" and c.get("old") == "PRIVATE" and c.get("new") == "PUBLIC"]
    check(bool(vis), "≥ 1 UPDATE with visibility PRIVATE→PUBLIC", "present", [changes_of(x) for x in r.content][:4])
    bad = [c.get("field") for x in r.content for c in changes_of(x) if c.get("field") in ("contentHash", "fileContent")]
    check(not bad, "no contentHash/fileContent field", "none", bad)


@tc("TC-CORE-AUDIT-013")
def test_audit_013_series_audit(ctx):
    r = audit(ctx.T_PLAT, entityType="CORE_NUMBER_SERIES", entityId=ctx.SEQ_PLAT_ID)
    st(r, 200)
    acts = [x.get("action") for x in r.content]
    check("CREATE" in acts and "UPDATE" in acts, "CREATE and UPDATE rows", "both", acts)
    up = [c for x in r.content if x.get("action") == "UPDATE" for c in changes_of(x)]
    pre = [c for c in up if c.get("field") == "prefix"]
    check(bool(pre) and pre[0].get("old") == "INV" and pre[0].get("new") == "NEW", "prefix INV→NEW", "INV→NEW", pre)
    check(any(c.get("field") == "pattern" for c in up), "pattern change", "present", [c.get("field") for c in up])
    allf = [c.get("field") for x in r.content for c in changes_of(x)]
    check("nextValue" not in allf, "no nextValue field", "absent", allf)


@tc("TC-CORE-AUDIT-014")
def test_audit_014_settings_audit(ctx):
    r = audit(ctx.T_PLAT, entityType="CU_APP_CONFIGURATION", size=100)
    st(r, 200)
    rows = [x for x in r.content if str(x.get("entityId")) == str(ctx.CFG_PLAT_DEF_ID)]
    acts = [x.get("action") for x in rows]
    check("CREATE" in acts, "PLATFORM default CREATE row", "present", acts)
    ups = [changes_of(x) for x in rows if x.get("action") == "UPDATE"]
    check(any(c.get("field") == "configValue" and c.get("new") == "default-2" for ch in ups for c in ch),
          "UPDATE configValue → default-2", "present", ups)
    check(any(c.get("field") == "isActive" for ch in ups for c in ch), "UPDATE of isActive", "present", ups)
    r = audit(ctx.T_A, entityType="CU_APP_CONFIGURATION", size=100)
    st(r, 200)
    rows = [x for x in r.content if str(x.get("entityId")) == str(ctx.CFG_A_ID)]
    acts = [x.get("action") for x in rows]
    check("CREATE" in acts, "A override CREATE row", "present", acts)
    check(any(c.get("field") == "configValue" and c.get("new") == "tenant-value-2" for x in rows if x.get("action") == "UPDATE"
              for c in changes_of(x)), "A UPDATE (tenant-value-2)", "present", [changes_of(x) for x in rows])
    plat_ids = {str(ctx.get("CFG_PLAT_DEF_ID")), str(ctx.get("CFG_PLAT_OVR_ID"))}
    leak = [x.get("entityId") for x in r.content if str(x.get("entityId")) in plat_ids]
    check(not leak, "none of PLATFORM's rows in A", "none", leak)


def _all_audit(token):
    rows, page = [], 0
    while True:
        r = api("GET", f"/api/v1/audit/events?size=200&page={page}", t=token)
        if r.status != 200:
            return rows, r
        rows += r.content
        d = r.data or {}
        if d.get("last", True) or not r.content or page > 500:
            return rows, r
        page += 1


@tc("TC-CORE-AUDIT-016")
def test_audit_016_denylist_scan(ctx):
    deny = ["password", "secret", "token", "hash", "credential", "apikey", "privatekey", "salt", "configjson"]
    for who in ("T_PLAT", "T_A", "T_B"):
        rows, r = _all_audit(ctx[who])
        st(r, 200, what=f"GET /api/v1/audit/events?size=200 (all pages) as {who}")
        bad = sorted({c.get("field") for x in rows for c in changes_of(x)
                      if any(d in str(c.get("field", "")).lower() for d in deny)})
        check(not bad, f"no sensitive field in {len(rows)} rows ({who})", "none", bad)


# =============================================================================================
# Phase 10 — reports
# =============================================================================================
@tc("TC-CORE-REPORT-001")
def test_report_001_definitions(ctx):
    r = api("GET", "/api/v1/report/definitions", t=ctx.T_PLAT)
    st(r, 200)
    defs = r.data or []
    eq(sorted(x.get("code") for x in defs), sorted(["SEC_USER_LIST", "AUDIT_EVENT_LIST", "NOTIF_LOG_SUMMARY", "APP_SMOKE_REPORT"]), "codes")
    for x in defs:
        ok = all(x.get(k) for k in ("moduleCode", "titleAr", "titleEn")) and isinstance(x.get("params"), list) \
            and x.get("authority") == f"{x.get('moduleCode')}:REPORT:{x.get('code')}"
        check(ok, f"{x.get('code')} has moduleCode/titleAr/titleEn/authority/params", "<MODULE>:REPORT:<CODE>",
              {k: x.get(k) for k in ("moduleCode", "authority")})


@tc("TC-CORE-REPORT-002")
def test_report_002_one_definition_unknown(ctx):
    r = api("GET", "/api/v1/report/definitions/AUDIT_EVENT_LIST", t=ctx.T_PLAT)
    st(r, 200)
    d = r.data or {}
    eq(d.get("moduleCode"), "AUDIT", "data.moduleCode")
    eq(d.get("authority"), "AUDIT:REPORT:AUDIT_EVENT_LIST", "data.authority")
    eq([p.get("name") for p in d.get("params") or []], ["action", "entityType", "entityId", "actor", "occurredFrom", "occurredTo"], "params[*].name")
    st(api("GET", "/api/v1/report/definitions/NOPE", t=ctx.T_PLAT), 404, "REPORT_NOT_FOUND")
    st(api("POST", "/api/v1/report/NOPE/run", t=ctx.T_PLAT, body={}), 404, "REPORT_NOT_FOUND")


@tc("TC-CORE-REPORT-003")
def test_report_003_sec_user_list_isolated(ctx):
    r = api("POST", "/api/v1/report/SEC_USER_LIST/run", t=ctx.T_A, body={"params": {"realm": "STAFF"}, "page": 0, "size": 50})
    st(r, 200)
    d = r.data or {}
    eq(d.get("code"), "SEC_USER_LIST", "data.code")
    eq([c.get("key") for c in d.get("columns") or []],
       ["username", "email", "fullNameAr", "fullNameEn", "realm", "status", "active", "lastLoginAt", "createdAt"], "columns[*].key")
    names = [x.get("username") for x in d.get("rows") or []]
    allowed = {"ta-admin", f"alice-{ctx.run}", f"c1-{ctx.run}@shop.test", f"lim-{ctx.run}", f"inact-{ctx.run}"}
    check(set(names) <= allowed, "rows ⊆ A's staff users", sorted(allowed), names)
    required = {"ta-admin", f"alice-{ctx.run}", f"c1-{ctx.run}@shop.test"}
    check(required <= set(names), "rows contain ta-admin, alice and the staff c1", sorted(required), names)
    check("tb-admin" not in names and f"bob-{ctx.run}" not in names, "no B users", "absent", names)
    eq((d.get("page"), d.get("size"), d.get("totalRows")), (0, 50, len(names)), "page/size/totalRows")
    blob = json.dumps(d)
    check("$2a$" not in blob and "passwordHash" not in blob, "never a password hash", "absent", "ok" if "$2a$" not in blob else "hash present")


@tc("TC-CORE-REPORT-004")
def test_report_004_invalid_params_localized(ctx):
    r = api("POST", "/api/v1/report/SEC_USER_LIST/run", t=ctx.T_PLAT, lang="ar",
            body={"params": {"activeOnly": "maybe", "createdFrom": "2026-13-45", "bogus": "1"}})
    st(r, 400, "REPORT_PARAM_INVALID")
    f = {x.get("field") for x in r.field_errors}
    check({"activeOnly", "createdFrom", "bogus"} <= f, "fieldErrors ⊇ activeOnly,createdFrom,bogus", "all three", sorted(f))
    check(has_arabic(r.error.get("message")), "message in Arabic", "Arabic", r.error.get("message"))


CSV_AR = b"\xef\xbb\xbf" + "المعرف,التسمية\r\n1,alpha\r\n2,بيتا\r\n3,\"gamma, delta\"\r\n".encode("utf-8")


@tc("TC-CORE-REPORT-005")
def test_report_005_csv_arabic_exact(ctx):
    r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=csv", t=ctx.T_PLAT, lang="ar", body={"params": {}})
    st(r, 200)
    eq(r.headers.get("content-type"), "text/csv;charset=UTF-8", "Content-Type")
    disp = r.headers.get("content-disposition", "")
    check(disp.startswith("attachment") and "filename*=UTF-8''APP_SMOKE_REPORT.csv" in disp, "Content-Disposition",
          "attachment; filename*=UTF-8''APP_SMOKE_REPORT.csv", disp)
    check(r.body == CSV_AR, "exact body bytes", CSV_AR.hex(), r.body.hex())


@tc("TC-CORE-REPORT-006")
def test_report_006_csv_english(ctx):
    r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=csv", t=ctx.T_PLAT, lang="en", body={"params": {}})
    st(r, 200)
    check(r.body.startswith(b"\xef\xbb\xbfId,Label\r\n"), "after the BOM starts with Id,Label\\r\\n", "Id,Label", r.body[:40])


def csv_rows(body):
    text = body.decode("utf-8")
    if text.startswith("﻿"):
        text = text[1:]
    return list(csv.reader(io.StringIO(text)))


@tc("TC-CORE-REPORT-007")
def test_report_007_formula_injection_guard(ctx):
    st(api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(f"f-{ctx.run}", f"f-{ctx.run}@t.test", "صيغة", "=1+1")), 201)
    st(api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(f"g-{ctx.run}", f"g-{ctx.run}@t.test", "صيغة", "@SUM(A1)")), 201)
    r = api("POST", "/api/v1/report/SEC_USER_LIST/export?format=csv", t=ctx.T_A, lang="en", body={"params": {"realm": "STAFF"}})
    st(r, 200)
    rows = csv_rows(r.body) if r.status == 200 else [[]]
    hdr = rows[0] if rows else []
    idx = next((i for i, h in enumerate(hdr) if "english" in h.lower() and "name" in h.lower()), None)
    cells = [row[idx] for row in rows[1:] if idx is not None and len(row) > idx]
    check("'=1+1" in cells and "'@SUM(A1)" in cells, "fullNameEn cells apostrophe-prefixed", "'=1+1 and '@SUM(A1)",
          f"header={hdr} cells={cells}")


@tc("TC-CORE-REPORT-008")
def test_report_008_json_export_unknown_format(ctx):
    r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=json", t=ctx.T_PLAT, body={"params": {}})
    st(r, 200)
    check(r.headers.get("content-type", "").startswith("application/json"), "Content-Type application/json", "application/json", r.headers.get("content-type"))
    disp = r.headers.get("content-disposition", "")
    check(disp.startswith("attachment") and "filename*=UTF-8''APP_SMOKE_REPORT.json" in disp, "Content-Disposition", "attachment …json", disp)
    j = r.json if isinstance(r.json, dict) else {}
    j = j.get("data", j) if "success" in j else j
    eq((j.get("code"), len(j.get("columns") or []), len(j.get("rows") or [])), ("APP_SMOKE_REPORT", 2, 3), "code/columns/rows")
    r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=xml", t=ctx.T_PLAT, body={"params": {}})
    st(r, 400, "REPORT_PARAM_INVALID")
    eq((r.field_errors[:1] or [{}])[0].get("field"), "format", "fieldErrors[0].field")


@tc("TC-CORE-REPORT-009")
def test_report_009_no_report_permissions(ctx):
    r = api("GET", "/api/v1/report/definitions", t=ctx.T_ALICE)
    st(r, 200)
    eq(r.data, [], "data")
    st(api("GET", "/api/v1/report/definitions/SEC_USER_LIST", t=ctx.T_ALICE), 403, "ACCESS_DENIED")
    st(api("POST", "/api/v1/report/SEC_USER_LIST/run", t=ctx.T_ALICE, body={}), 403, "ACCESS_DENIED")
    st(api("POST", "/api/v1/report/SEC_USER_LIST/export", t=ctx.T_ALICE, body={}), 403, "ACCESS_DENIED")


@tc("TC-CORE-REPORT-010")
def test_report_010_needs_auth(ctx):
    st(api("GET", "/api/v1/report/definitions"), 401, "SEC-401-INVALID-CREDENTIALS")


@tc("TC-CORE-REPORT-011")
def test_report_011_per_report_grant(ctx):
    r = api("POST", "/api/v1/sec/roles", t=ctx.T_A, body={"code": f"TC_RPT_{ctx.RUN}", "nameAr": "تقارير", "nameEn": "Reports"})
    st(r, 201)
    role = (r.data or {}).get("rolePk")
    ctx.RPT_ROLE = role
    r = api("POST", "/api/v1/sec/registry/search", t=ctx.T_A, body={"pageCode": "SEC_REPORTS"})
    st(r, 200)
    mod_id = scr_id = view_id = rep_id = None
    for m in r.content:
        for s in m.get("screens") or []:
            if s.get("pageCode") == "SEC_REPORTS":
                mod_id, scr_id = m.get("moduleRegPk"), s.get("screenRegPk")
                for a in s.get("actions") or []:
                    if a.get("permissionCode") == "PERM_SEC_REPORTS_VIEW":
                        view_id = a.get("actionRegPk")
                    if a.get("permissionCode") == "SEC:REPORT:SEC_USER_LIST":
                        rep_id = a.get("actionRegPk")
    check(all([mod_id, scr_id, view_id, rep_id]), "registry ids found", "module/screen/VIEW/SEC_USER_LIST", (mod_id, scr_id, view_id, rep_id))
    st(api("POST", f"/api/v1/sec/roles/{role}/modules", t=ctx.T_A, body={"moduleId": mod_id}), 201)
    st(api("POST", f"/api/v1/sec/roles/{role}/screens", t=ctx.T_A, body={"screenId": scr_id}), 201)
    st(api("POST", f"/api/v1/sec/roles/{role}/actions", t=ctx.T_A, body={"actionId": view_id}), 201)
    st(api("POST", f"/api/v1/sec/roles/{role}/actions", t=ctx.T_A, body={"actionId": rep_id}), 201)
    r = api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(f"rpt-{ctx.run}", f"rpt-{ctx.run}@t.test", "تقارير", "Reporter"))
    st(r, 201)
    uid = (r.data or {}).get("userPk")
    st(api("PUT", f"/api/v1/sec/users/{uid}/roles", t=ctx.T_A, body={"roleIds": [role]}), 200)
    r = login_staff(ctx.TA, f"rpt-{ctx.run}", PW)
    st(r, 200)
    t = (r.data or {}).get("accessToken")
    ctx.T_RPT = t
    r = api("GET", "/api/v1/report/definitions", t=t)
    st(r, 200)
    eq([x.get("code") for x in r.data or []], ["SEC_USER_LIST"], "definitions data[*].code")
    st(api("POST", "/api/v1/report/SEC_USER_LIST/run", t=t, body={}), 200)
    st(api("POST", "/api/v1/report/NOTIF_LOG_SUMMARY/run", t=t, body={}), 403, "ACCESS_DENIED")


@tc("TC-CORE-AUDIT-015")
def test_audit_015_category_and_role_audit(ctx):
    r = audit(ctx.T_A, entityType="FILE_CATEGORY", entityId=ctx.CAT_PUB)
    st(r, 200)
    acts = [x.get("action") for x in r.content]
    check("CREATE" in acts, "FILE_CATEGORY CREATE", "present", acts)
    check(any(c.get("field") == "allowPublic" for x in r.content if x.get("action") == "UPDATE" for c in changes_of(x)),
          "UPDATE of allowPublic (FILE-020)", "present", [changes_of(x) for x in r.content])
    r = audit(ctx.T_A, entityType="SEC_ROLE", entityId=ctx.RPT_ROLE)
    st(r, 200)
    check("CREATE" in [x.get("action") for x in r.content], "SEC_ROLE CREATE", "present", [x.get("action") for x in r.content])


@tc("TC-CORE-REPORT-014")
def test_report_014_audit_event_list_isolated(ctx):
    r = api("POST", "/api/v1/report/AUDIT_EVENT_LIST/run", t=ctx.T_B, body={"params": {"action": "LOGIN"}, "size": 200})
    st(r, 200)
    rows = (r.data or {}).get("rows") or []
    check(rows and all(x.get("action") == "LOGIN" for x in rows), "B rows non-empty and all LOGIN", "LOGIN",
          {x.get("action") for x in rows})
    actors = {x.get("actor") for x in rows}
    check("tb-admin" in actors, "B rows contain tb-admin's LOGIN (TENANT-007)", "tb-admin", sorted(map(str, actors)))
    check("ta-admin" not in actors and f"alice-{ctx.run}" not in actors, "B has no A actors", "absent", sorted(map(str, actors)))
    r = api("POST", "/api/v1/report/AUDIT_EVENT_LIST/run", t=ctx.T_A, body={"params": {"action": "LOGIN"}, "size": 200})
    st(r, 200)
    actors = {x.get("actor") for x in (r.data or {}).get("rows") or []}
    check(f"alice-{ctx.run}" in actors and "tb-admin" not in actors, "A rows contain alice, no tb-admin", "alice, no tb-admin", sorted(map(str, actors)))


@tc("TC-CORE-REPORT-015")
def test_report_015_notif_log_summary(ctx):
    r = api("POST", "/api/v1/report/NOTIF_LOG_SUMMARY/run", t=ctx.T_A, body={"params": {}, "size": 200})
    st(r, 200)
    d = r.data or {}
    eq([c.get("key") for c in d.get("columns") or []], ["day", "channel", "status", "count"], "columns[*].key")
    rows = d.get("rows") or []
    def cnt(ch, s):
        return sum(int(x.get("count") or 0) for x in rows if x.get("channel") == ch and x.get("status") == s)
    check(cnt("EMAIL", "SKIPPED_NO_PROVIDER") >= 2, "EMAIL/SKIPPED_NO_PROVIDER count ≥ 2", ">=2", cnt("EMAIL", "SKIPPED_NO_PROVIDER"))
    check(cnt("IN_APP", "SENT") >= 1, "IN_APP/SENT row", ">=1", cnt("IN_APP", "SENT"))
    check(cnt("PUSH", "CHANNEL_DISABLED") >= 1, "PUSH/CHANNEL_DISABLED row", ">=1", cnt("PUSH", "CHANNEL_DISABLED"))
    rb = api("POST", "/api/v1/report/NOTIF_LOG_SUMMARY/run", t=ctx.T_B, body={"params": {}, "size": 200})
    st(rb, 200)
    brows = (rb.data or {}).get("rows") or []
    b_email = sum(int(x.get("count") or 0) for x in brows if x.get("channel") == "EMAIL")
    check(brows and b_email >= 1, "B's summary is non-empty (B's c1 verify mail, SEC-011)", "EMAIL >= 1",
          [(x.get("channel"), x.get("status"), x.get("count")) for x in brows])
    b_push = sum(int(x.get("count") or 0) for x in brows if x.get("channel") == "PUSH")
    b_inapp = sum(int(x.get("count") or 0) for x in brows if x.get("channel") == "IN_APP")
    check(b_push == 0 and b_inapp == 0, "B's rows do not include A's counts (no PUSH/IN_APP dispatch in B)", "0, 0", (b_push, b_inapp))


@tc("TC-CORE-REPORT-016")
def test_report_016_core_report_export(ctx):
    r = api("POST", "/api/v1/report/AUDIT_EVENT_LIST/export?format=csv", t=ctx.T_A, lang="en", body={"params": {"action": "LOGIN"}})
    st(r, 200)
    rows = csv_rows(r.body) if r.status == 200 else [[]]
    exp = "Occurred at,Action,Actor,Actor realm,Entity type,Entity id,Summary (Arabic),Summary (English),IP address,Reference"
    eq(",".join(rows[0]) if rows else "", exp, "header line")
    check(len(rows) > 1 and all(len(x) > 1 and x[1] == "LOGIN" for x in rows[1:]),
          "at least one data row, every data row Action = LOGIN", "LOGIN",
          sorted({x[1] if len(x) > 1 else "" for x in rows[1:]}))


@tc("TC-CORE-REPORT-017")
def test_report_017_invalid_lookup_param(ctx):
    r = api("POST", "/api/v1/report/NOTIF_LOG_SUMMARY/run", t=ctx.T_PLAT, body={"params": {"channel": "BOGUS"}})
    st(r, 400, "REPORT_PARAM_INVALID")
    eq((r.field_errors[:1] or [{}])[0].get("field"), "channel", "fieldErrors[0].field")
    st(api("POST", "/api/v1/report/NOTIF_LOG_SUMMARY/run", t=ctx.T_PLAT, body={"params": {"channel": "EMAIL", "status": "SENT"}}), 200)


@tc("TC-CORE-APP-001")
def test_app_001_app_report_runs(ctx):
    r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/run", t=ctx.T_PLAT, body={"params": {}})
    st(r, 200)
    d = r.data or {}
    eq([c.get("key") for c in d.get("columns") or []], ["id", "label"], "columns[*].key")
    eq(d.get("rows"), [{"id": 1, "label": "alpha"}, {"id": 2, "label": "بيتا"}, {"id": 3, "label": "gamma, delta"}], "rows")
    st(api("POST", "/api/v1/report/APP_SMOKE_REPORT/run", t=ctx.T_PLAT, body={"params": {"label": "alpha"}}), 200)


# =============================================================================================
# Phase 11 — tenant lifecycle
# =============================================================================================
def tstatus(ctx, tid, code):
    return api("PATCH", f"/api/v1/platform/tenants/{tid}/status", t=ctx.T_PLAT, body={"statusCode": code})


@tc("TC-CORE-TENANT-025")
def test_tenant_025_lists_isolated_and_lookups_copied(ctx):
    # file categories: B's search never shows A's TC_PUB/TC_PRV
    r = api("POST", "/api/v1/files/categories/search", t=ctx.T_B, body={"size": 200})
    st(r, 200)
    codes = {x.get("categoryCode") for x in r.content}
    check(not ({f"TC_PUB_{ctx.RUN}", f"TC_PRV_{ctx.RUN}"} & codes), "B's category search has no A category", "absent", sorted(map(str, codes)))
    ra = api("POST", "/api/v1/files/categories/search", t=ctx.T_A, body={"size": 200})
    check({f"TC_PUB_{ctx.RUN}", f"TC_PRV_{ctx.RUN}"} <= {x.get("categoryCode") for x in ra.content},
          "A's own search shows both", "present", ra.excerpt(200), ra.req)
    # files listed by owner (FILE-003's owner 4711/PRODUCT/SHOP): A sees its documents, B none of them
    q = "/api/v1/files?ownerId=4711&ownerType=PRODUCT&moduleCode=SHOP&size=200"
    fa = api("GET", q, t=ctx.T_A)
    fb = api("GET", q, t=ctx.T_B)
    st(fa, 200)
    st(fb, 200)
    a_ids = {x.get("id") for x in fa.content}
    check(ctx.DOC1 in a_ids, "A's owner list contains $DOC1", ctx.DOC1, sorted(map(str, a_ids)))
    check(not (a_ids & {x.get("id") for x in fb.content}), "B's owner list shares no document with A's", "disjoint",
          [x.get("id") for x in fb.content])
    # roles: B's search never shows A's TC_RPT/TC_LIM roles
    r = api("POST", "/api/v1/sec/roles/search", t=ctx.T_B, body={"size": 200})
    st(r, 200)
    rcodes = {x.get("code") for x in r.content}
    check(r.content and not ({f"TC_RPT_{ctx.RUN}", f"TC_LIM_{ctx.RUN}"} & rcodes), "B's role search has no A role (non-empty)",
          "absent", sorted(map(str, rcodes)))
    # templates: both tenants hold the same codes, as separate rows
    ta = api("POST", "/api/v1/notifications/templates/search", t=ctx.T_A, body={"size": 200})
    tb = api("POST", "/api/v1/notifications/templates/search", t=ctx.T_B, body={"size": 200})
    st(ta, 200)
    st(tb, 200)
    check(ta.content and tb.content and not ({x.get("id") for x in ta.content} & {x.get("id") for x in tb.content}),
          "A's and B's template rows are disjoint (no shared id)", "disjoint",
          ([x.get("id") for x in ta.content], [x.get("id") for x in tb.content]))
    # provisioning copied PLATFORM's lookup catalog: same keys, different rows
    lp = api("POST", "/api/v1/mdl/lookup-types/search", t=ctx.T_PLAT, body={"size": 200})
    la = api("POST", "/api/v1/mdl/lookup-types/search", t=ctx.T_A, body={"size": 200})
    st(lp, 200)
    st(la, 200)
    pk = {x.get("key") for x in lp.content}
    ak = {x.get("key") for x in la.content}
    check(pk and pk == ak, "A's lookup-type keys = PLATFORM's", sorted(map(str, pk)), sorted(map(str, ak)))
    check(not ({x.get("lookupTypePk") for x in lp.content} & {x.get("lookupTypePk") for x in la.content}),
          "A's lookup types are its own rows (no shared id)", "disjoint",
          ([x.get("lookupTypePk") for x in lp.content], [x.get("lookupTypePk") for x in la.content]))


@tc("TC-CORE-TENANT-019")
def test_tenant_019_platform_protected(ctx):
    st(tstatus(ctx, 1, "SUSPENDED"), 422, "TENANT_PLATFORM_PROTECTED")
    st(tstatus(ctx, ctx.A_ID, "BOGUS"), 400, "VALIDATION_ERROR")
    st(tstatus(ctx, 999999999, "ACTIVE"), 404, "TENANT_NOT_FOUND")
    r = api("GET", "/api/v1/platform/tenants/1", t=ctx.T_PLAT)
    eq((r.data or {}).get("statusCode"), "ACTIVE", "PLATFORM still ACTIVE")


@tc("TC-CORE-TENANT-020")
def test_tenant_020_suspend_c(ctx):
    r = tstatus(ctx, ctx.C_ID, "SUSPENDED")
    st(r, 200)
    eq((r.data or {}).get("statusCode"), "SUSPENDED", "data.statusCode")


@tc("TC-CORE-TENANT-021")
def test_tenant_021_suspended_cannot_login(ctx):
    st(login_staff(ctx.TC, "tc-admin", PW), 403, "TENANT_SUSPENDED")


@tc("TC-CORE-TENANT-022")
def test_tenant_022_issued_tokens_stop(ctx):
    st(api("GET", "/api/v1/sec/menu", t=ctx.T_C), 403, "TENANT_SUSPENDED")


@tc("TC-CORE-TENANT-023")
def test_tenant_023_suspended_customer_and_public(ctx):
    st(register(ctx.TC, f"s-{ctx.run}@shop.test", name="S"), 403, "TENANT_SUSPENDED")
    st(api("GET", f"/api/v1/public/files/{ctx.TC}/anything"), 403, "TENANT_SUSPENDED")


@tc("TC-CORE-TENANT-024")
def test_tenant_024_reactivate(ctx):
    r = tstatus(ctx, ctx.C_ID, "ACTIVE")
    st(r, 200)
    eq((r.data or {}).get("statusCode"), "ACTIVE", "data.statusCode")
    st(login_staff(ctx.TC, "tc-admin", PW), 200)


# =============================================================================================
# Phase 12 — profile runs
# =============================================================================================
SINK = {"sink": None}


def mail_token(ctx, to, template_hint, timeout=25):
    sink = SINK["sink"]
    if sink is None:
        raise Blocked("no SMTP sink")
    seen = ctx.setdefault("_seen_mail", set())

    def pred(m):
        key = m.get("Message-ID") or str(id(m))
        if key in seen:
            return False
        rc = (m.get("To") or "") + (m.get("X-Sink-Rcpt") or "")
        return to.lower() in rc.lower() and "token=" in sink.text_of(m)
    m = sink.wait_for(pred, timeout)
    if m is None:
        return None, None
    seen.add(m.get("Message-ID") or str(id(m)))
    text = sink.text_of(m)
    mt = re.search(r"[?&]token=([^\s\"'<>&]+)", text)
    return (urllib.parse.unquote(mt.group(1)) if mt else None), m.get("Subject")


@tc("TC-CORE-SEC-021", profile="P-MAIL")
def test_sec_021_verify_single_use(ctx):
    e = f"c2-{ctx.run}@shop.test"
    r = register(ctx.TA, e, name="Customer Two")
    st(r, 201)
    ctx.C2_ID = (r.data or {}).get("id")
    tok, subj = mail_token(ctx, e, "CUSTOMER_VERIFY_EMAIL")
    check(bool(tok), "captured CUSTOMER_VERIFY_EMAIL mail yields token", "token", f"subject={subj!r} token={'yes' if tok else 'no'}")
    if not tok:
        raise Blocked("no verify token captured")
    r = api("POST", f"{CUST}/verify", tc=ctx.TA, body={"token": tok})
    st(r, 200)
    eq((r.data or {}).get("statusCode"), "ACTIVE", "data.statusCode")
    eq((r.data or {}).get("realm"), "CUSTOMER", "data.realm")
    st(api("POST", f"{CUST}/verify", tc=ctx.TA, body={"token": tok}), 409, "VERIFY_TOKEN_INVALID")


@tc("TC-CORE-SEC-022", profile="P-MAIL")
def test_sec_022_verified_customer_login(ctx):
    r = cust_login(ctx.TA, f"c2-{ctx.run}@shop.test", PW)
    st(r, 200)
    d = r.data or {}
    if d.get("accessToken"):
        ctx.T_CUST = d["accessToken"]
    eq(d.get("tokenType"), "Bearer", "tokenType")
    c = jwt_claims(d.get("accessToken", ""))
    eq(c.get("realm"), "CUSTOMER", "JWT realm")
    eq(c.get("tid"), ctx.A_ID, "JWT tid")


@tc("TC-CORE-SEC-023", profile="P-MAIL")
def test_sec_023_read_my_profile(ctx):
    r = api("GET", "/api/v1/customers/me", t=ctx.T_CUST)
    st(r, 200)
    d = r.data or {}
    eq((d.get("id"), d.get("email"), d.get("realm"), d.get("statusCode")),
       (ctx.C2_ID, f"c2-{ctx.run}@shop.test", "CUSTOMER", "ACTIVE"), "id/email/realm/statusCode")


@tc("TC-CORE-SEC-024", profile="P-MAIL")
def test_sec_024_profile_update_mass_assignment(ctx):
    r = api("PATCH", "/api/v1/customers/me", t=ctx.T_CUST,
            body={"fullNameAr": "اسم جديد", "fullNameEn": "New Name", "email": f"evil-{ctx.run}@x.test",
                  "statusCode": "SUSPENDED", "realm": "STAFF", "id": 1})
    g = api("GET", "/api/v1/customers/me", t=ctx.T_CUST)
    st(r, 200)
    st(g, 200)
    for resp in (r, g):
        d = resp.data or {}
        eq((d.get("fullNameEn"), d.get("fullNameAr")), ("New Name", "اسم جديد"), "names updated")
        eq((d.get("email"), d.get("statusCode"), d.get("realm"), d.get("id")),
           (f"c2-{ctx.run}@shop.test", "ACTIVE", "CUSTOMER", ctx.C2_ID), "immutable fields unchanged")


@tc("TC-CORE-SEC-025", profile="P-MAIL")
def test_sec_025_customer_token_on_staff(ctx):
    st(api("POST", "/api/v1/sec/users/search", t=ctx.T_CUST, body={"size": 10}), 403, "REALM_MISMATCH")
    st(api("GET", "/api/v1/sec/menu", t=ctx.T_CUST), 403, "REALM_MISMATCH")


@tc("TC-CORE-SEC-026", profile="P-MAIL")
def test_sec_026_full_customer_reset(ctx):
    e = f"c2-{ctx.run}@shop.test"
    st(api("POST", f"{CUST}/password-reset/request", tc=ctx.TA, body={"email": e}), 200)
    tok, subj = mail_token(ctx, e, "CUSTOMER_PASSWORD_RESET")
    check(bool(tok), "captured CUSTOMER_PASSWORD_RESET mail yields token", "token", f"subject={subj!r}")
    if not tok:
        raise Blocked("no reset token captured")
    body = {"token": tok, "newPassword": "Cust0mer!New"}
    r = api("POST", f"{CUST}/password-reset/complete", tc=ctx.TA, body=body)
    st(r, 200)
    eq((r.data or {}).get("messageEn"), "Your password has been updated", "data.messageEn")
    r = cust_login(ctx.TA, e, "Cust0mer!New")
    st(r, 200, what="login with the new password")
    if (r.data or {}).get("accessToken"):
        ctx.T_CUST = r.data["accessToken"]   # refreshed verified-customer token for the later P-MAIL cases
    st(cust_login(ctx.TA, e, PW), 401, "SEC-401-INVALID-CREDENTIALS")
    st(api("POST", f"{CUST}/password-reset/complete", tc=ctx.TA, body=body), 409, "SEC-409-RESET-TOKEN-INVALID")


@tc("TC-CORE-SEC-027", profile="P-MAIL")
def test_sec_027_reset_verifies_unverified(ctx):
    e = f"c3-{ctx.run}@shop.test"
    r = register(ctx.TA, e)
    st(r, 201)
    eq((r.data or {}).get("statusCode"), "PENDING_VERIFICATION", "statusCode")
    mail_token(ctx, e, "CUSTOMER_VERIFY_EMAIL")  # consume the verify mail so the next token is the reset one
    st(api("POST", f"{CUST}/password-reset/request", tc=ctx.TA, body={"email": e}), 200)
    tok, subj = mail_token(ctx, e, "CUSTOMER_PASSWORD_RESET")
    if not tok:
        check(False, "reset mail token", "token", subj)
        raise Blocked("no reset token captured")
    st(api("POST", f"{CUST}/password-reset/complete", tc=ctx.TA, body={"token": tok, "newPassword": "Cust0mer!C3"}), 200)
    st(cust_login(ctx.TA, e, "Cust0mer!C3"), 200)


def _mail_dispatch_alice(ctx):
    return api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=dispatch_body(ctx.ALICE_ID, ["EMAIL"]))


@tc("TC-CORE-NOTIF-003", profile="P-MAIL")
def test_notif_003_email_sent(ctx):
    lid, log = _dispatch_and_poll(ctx, dispatch_body(ctx.ALICE_ID, ["EMAIL"]),
                                  lambda d: d.get("notificationStatusId") not in ("PENDING", "QUEUED"), timeout=20)
    d = log.data or {}
    eq(d.get("notificationStatusId"), "SENT", "notificationStatusId")
    eq(d.get("attempts"), 1, "attempts")
    check(bool(d.get("sentAt")), "sentAt set", "set", d.get("sentAt"))
    to = f"alice-{ctx.run}@t.test"
    m = SINK["sink"].wait_for(lambda m: to in ((m.get("To") or "") + (m.get("X-Sink-Rcpt") or "")), 5) if SINK["sink"] else None
    check(m is not None, f"sink received one mail to {to}", "1 mail", "received" if m else f"none (log lastError={d.get('lastError')!r})")


@tc("TC-CORE-NOTIF-012", profile="P-MAIL")
def test_notif_012_customer_inbox(ctx):
    lid, log = _dispatch_and_poll(ctx, dispatch_body(ctx.C2_ID, ["IN_APP"], "TEST_REF"),
                                  lambda d: d.get("notificationStatusId") not in ("PENDING", "QUEUED"))
    r = api("GET", "/api/v1/customers/me/inbox", t=ctx.T_CUST)
    st(r, 200)
    it = [x for x in r.content if x.get("recipientUserId") == ctx.C2_ID]
    check(bool(it), "list has the item (recipientUserId = $C2_ID)", "present", r.excerpt())
    if it:
        p = api("PATCH", f"/api/v1/customers/me/inbox/{it[0].get('id')}/read", t=ctx.T_CUST)
        st(p, 200)
        eq((p.data or {}).get("read"), True, "data.read")


@tc("TC-CORE-NOTIF-014", profile="P-MAIL")
def test_notif_014_customer_on_staff_inbox(ctx):
    st(api("GET", "/api/v1/notif/inbox", t=ctx.T_CUST), 403, "REALM_MISMATCH")


@tc("TC-CORE-NOTIF-015", profile="P-MAIL")
def test_notif_015_staff_with_customer_name(ctx):
    e = f"c2-{ctx.run}@shop.test"
    st(api("POST", "/api/v1/sec/users", t=ctx.T_A, body=user_body(e, e, "موظف", "Staff", "StaffPass!9")), 201)
    r = login_staff(ctx.TA, e, "StaffPass!9")
    st(r, 200)
    t = (r.data or {}).get("accessToken")
    r = api("GET", "/api/v1/notif/inbox", t=t)
    st(r, 200)
    check(not [x for x in r.content if x.get("recipientUserId") == ctx.C2_ID], "no item of $C2_ID", "none", r.excerpt())


@tc("TC-CORE-SEC-034", profile="P-MAIL")
def test_sec_034_same_email_both_realms_login(ctx):
    e = f"c2-{ctx.run}@shop.test"
    r = login_staff(ctx.TA, e, "StaffPass!9")
    st(r, 200, what="staff login c2 (NOTIF-015's staff account)")
    ts = (r.data or {}).get("accessToken") or ""
    eq(jwt_claims(ts).get("realm"), "STAFF", "staff JWT realm")
    s = api("POST", "/api/v1/sec/users/search", t=ctx.T_A,
            body={"filters": [{"field": "username", "operator": "EQUALS", "value": e}]})
    st(s, 200)
    staff_pk = [x.get("userPk") for x in s.content]
    eq(len(staff_pk), 1, "exactly one STAFF row named c2")
    r = cust_login(ctx.TA, e, "Cust0mer!New")
    st(r, 200, what="customer login c2 (password set by SEC-026)")
    tc_ = (r.data or {}).get("accessToken") or ""
    eq(jwt_claims(tc_).get("realm"), "CUSTOMER", "customer JWT realm")
    me = api("GET", "/api/v1/customers/me", t=tc_)
    st(me, 200)
    mid = (me.data or {}).get("id")
    eq(mid, ctx.C2_ID, "/customers/me id = $C2_ID")
    check(staff_pk and mid not in staff_pk, "/customers/me id != the staff userPk", f"!= {staff_pk}", mid)


@tc("TC-CORE-SEC-032", profile="P-MAIL")
def test_sec_032_staff_sessions_exclude_customers(ctx):
    def staff_sessions(t):
        r = api("POST", "/api/v1/sec/sessions/search", t=t, body={"size": 200})
        st(r, 200)
        return r, [x.get("activeSessionPk") for x in r.content]
    r = login_staff(ctx.TA, "ta-admin", PW)
    st(r, 200)
    t1 = r.data["accessToken"]
    _, before = staff_sessions(t1)
    r = cust_login(ctx.TA, f"c2-{ctx.run}@shop.test", "Cust0mer!New")
    st(r, 200, what="customer login c2 → a new customer session")
    t_cust = (r.data or {}).get("accessToken")
    r = login_staff(ctx.TA, "ta-admin", PW)
    st(r, 200)
    t2 = r.data["accessToken"]
    lst, after = staff_sessions(t2)
    check(before and after, "the staff lists are non-empty", "non-empty", (before, after))
    check(ctx.C2_ID not in [x.get("userId") for x in lst.content], "no listed session belongs to $C2_ID", "absent",
          sorted({x.get("userId") for x in lst.content}))
    lo, hi = max(before or [0]), max(after or [0])
    hidden = [i for i in range(lo + 1, hi) if i not in after]
    check(len(hidden) >= 1, "the ids allocated between the two staff logins that the staff list does not show (the customer's)",
          ">= 1", hidden)
    for sid in hidden:
        st(api("DELETE", f"/api/v1/sec/sessions/{sid}", t=t2), 404, "SEC-404-SESSION", what=f"DELETE /api/v1/sec/sessions/{sid} (not a staff session)")
    st(api("GET", "/api/v1/customers/me", t=t_cust), 200, what="the customer's session is still live (GET /customers/me)")
    d = api("GET", "/api/v1/sec/dashboard", t=t2)
    st(d, 200)
    eq(((d.data or {}).get("activeSessions") or {}).get("count"), (lst.data or {}).get("totalElements"),
       "dashboard activeSessions.count = staff session search totalElements (live customer sessions not counted)")


@tc("TC-CORE-TENANT-026", profile="P-MAIL")
def test_tenant_026_suspended_tenant_customer_token(ctx):
    st(api("GET", "/api/v1/customers/me", t=ctx.T_CUST), 200, what="before: T_CUST works")
    r = api("PATCH", f"/api/v1/platform/tenants/{ctx.A_ID}/status", t=ctx.T_PLAT, body={"statusCode": "SUSPENDED"})
    st(r, 200, what="suspend the run's tenant A")
    try:
        st(api("GET", "/api/v1/customers/me", t=ctx.T_CUST), 403, "TENANT_SUSPENDED", what="issued customer token refused")
        st(cust_login(ctx.TA, f"c2-{ctx.run}@shop.test", "Cust0mer!New"), 403, "TENANT_SUSPENDED", what="customer login refused")
    finally:
        r = api("PATCH", f"/api/v1/platform/tenants/{ctx.A_ID}/status", t=ctx.T_PLAT, body={"statusCode": "ACTIVE"})
        st(r, 200, what="re-activate tenant A")


@tc("TC-CORE-FILE-022", profile="P-MAIL")
def test_file_022_customer_tokens_on_files(ctx):
    r = api("POST", "/api/v1/files/categories", t=ctx.T_A,
            body={"categoryCode": f"TC_PUB_{ctx.RUN}", "nameAr": "فئة", "nameEn": f"Public {ctx.RUN}", "allowPublic": True})
    st(r, 201, what="setup FILE-001")
    cat = (r.data or {}).get("id")
    r = upload(ctx, ctx.T_A, "logo.png", "image/png", PNG, cat)
    st(r, 201, what="setup FILE-003")
    doc = (r.data or {}).get("id")
    p = publish(ctx, doc)
    st(p, 200, what="setup FILE-008")
    url = (p.data or {}).get("publicUrl") or "/x"
    g = api("GET", url, t=ctx.T_CUST)
    check(g.status == 200 and g.body == PNG, "GET $URL3 with T_CUST → 200 (like anonymous)", "200 + bytes", f"{g.status} {g.excerpt(150)}", g.req)
    st(api("POST", url, t=ctx.T_CUST), 405, "METHOD_NOT_ALLOWED")
    st(api("GET", f"/api/v1/files/{doc}", t=ctx.T_CUST), 403, "REALM_MISMATCH")


@tc("TC-CORE-AUDIT-006", profile="P-MAIL")
def test_audit_006_customer_login_audited(ctx):
    r = audit(ctx.T_A, entityType="SEC_USER", entityId=ctx.C2_ID, action="LOGIN")
    st(r, 200)
    check(len(r.content) >= 1, "≥ 1 row", ">=1", len(r.content))
    if r.content:
        eq(r.content[0].get("actorRealm"), "CUSTOMER", "actorRealm")
        eq(r.content[0].get("actor"), f"c2-{ctx.run}@shop.test", "actor")


@tc("TC-CORE-AUDIT-009", profile="P-MAIL")
def test_audit_009_customer_token_403(ctx):
    st(api("GET", "/api/v1/audit/events", t=ctx.T_CUST), 403, "REALM_MISMATCH")


@tc("TC-CORE-REPORT-012", profile="P-MAIL")
def test_report_012_customer_token_403(ctx):
    st(api("GET", "/api/v1/report/definitions", t=ctx.T_CUST), 403, "REALM_MISMATCH")
    st(api("POST", "/api/v1/report/SEC_USER_LIST/run", t=ctx.T_CUST, body={}), 403, "REALM_MISMATCH")
    st(api("POST", "/api/v1/report/SEC_USER_LIST/export", t=ctx.T_CUST, body={}), 403, "REALM_MISMATCH")


@tc("TC-CORE-PLATFORM-004", profile="P-MAIL")
def test_platform_004_customer_token_on_platform(ctx):
    st(api("GET", "/api/v1/platform/tenants", t=ctx.T_CUST), 403, "REALM_MISMATCH")


@tc("TC-CORE-NOTIF-006", profile="P-MAIL-DOWN")
def test_notif_006_failing_provider_retried(ctx):
    r = api("POST", "/api/v1/notifications/dispatch", t=ctx.T_A, body=dispatch_body(ctx.ALICE_ID, ["EMAIL"]))
    st(r, 200)
    ids = (r.data or {}).get("logIds") or []
    if not ids:
        raise Blocked("no log id")
    lid = ids[0]
    # The first attempt is counted (prepare() commits attempts+1) BEFORE the SMTP connect; lastError and
    # nextAttemptAt are written only when its outcome is recorded. Wait for the recorded outcome, not the count.
    early = poll(lambda: get_log(ctx.T_A, lid),
                 lambda x: (x.data or {}).get("attempts", 0) >= 1 and bool((x.data or {}).get("lastError"))
                 and ((x.data or {}).get("nextAttemptAt") or (x.data or {}).get("notificationStatusId") == "FAILED"),
                 timeout=15, interval=0.1)
    d = early.data or {}
    eq(d.get("notificationStatusId"), "QUEUED", "early notificationStatusId")
    check((d.get("attempts") or 0) >= 1, "early attempts ≥ 1", ">=1", d.get("attempts"))
    check(bool(d.get("nextAttemptAt")), "early nextAttemptAt set", "set", d.get("nextAttemptAt"))
    check(bool(d.get("lastError")), "early lastError set", "set", d.get("lastError"))
    final = poll(lambda: get_log(ctx.T_A, lid), lambda x: (x.data or {}).get("notificationStatusId") == "FAILED", timeout=60, interval=1)
    d = final.data or {}
    eq(d.get("notificationStatusId"), "FAILED", "final notificationStatusId")
    eq(d.get("attempts"), 5, "final attempts")
    eq(d.get("retryCount"), 4, "final retryCount")
    check(bool(d.get("lastError")) and bool(d.get("errorMessage")), "lastError and errorMessage non-empty", "non-empty",
          (d.get("lastError"), d.get("errorMessage")))
    le = d.get("lastError") or ""
    check("MailConnectException" in le or "2525" in le, "lastError names the connection failure (MailConnectException / port 2525)",
          "MailConnectException … 2525", le)
    check("missing recipient email address" not in le, "the failure is the mail server, not a missing address",
          "no 'missing recipient email address'", le)
    s = api("POST", "/api/v1/notifications/logs/search", t=ctx.T_A,
            body={"filters": [{"field": "notificationStatusId", "operator": "EQUALS", "value": "FAILED"}], "size": 200})
    st(s, 200)
    check(lid in [x.get("id") for x in s.content], "search contains the log id", lid, [x.get("id") for x in s.content][:10])


@tc("TC-CORE-REPORT-013", profile="P-CAP")
def test_report_013_export_cap(ctx):
    cap = ctx.get("CAP")
    if cap == 2:
        st(api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=csv", t=ctx.T_PLAT, body={"params": {}}), 422, "REPORT_EXPORT_TOO_LARGE")
        r = api("POST", "/api/v1/report/APP_SMOKE_REPORT/run", t=ctx.T_PLAT, body={})
        st(r, 200)
        eq(len((r.data or {}).get("rows") or []), 3, "run returns 3 rows (cap applies to exports only)")
    elif cap == 3:
        st(api("POST", "/api/v1/report/APP_SMOKE_REPORT/export?format=csv", t=ctx.T_PLAT, body={"params": {}}), 200,
           what="export with the cap at 3")
    else:
        raise Blocked("--cap 2 or --cap 3 required for P-CAP")


@tc("TC-CORE-FILE-025", profile="P-LOCAL")
def test_file_025_local_provider(ctx):
    root = ctx.get("LOCAL_ROOT")
    _file_003(ctx, "LOCAL")
    if root:
        y, m = dt.date.today().strftime("%Y"), dt.date.today().strftime("%m")
        hits = glob.glob(os.path.join(root, str(ctx.A_ID), "*", y, m, f"{ctx.DOC1}_logo.png"))
        check(bool(hits), "file exists under <root>/<tenantId>/<category>/<yyyy>/<MM>/<id>_<name>",
              f"{root}/{ctx.A_ID}/<category>/{y}/{m}/{ctx.DOC1}_logo.png", hits or os.listdir(root))
    _file_004(ctx)
    _file_005(ctx)
    _file_008(ctx)
    _file_009(ctx)
    _file_017(ctx)
    _file_019(ctx)


# =============================================================================================
# execution order (plan §3)
# =============================================================================================
def ids(*names):
    return [f"TC-CORE-{n}" for n in names]


def rng(mod, a, b):
    return [f"{mod}-{i:03d}" for i in range(a, b + 1)]


ORDER = {
    "P-LIVE": ids(*rng("CORE", 1, 8), *rng("TENANT", 1, 4), "PLATFORM-001",
                  *rng("SEC", 1, 4), "SEQ-001", *rng("SETTINGS", 1, 3),
                  *rng("TENANT", 5, 12), "SEQ-002",
                  *rng("TENANT", 13, 18), "SEC-005", "SEC-006", "PLATFORM-002", "PLATFORM-003", "APP-002",
                  *rng("SEQ", 3, 14),
                  *rng("SETTINGS", 4, 10),
                  *rng("SEC", 7, 13), "SEC-028", *rng("SEC", 14, 20), "SEC-029", "SEC-030", "SEC-031", "SEC-033",
                  "NOTIF-001",
                  *rng("FILE", 1, 18), "FILE-024", *rng("FILE", 19, 21), "FILE-023",
                  "NOTIF-002", "NOTIF-004", "NOTIF-005", *rng("NOTIF", 7, 11), "NOTIF-013", "NOTIF-016",
                  *rng("AUDIT", 1, 5), "AUDIT-007", "AUDIT-008", *rng("AUDIT", 10, 14), "AUDIT-016",
                  *rng("REPORT", 1, 11), "AUDIT-015", *rng("REPORT", 14, 17), "APP-001",
                  "TENANT-025", *rng("TENANT", 19, 24)),
    "P-MAIL": ids(*rng("SEC", 21, 27), "NOTIF-003", "NOTIF-012", "NOTIF-014", "NOTIF-015", "SEC-034", "SEC-032",
                  "FILE-022", "AUDIT-006", "AUDIT-009", "REPORT-012", "PLATFORM-004", "TENANT-026"),
    "P-MAIL-DOWN": ids("NOTIF-006"),
    "P-CAP": ids("REPORT-013"),
    "P-LOCAL": ids("FILE-025"),
}


def profile_setup(ctx, profile):
    """Plan §3 phase 12: re-run phase 1 (logins) plus the phase-2 tenant A setup with a fresh RUN first."""
    steps = []

    def step(name, fn):
        r = fn()
        steps.append({"step": name, "status": r.status, "code": r.code})
        if r.status not in (200, 201):
            raise Blocked(f"profile setup step `{name}` failed: {r.status} {r.excerpt(200)}")
        return r
    r = step("SEC-001 login PLATFORM admin", lambda: login_staff("PLATFORM", "admin", ctx.ADMIN_PW))
    ctx.T_PLAT = r.data["accessToken"]
    if profile == "P-CAP":
        return steps
    r = step("TENANT-005 provision A", lambda: api("POST", "/api/v1/platform/tenants", t=ctx.T_PLAT,
                                                   body=tenant_body(ctx, ctx.TA, "ta-admin", "Tenant A")))
    ctx.A_ID = r.data["id"]
    r = step("TENANT-006 login ta-admin", lambda: login_staff(ctx.TA, "ta-admin", PW))
    ctx.T_A = r.data["accessToken"]
    if profile == "P-LOCAL":
        r = step("FILE-001 category allowPublic", lambda: api("POST", "/api/v1/files/categories", t=ctx.T_A, body={
            "categoryCode": f"TC_PUB_{ctx.RUN}", "nameAr": "فئة", "nameEn": f"Public {ctx.RUN}", "allowPublic": True}))
        ctx.CAT_PUB = r.data["id"]
        r = step("FILE-002 category private", lambda: api("POST", "/api/v1/files/categories", t=ctx.T_A, body={
            "categoryCode": f"TC_PRV_{ctx.RUN}", "nameAr": "فئة", "nameEn": f"Private {ctx.RUN}"}))
        ctx.CAT_PRV = r.data["id"]
        return steps
    r = step("TENANT-013 alice", lambda: api("POST", "/api/v1/sec/users", t=ctx.T_A,
                                             body=user_body(f"alice-{ctx.run}", f"alice-{ctx.run}@t.test", "أليس", "Alice")))
    ctx.ALICE_ID = r.data["userPk"]
    return steps


# =============================================================================================
# runner
# =============================================================================================
def run(args):
    BASE["url"] = args.base.rstrip("/")
    now = dt.datetime.now()
    run_id = args.run or now.strftime("%y%m%d%H%M") + uuid.uuid4().hex[:2].upper()
    ctx = Ctx(RUN=run_id, run=run_id.lower(), ADMIN_PW=args.admin_password or os.environ.get("ERP_BOOTSTRAP_ADMIN_PASSWORD"))
    if not ctx.get("ADMIN_PW"):
        sys.exit("admin password required (--admin-password or env ERP_BOOTSTRAP_ADMIN_PASSWORD)")
    ctx.TA, ctx.TB, ctx.TC = f"TCA{run_id}", f"TCB{run_id}", f"TCC{run_id}"
    ctx.CAP = args.cap
    ctx.LOCAL_ROOT = args.local_root
    by_id = {}
    for f, tids, prof in TESTS:
        for t in tids:
            by_id[t] = (f, prof)
    order = ORDER[args.profile]
    if args.only:
        order = [t for t in order if t in set(args.only.split(","))]
    meta = {"profile": args.profile, "base": BASE["url"], "run": run_id, "started": now.isoformat(timespec="seconds"),
            "cap": args.cap, "local_root": args.local_root}
    sink = None
    if args.profile == "P-MAIL":
        from smtp_sink import SmtpSink
        mail_dir = args.mail_dir or tempfile.mkdtemp(prefix=f"erp-verify-mail-{run_id}-")  # captured mail holds tokens: never under the repo
        sink = SmtpSink(args.smtp_port, mail_dir).start()
        SINK["sink"] = sink
        meta["mail_dir"] = mail_dir
    setup_err = None
    if args.profile != "P-LIVE":
        try:
            meta["setup"] = profile_setup(ctx, args.profile)
        except Blocked as e:
            setup_err = str(e)
    print(f"RUN={run_id} profile={args.profile} base={BASE['url']}")
    for tid in order:
        f, prof = by_id[tid]
        RESULTS[tid] = {"tc": tid, "test": f.__name__, "profile": args.profile, "checks": [], "result": None, "note": ""}
        CUR["tc"] = tid
        t0 = time.time()
        try:
            if setup_err:
                raise Blocked(setup_err)
            f(ctx)
            rec = RESULTS[tid]
            rec["result"] = "PASS" if rec["checks"] and all(c["ok"] for c in rec["checks"]) else "FAIL"
        except Blocked as e:
            rec = RESULTS[tid]
            failed_before = any(not c["ok"] for c in rec["checks"])
            rec["result"] = "FAIL" if failed_before else "BLOCKED"
            rec["note"] = str(e)
        except Exception as e:  # noqa: BLE001
            rec = RESULTS[tid]
            rec["result"] = "FAIL"
            rec["note"] = f"exception: {e!r}"
            rec["checks"].append({"ok": False, "what": "test raised", "expected": "", "observed": traceback.format_exc()[-600:], "request": ""})
        rec["seconds"] = round(time.time() - t0, 2)
        print(f"  {rec['result']:<8} {tid:<22} {f.__name__}" + (f"  [{rec['note']}]" if rec["note"] else ""))
    if sink:
        sink.stop()
    meta["finished"] = dt.datetime.now().isoformat(timespec="seconds")
    meta["plan_notes"] = ctx.get("plan_notes", [])
    out = args.out or os.path.join(HERE, "results", f"{now.strftime('%Y%m%dT%H%M%S')}-{args.profile}.json")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as fh:
        json.dump({"meta": meta, "results": RESULTS}, fh, ensure_ascii=False, indent=1)
    tot = {}
    for r in RESULTS.values():
        tot[r["result"]] = tot.get(r["result"], 0) + 1
    print(f"results → {out}  {tot}")
    return 1 if any(r["result"] != "PASS" for r in RESULTS.values()) else 0


def main():
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", default="http://localhost:7272")
    ap.add_argument("--profile", default="P-LIVE", choices=list(ORDER))
    ap.add_argument("--admin-password", default=None)
    ap.add_argument("--run", default=None, help="RUN suffix (default: timestamp + random)")
    ap.add_argument("--only", default=None, help="comma-separated TC ids (debug)")
    ap.add_argument("--smtp-port", type=int, default=1025)
    ap.add_argument("--mail-dir", default=None)
    ap.add_argument("--cap", type=int, default=None, help="P-CAP: the max-export-rows the instance runs with (2 or 3)")
    ap.add_argument("--local-root", default=None, help="P-LOCAL: erp.core.files.local.root of the instance")
    ap.add_argument("--out", default=None)
    ap.add_argument("--report", nargs="+", default=None, help="merge result JSON files into core-verify-report.md")
    args = ap.parse_args()
    if args.report:
        import core_verify_report
        return core_verify_report.build(args.report)
    return run(args)


if __name__ == "__main__":
    sys.exit(main())
