#!/usr/bin/env python3
"""
generate_all.py — regenerates docs/api-docs/<module>/ for every erp-core module
(+ the reference app) from the RUNNING erp-app-reference, using the existing
api-doc-generator pipeline (erp-app-reference/governance/governance-tools/
api-doc-generator) with its repository discovery bypassed.

Why a wrapper instead of `generate.py --module X`:
  * discovery.py finds the springdoc group per module -- but the springdoc
    groups do not map 1:1 to modules (a `customers` group, no group for tenant/sequence/audit/report/app).
  * generate.py's override path (--openapi + --source) drops the message
    bundles and Flyway roots, so the docs would lose the i18n message columns
    and the unique-constraint section.

What it does instead (nothing invented, everything from the live app + source):
  1. GET <base>/v3/api-docs (the aggregate document, every operation).
  2. Assign each (method, path) to the ONE module whose controller source
     declares that verb+route, using the generator's own matcher
     (security_extractor.find_controller_for_endpoint). Zero or several
     matches abort the run.
  3. Write a per-module copy of the aggregate document whose `paths` keep only
     that module's operations (components untouched) to a temp dir.
  4. Build a discovery.RepositoryContext by hand (OpenAPI = that copy, module
     source root, erp-core/src/main/java as the shared root, the core i18n
     bundles, both Flyway roots, no execution plan -> no contract ids) and call
     generator.run(context, mode) -- the same pipeline generate.py runs.
  5. For generate/update, mirror each erp-core module folder into this repo's
     governance tree, governance/backend/modules/<MOD>/api-docs/ (the copy the
     frontend reads); `app` is the reference app, not a governed module.

Usage (from anywhere; Python 3.10+):
    python docs/api-docs/_tools/generate_all.py [--base http://localhost:7272] [--function generate|update|review|check]
"""

import argparse
import json
import re
import shutil
import sys
import tempfile
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
GEN = REPO / "erp-app-reference" / "governance" / "governance-tools" / "api-doc-generator"
sys.path.insert(0, str(GEN))

import generator  # noqa: E402
from discovery import RepositoryContext  # noqa: E402
from extractors import security_extractor  # noqa: E402

CORE_JAVA = REPO / "erp-core" / "src" / "main" / "java"
APP_JAVA = REPO / "erp-app-reference" / "src" / "main" / "java"
OUT = REPO / "docs" / "api-docs"
GOVERNANCE_MODULES = REPO / "governance" / "backend" / "modules"
NOT_GOVERNED = {"app"}
FALLBACK: list[str] = []
HTTP_METHODS = ("get", "post", "put", "delete", "patch", "head", "options", "trace")

MODULES = {
    "sec": CORE_JAVA / "com" / "erp" / "sec",
    "tenant": CORE_JAVA / "com" / "erp" / "tenant",
    "file": CORE_JAVA / "com" / "erp" / "file",
    "notif": CORE_JAVA / "com" / "erp" / "notif",
    "mdl": CORE_JAVA / "com" / "erp" / "mdl",
    "cu": CORE_JAVA / "com" / "erp" / "cu",
    "sequence": CORE_JAVA / "com" / "erp" / "sequence",
    "audit": CORE_JAVA / "com" / "erp" / "audit",
    "report": CORE_JAVA / "com" / "erp" / "report",
    "app": APP_JAVA / "com" / "erp" / "app",
}


def operations(spec: dict) -> list[tuple[str, str]]:
    return sorted((m.upper(), p) for p, item in spec.get("paths", {}).items() for m in item if m in HTTP_METHODS)


TAG_RE = re.compile(r'@Tag\(\s*name\s*=\s*"([^"]+)"')


def tag_owners() -> dict[str, list[str]]:
    """@Tag(name = ...) of every controller -> the module(s) declaring it."""
    owners: dict[str, list[str]] = {}
    for mod, root in MODULES.items():
        for f in sorted(root.rglob("*Controller.java")):
            for name in TAG_RE.findall(f.read_text(encoding="utf-8")):
                owners.setdefault(name, []).append(mod)
    return owners


