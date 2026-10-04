"""Builds docs/test-api/core-verify-report.md from core_api_verify.py result files (mechanical).

    python docs/test-api/core_api_verify.py --report docs/test-api/results/*.json

Inputs: the result JSON files, the plan (docs/test-api/core-test-plan.md, for the TC list and §9),
the classification file core-verify-classification.json (the human judgement for each FAIL, with
its source) and the test sources (to verify each §9 JUnit name by grep).
"""
from __future__ import annotations

import glob
import json
import os
import re
import subprocess
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
PLAN = os.path.join(HERE, "core-test-plan.md")
CLASSIFICATION = os.path.join(HERE, "core-verify-classification.json")
OUT = os.path.join(HERE, "core-verify-report.md")
MODULES = ["CORE", "TENANT", "PLATFORM", "SEC", "SEQ", "SETTINGS", "FILE", "NOTIF", "AUDIT", "REPORT", "APP"]


def _git(*args):
    try:
        return subprocess.check_output(["git", "-C", ROOT, *args], text=True).strip()
    except Exception:
        return "?"


def plan_ids():
    text = open(PLAN, encoding="utf-8").read()
    return re.findall(r"^\| (TC-CORE-[A-Z]+-\d{3}) \|", text, re.M)


def plan_section9():
    text = open(PLAN, encoding="utf-8").read()
    sec = text[text.index("## 9."):]
    rows = []
    for line in sec.splitlines():
        if not line.startswith("| ") or line.startswith("| Step") or line.startswith("|---"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 4:
            rows.append({"step": cells[0], "behaviour": cells[1], "covered": cells[3]})
    return rows


_TEST_FILES = None


def test_files():
    global _TEST_FILES
    if _TEST_FILES is None:
        _TEST_FILES = {}
        for base in ("erp-core/src/test", "erp-app-reference/src/test"):
            for p in glob.glob(os.path.join(ROOT, base, "**", "*.java"), recursive=True):
                _TEST_FILES.setdefault(os.path.basename(p)[:-5], []).append(p)
    return _TEST_FILES


def verify_junit(cell):
    """Return [(name, found, where)] for each backticked JUnit reference in a §9 cell."""
    out, cls = [], None
    for tok in re.findall(r"`([^`]+)`", cell):
        if " " in tok or re.fullmatch(r"[A-Z0-9_]+", tok):
            continue                      # `jar tf` (a manual check) or a report code such as TST_CSV_SAMPLE
        prefix = tok.endswith("…") or tok.endswith("_…")
        tok_clean = tok.rstrip("…").rstrip("_") if prefix else tok
        if "." in tok_clean:
            cls, meth = tok_clean.split(".", 1)
        elif tok_clean[:1].isupper():
            cls, meth = tok_clean, None
        else:
            meth = tok_clean
        files = test_files().get(cls or "", [])
        if not files:
            out.append((tok, False, f"no test class {cls}"))
            continue
        if meth is None:
            out.append((tok, True, os.path.relpath(files[0], ROOT).replace("\\", "/")))
            continue
        # a JUnit method `void name(` or an ArchUnit `@ArchTest static final ArchRule name =`
        name = re.escape(meth) + (r"\w*" if prefix else "")
        pat = re.compile(r"(void\s+" + name + r"\s*\(|ArchRule\s+" + name + r"\s*=)")
        hit = next((f for f in files if pat.search(open(f, encoding="utf-8", errors="replace").read())), None)
        out.append((tok, bool(hit), os.path.relpath(hit, ROOT).replace("\\", "/") if hit else f"no method in {cls}"))
    return out


def merge(files):
    merged, metas = OrderedDict(), []
    for f in files:
        d = json.load(open(f, encoding="utf-8"))
        meta = d["meta"]
        meta["file"] = os.path.relpath(f, ROOT).replace("\\", "/")
        metas.append(meta)
        for tid, r in d["results"].items():
            part = dict(r, run=meta["run"], profile=meta["profile"], cap=meta.get("cap"), file=meta["file"])
            merged.setdefault(tid, []).append(part)
    final = {}
    for tid, parts in merged.items():
        res = [p["result"] for p in parts]
        if all(x == "PASS" for x in res):
            verdict = "PASS"
        elif "FAIL" in res:
            verdict = "FAIL"
        else:
            verdict = "BLOCKED"
        final[tid] = {"result": verdict, "parts": parts, "test": parts[0]["test"], "profile": parts[0]["profile"]}
    return final, metas


def md_cell(s):
    return str(s).replace("|", "\\|").replace("\n", " ")


def build(files):
    files = sorted({os.path.abspath(f) for pat in files for f in glob.glob(pat)})
    final, metas = merge(files)
    import core_api_verify as v
    impl = {t: f.__name__ for f, ids, _ in v.TESTS for t in ids}
    prof = {t: p for f, ids, p in v.TESTS for t in ids}
    cls = json.load(open(CLASSIFICATION, encoding="utf-8")) if os.path.exists(CLASSIFICATION) else {}
    ids = plan_ids()
    rows, tot = [], OrderedDict((k, 0) for k in ["PASS", "FAIL", "SKIPPED", "BLOCKED", "IN-PROCESS", "NO-TEST"])
    per_mod = OrderedDict((m, OrderedDict((k, 0) for k in tot)) for m in MODULES)
    for tid in ids:
        mod = tid.split("-")[2]
        if tid not in impl:
            res, test = "NO-TEST", "✗ no matching test"
        elif tid in final:
            res, test = final[tid]["result"], f"`{impl[tid]}()`"
        else:
            res, test = "SKIPPED", f"`{impl[tid]}()`"
        tot[res] += 1
        per_mod[mod][res] += 1
        shown = res
        if res == "SKIPPED":
            shown = f"SKIPPED ({cls.get('skipped', {}).get(tid, 'its profile was not run')})"
        if res == "FAIL" and tid in cls.get("fail", {}):
            shown = f"FAIL — {cls['fail'][tid]['class']}"
        runs = ", ".join(sorted({p["run"] for p in final.get(tid, {}).get("parts", [])}))
        rows.append(f"| {tid} | {test} | {prof.get(tid, '?')} | {shown} | {runs} |")

    L = []
    w = L.append
    w("# erp-core 1.0.0 — API verification report (`TC-CORE-*`)")
    w("")
    w("Generated by `python docs/test-api/core_api_verify.py --report docs/test-api/results/*.json` "
      "(`core_verify_report.py`). Do not edit by hand: re-run the verification and regenerate. The classification of each "
      "failure lives in `core-verify-classification.json`.")
    w("")
    w("## 1. What was run")
    w("")
    w(f"- Input: `docs/test-api/core-test-plan.md` (166 cases). Contract reference: `docs/api-docs/` (105 operations).")
    w(f"- Code under test: branch `{_git('rev-parse', '--abbrev-ref', 'HEAD')}` at `{_git('rev-parse', '--short', 'HEAD')}` "
      "(erp-core 1.0.0 + erp-app-reference 1.1.0-SNAPSHOT, the same code as the `v1.0.0` tag per the plan §2).")
    w("- Method: skill `api-verify` (translation, not derivation) with the phase-D overrides: one script, "
      "`docs/test-api/core_api_verify.py`, one `test_<name>()` per TC id declared with `@tc(\"TC-CORE-…\")`, run in the "
      "plan's §3 order, with a fresh `RUN` suffix per run. Stdlib-only Python; `smtp_sink.py` is a stdlib SMTP capture sink.")
    w("- Contract facts (in place of `api-verify-config.md`, which does not apply): success envelope "
      "`ApiResponse{success,data,timestamp}`; failure envelope `ApiResponse{success:false,error:ApiError{code,message,fieldErrors}}` "
      "(`com.erp.common.web.ApiResponse`/`ApiError`, `GlobalExceptionHandler`, `SecSecurityErrorHandler`, "
      "`RealmEnforcementFilter`); paging = Spring `Page` under `data`; base path `/api/v1`; languages en (default) / ar via "
      "`Accept-Language`.")
    w("")
    w("### 1.1 Runs")
    w("")
    w("| Profile | Base URL | RUN | Started | Instance | Result file |")
    w("|---|---|---|---|---|---|")
    inst = {
        "P-LIVE": "the shared long-lived instance (profile `dev`, simple cache, no SMTP, DB storage) — not restarted",
        "P-MAIL": "own instance, variant jar (+ `spring-boot-starter-mail`), `--spring.mail.host=localhost --spring.mail.port=1025`, sink in-process",
        "P-MAIL-DOWN": "own instance, variant jar, `--spring.mail.host=localhost --spring.mail.port=2525` (closed), default retry backoff",
        "P-CAP": "own instance, plain jar, `--erp.core.report.max-export-rows=<cap>`",
        "P-LOCAL": "own instance, plain jar, `--erp.core.files.storage=LOCAL --erp.core.files.local.root=<temp dir>`",
    }
    for m in metas:
        extra = f" (cap {m['cap']})" if m.get("cap") else ""
        w(f"| {m['profile']}{extra} | {m['base']} | `{m['run']}` | {m['started']} | {inst.get(m['profile'], '')} | `{m['file']}` |")
    w("")
    w("### 1.2 How to run")
    w("")
    w("```bash")
    w("export ERP_BOOTSTRAP_ADMIN_PASSWORD='<bootstrap admin password>'   # never written to any output")
    w("# P-LIVE (147 cases) against the running app")
    w("python docs/test-api/core_api_verify.py --base http://localhost:7272")
    w("# profile instances (own port + scratch DB); JDK 21: export JAVA_HOME=...; createdb erp_pd_verify")
    w("#  plain jar   : mvn -o -q -DskipTests package   -> erp-app-reference/target/erp-app-reference-1.1.0-SNAPSHOT.jar")
    w("#  variant jar : temporarily add org.springframework.boot:spring-boot-starter-mail to erp-app-reference/pom.xml,")
    w("#                mvn -o -q -DskipTests package, copy the jar aside, `git checkout erp-app-reference/pom.xml` (never committed)")
    w("docs/test-api/start_profile_instance.sh app-mail.jar 7295 erp_pd_verify pmail.log --spring.mail.host=localhost --spring.mail.port=1025")
    w("python docs/test-api/core_api_verify.py --profile P-MAIL --base http://localhost:7295 --smtp-port 1025")
    w("docs/test-api/start_profile_instance.sh app-mail.jar 7295 erp_pd_verify down.log --spring.mail.host=localhost --spring.mail.port=2525")
    w("python docs/test-api/core_api_verify.py --profile P-MAIL-DOWN --base http://localhost:7295")
    w("docs/test-api/start_profile_instance.sh app-plain.jar 7295 erp_pd_verify cap2.log --erp.core.report.max-export-rows=2")
    w("python docs/test-api/core_api_verify.py --profile P-CAP --cap 2 --base http://localhost:7295")
    w("docs/test-api/start_profile_instance.sh app-plain.jar 7295 erp_pd_verify cap3.log --erp.core.report.max-export-rows=3")
    w("python docs/test-api/core_api_verify.py --profile P-CAP --cap 3 --base http://localhost:7295")
    w("docs/test-api/start_profile_instance.sh app-plain.jar 7295 erp_pd_verify local.log --erp.core.files.storage=LOCAL --erp.core.files.local.root=<dir>")
    w("python docs/test-api/core_api_verify.py --profile P-LOCAL --local-root <dir> --base http://localhost:7295")
    w("python docs/test-api/core_api_verify.py --report docs/test-api/results/*.json   # this report")
    w("```")
    w("")
    w("Each profile instance was stopped before the next started (one DB, `erp_pd_verify`, persisting across "
      "restarts as plan §2.1 says, with a fresh `RUN` per profile). The instances were stopped and the scratch DB "
      "dropped at the end. Captured mail is written outside the repository (it holds tokens).")
    w("")
    w("## 2. Summary")
    w("")
    w("| Module | Cases | PASS | FAIL | SKIPPED | BLOCKED | IN-PROCESS | no test |")
    w("|---|---:|---:|---:|---:|---:|---:|---:|")
    for m, c in per_mod.items():
        w(f"| {m} | {sum(c.values())} | {c['PASS']} | {c['FAIL']} | {c['SKIPPED']} | {c['BLOCKED']} | {c['IN-PROCESS']} | {c['NO-TEST']} |")
    w(f"| **Total** | **{sum(tot.values())}** | **{tot['PASS']}** | **{tot['FAIL']}** | **{tot['SKIPPED']}** | "
      f"**{tot['BLOCKED']}** | **{tot['IN-PROCESS']}** | **{tot['NO-TEST']}** |")
    w("")
    w("IN-PROCESS is 0 by construction: every TC-CORE id is an HTTP case; the plan's in-process behaviours carry no "
      "TC id and are listed in §7 with their JUnit tests, each verified to exist.")
    fails = [t for t in ids if t in final and final[t]["result"] == "FAIL"]
    by_class = OrderedDict()
    for t in fails:
        c = cls.get("fail", {}).get(t, {}).get("class", "UNCLASSIFIED")
        by_class.setdefault(c, []).append(t.replace("TC-CORE-", ""))
    if by_class:
        w("")
        w("Failures by classification: " + "; ".join(f"**{k}** {len(v)} ({', '.join(v)})" for k, v in by_class.items()) + ".")
    w("")
    w("## 3. Coverage table (TC-CORE-* → test function)")
    w("")
    w("| TC id | test function | profile | result | RUN |")
    w("|---|---|---|---|---|")
    L.extend(rows)
    w("")
    w("## 4. Problems")
    w("")
    if not fails:
        w("None.")
    for t in fails:
        c = cls.get("fail", {}).get(t, {})
        w(f"### {t} — {c.get('class', 'UNCLASSIFIED')}")
        w("")
        if c.get("summary"):
            w(c["summary"])
            w("")
        for p in final[t]["parts"]:
            for ch in p["checks"]:
                if ch["ok"]:
                    continue
                w(f"- **{md_cell(ch['what'])}**")
                if ch.get("request") and ch["request"] != ch["what"]:
                    w(f"  - request: `{md_cell(ch['request'])[:400]}`")
                w(f"  - expected: `{md_cell(ch['expected'])[:300]}`")
                w(f"  - observed: `{md_cell(ch['observed'])[:500]}`")
            if p.get("note"):
                w(f"- note: {p['note']}")
        if c.get("rationale"):
            w("")
            w(f"Classification: {c['rationale']}")
        w("")
    obs = [(t, o) for t in ids if t in final for p in final[t]["parts"] for o in p.get("observations", [])]
    w("## 5. Observations (recorded, never part of a verdict)")
    w("")
    if not obs:
        w("None.")
    for t, o in obs:
        w(f"- **{t}** — {md_cell(o['what'])}: `{md_cell(o['observed'])[:400]}`")
        if o.get("request"):
            w(f"  - request: `{md_cell(o['request'])[:400]}`")
    w("")
    w("## 6. Plan notes and gaps")
    w("")
    notes = cls.get("plan_notes", [])
    for m in metas:
        notes += [n for n in m.get("plan_notes", []) if n not in notes]
    for n in notes:
        w(f"- {n}")
    gaps = [t for t in ids if t not in impl]
    w("")
    w("**Gaps (TC ids without a matching test):** " + (", ".join(gaps) if gaps else "none — all 166 ids map to a `test_<name>()`."))
    skipped = [t for t in ids if t in impl and t not in final]
    w("")
    w("**Not executed:** " + (", ".join(skipped) if skipped else "none — every case ran in its profile."))
    w("")
    w("## 7. In-process behaviours (plan §9) — covered by JUnit, not by this script")
    w("")
    w("Each name was verified mechanically: the class file exists under `erp-core/src/test` or "
      "`erp-app-reference/src/test` and declares a `void <method>(` or an ArchUnit `ArchRule <name> =` field (a trailing `…` is matched as a prefix); bare upper-case codes (e.g. `TST_CSV_SAMPLE`, a test-only report code) are not test names and are skipped.")
    w("")
    w("| Step | Behaviour | JUnit (verified) |")
    w("|---|---|---|")
    missing_total = 0
    for r9 in plan_section9():
        ver = verify_junit(r9["covered"])
        if not ver:
            cov = f"not a JUnit test: {md_cell(r9['covered'])}"
        else:
            parts = []
            for name, ok, where in ver:
                parts.append(f"✓ `{name}`" if ok else f"✗ `{name}` ({where})")
                missing_total += 0 if ok else 1
            cov = "; ".join(parts)
        w(f"| {r9['step']} | {md_cell(r9['behaviour'])} | {cov} |")
    w("")
    w(f"JUnit names not found: **{missing_total}**.")
    w("")
    w("## 8. Surviving records and privileges")
    w("")
    w("No case documents a hard delete for what it creates, so the run data stays (the plan's §7 data hygiene: every "
      "code, username and e-mail carries the run suffix). On the shared P-LIVE instance, per RUN:")
    w("")
    for m in metas:
        if m["profile"] != "P-LIVE":
            continue
        r = m["run"]
        w(f"- RUN `{r}`: tenants `TCA{r}`, `TCB{r}`, `TCC{r}` (C re-activated by TENANT-024) with their admins "
          f"`ta-admin`/`tb-admin`/`tc-admin`; staff users `alice-{r.lower()}` (A, B), `bob-{r.lower()}`, `c1-{r.lower()}@shop.test` "
          f"(A staff), `f-{r.lower()}`, `g-{r.lower()}`, `rpt-{r.lower()}` (A), `p-noperm-{r.lower()}` (PLATFORM); customers "
          f"`c1/c4/c5-{r.lower()}@shop.test` (A; c4 left ACTIVE and c5 holding SYS_ADMIN by the SEC-029/030 defects) and "
          f"`c1-{r.lower()}@shop.test` (B); PLATFORM number series `TC_INV_{r}` and `TC_NEV_{r}`; PLATFORM configuration "
          f"`TC_DEF_{r}` (platform default, deactivated) and its PLATFORM-tenant override; A role `TC_RPT_{r}`; A file "
          f"categories `TC_PUB_{r}`/`TC_PRV_{r}` and their documents; an A `SMS` channel configuration; notification "
          f"logs and inbox rows; audit rows.")
    w("- **Permanent residue in reference tables read by other modules:** none. The run writes no registry rows "
      "(the permission catalog is only read) and no lookup values.")
    w("- **Privileges:** the script granted nothing to any pre-existing role or user. REPORT-011 creates its own role "
      "`TC_RPT_{RUN}` inside the run's own tenant A and grants it `PERM_SEC_REPORTS_VIEW` + `SEC:REPORT:SEC_USER_LIST` "
      "exactly as the case prescribes; that role only ever reaches the run's own user `rpt-{run}`.")
    w("- Profile runs used the scratch database `erp_pd_verify`, dropped at the end.")
    w("")
    open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(L) + "\n")
    print(f"wrote {OUT}: {dict(tot)}")
    return 0
