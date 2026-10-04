#!/usr/bin/env python3
"""
check_completeness.py — asserts that every operation of the running app's
aggregate OpenAPI document (<base>/v3/api-docs) is documented in EXACTLY ONE
module folder under docs/api-docs/, and that no folder documents an operation
the app does not serve.

An operation counts as documented in a folder when one of its
endpoints/*.md files carries the heading "## <METHOD> <path>".

Usage:  python docs/api-docs/_tools/check_completeness.py [--base http://localhost:7272]
Exit 0 = complete; 1 = something missing, duplicated or stale.
"""

import argparse
import json
import re
import sys
import urllib.request
from collections import defaultdict
from pathlib import Path

DOCS = Path(__file__).resolve().parents[1]
HTTP_METHODS = ("get", "post", "put", "delete", "patch", "head", "options", "trace")
HEADING = re.compile(r"^## (GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS|TRACE) (/\S*)\s*$")


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")  # docs carry Arabic and em dashes; a cp1256 console would crash
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:7272")
    args = ap.parse_args()

    with urllib.request.urlopen(f"{args.base}/v3/api-docs", timeout=60) as r:
        spec = json.loads(r.read().decode("utf-8"))
    served = sorted((m.upper(), p) for p, item in spec["paths"].items() for m in item if m in HTTP_METHODS)

    found: dict[tuple[str, str], list[str]] = defaultdict(list)
    per_module: dict[str, int] = {}
    for module_dir in sorted(d for d in DOCS.iterdir() if d.is_dir() and not d.name.startswith("_")):
        count = 0
        for md in sorted((module_dir / "endpoints").glob("*.md")):
            for line in md.read_text(encoding="utf-8").splitlines():
                m = HEADING.match(line)
                if m:
                    found[(m.group(1), m.group(2))].append(module_dir.name)
                    count += 1
        per_module[module_dir.name] = count

    missing = [op for op in served if op not in found]
    duplicated = [(op, mods) for op, mods in found.items() if len(mods) > 1]
    stale = [op for op in found if op not in set(served)]

    print(f"source: {args.base}/v3/api-docs -> {len(spec['paths'])} paths, {len(served)} operations")
    for op in served:
        print(f"  {op[0]:6} {op[1]:60} -> {', '.join(found.get(op, ['MISSING']))}")
    print("per module: " + ", ".join(f"{k}={v}" for k, v in per_module.items())
          + f"  (sum {sum(per_module.values())})")
    print(f"missing={len(missing)} duplicated={len(duplicated)} stale={len(stale)}")
    for op in missing:
        print(f"  MISSING    {op[0]} {op[1]}")
    for op, mods in duplicated:
        print(f"  DUPLICATED {op[0]} {op[1]} in {mods}")
    for op in stale:
        print(f"  STALE      {op[0]} {op[1]} (documented, not served)")
    ok = not (missing or duplicated or stale) and sum(per_module.values()) == len(served)
    print("RESULT: PASS" if ok else "RESULT: FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