def assign(spec: dict) -> dict[tuple[str, str], str]:
    """Primary: the generator's own verb+route matcher. Fallback, only when it
    finds nothing (it reads literal @XxxMapping("...") strings, so a route built
    from constants or an array of paths is invisible to it): the operation's
    first tag, when exactly one module's controller declares that @Tag."""
    owner: dict[tuple[str, str], str] = {}
    problems = []
    tags = tag_owners()
    for method, path in operations(spec):
        hits = [mod for mod, root in MODULES.items()
                if security_extractor.find_controller_for_endpoint(root, method, path)[0] is not None]
        if not hits:
            op_tags = spec["paths"][path][method.lower()].get("tags") or []
            hits = tags.get(op_tags[0], []) if op_tags else []
            if len(hits) == 1:
                FALLBACK.append(f"{method} {path} -> {hits[0]} (by @Tag \"{op_tags[0]}\")")
        if len(hits) != 1:
            problems.append(f"{method} {path}: matched {hits or 'no module'}")
        else:
            owner[(method, path)] = hits[0]
    if problems:
        raise SystemExit("cannot assign every operation to exactly one module:\n  " + "\n  ".join(problems))
    return owner


def filtered(spec: dict, owner: dict, module: str) -> dict:
    out = dict(spec)
    paths = {}
    for path, item in spec["paths"].items():
        kept = {k: v for k, v in item.items()
                if k not in HTTP_METHODS or owner.get((k.upper(), path)) == module}
        if any(k in HTTP_METHODS for k in kept):
            paths[path] = kept
    out["paths"] = paths
    return out


def publish_to_governance() -> None:
    """Replace governance/backend/modules/<MOD>/api-docs/ with docs/api-docs/<mod>/."""
    for module in MODULES:
        if module in NOT_GOVERNED:
            continue
        target = GOVERNANCE_MODULES / module.upper() / "api-docs"
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(OUT / module, target)
        print(f"mirrored docs/api-docs/{module}/ -> {target.relative_to(REPO).as_posix()}/")


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")  # docs carry Arabic and em dashes; a cp1256 console would crash
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", default="http://localhost:7272")
    ap.add_argument("--function", default="generate", choices=["generate", "update", "review", "check"])
    ap.add_argument("--server-url", help="Publish this URL as the document's server instead of the one springdoc "
                    "derived from the request (use http://localhost:7272, the reference app's default port, "
                    "when generating from an instance on another port)")
    ap.add_argument("--no-governance", action="store_true",
                    help="Do not mirror the generated folders into governance/backend/modules/<MOD>/api-docs/")
    args = ap.parse_args()

    with urllib.request.urlopen(f"{args.base}/v3/api-docs", timeout=60) as r:
        spec = json.loads(r.read().decode("utf-8"))
    owner = assign(spec)
    if args.server_url:
        spec["servers"] = [{"url": args.server_url, "description": "Generated server url"}]

    bundles = sorted((REPO / "erp-core" / "src" / "main" / "resources" / "i18n").glob("messages*.properties"))
    migrations = [d for d in (REPO / "erp-core" / "src" / "main" / "resources" / "db" / "migration",
                              REPO / "erp-app-reference" / "src" / "main" / "resources" / "db" / "migration")
                  if d.is_dir()]
    worst = 0
    with tempfile.TemporaryDirectory() as tmp:
        for module, root in MODULES.items():
            part = filtered(spec, owner, module)
            src = Path(tmp) / f"{module}.json"
            src.write_text(json.dumps(part), encoding="utf-8")
            context = RepositoryContext(
                module=module.upper(),
                openapi_source=str(src),
                output=OUT / module,
                source_root=root,
                common_source_roots=[CORE_JAVA],
                execution_plan=None,
                migration_roots=migrations,
                message_bundles=bundles,
                message_basename="messages",
            )
            print(f"===== {module} ({len(operations(part))} operations) =====")
            if args.function == "check":
                report, code = generator.check(context)
                worst = max(worst, code)
            else:
                report = generator.run(context, mode=args.function)
            print(report)
    if args.function in ("generate", "update") and not args.no_governance:
        publish_to_governance()
    for line in FALLBACK:
        print(f"assigned by tag fallback: {line}")
    print(f"TOTAL operations in {args.base}/v3/api-docs: {len(owner)}")
    return worst


if __name__ == "__main__":
    raise SystemExit(main())
