"""
Reads the module's own API REGISTRY out of its backend execution plan and
joins it to the implemented endpoints, so every generated endpoint carries
the contract id (API-<MOD>-NNN) the rest of the governance chain — the SRS,
the frontend execution plan, the test manifest, the alignment report — is
written in terms of.

Why this exists
───────────────
Before this extractor the generated docs were correct but *anonymous*: they
named METHOD + path and nothing else. Every downstream artifact, and every
frontend developer, works from a contract id instead ("implement
API-SEC-004"), and the id appeared nowhere in the published api-docs. The
only place left to resolve an id into a path was a planning document — and a
planning document states the path that was *proposed*, not the one that was
implemented. That is how a frontend ends up calling
/api/v1/security/auth/forgot-password against a backend that serves
/api/v1/sec/auth/password-reset/complete: not a wrong path in the docs, but
a missing join between the id and the docs.

So the id is stamped onto the implemented endpoint here, and any id that is
planned but not implemented (or implemented but not planned) is reported as
contract drift rather than silently dropped — a stale contract id is exactly
the failure mode above, and it is only detectable by comparing the two sides.

Like every other extractor: it finds real metadata or leaves the field empty.
The registry table is read verbatim; no path is ever inferred from an id.
"""

import re
from pathlib import Path
from typing import Optional

from extractors import security_extractor
from models.api_doc_model import ApiDocument, ContractDrift, ContractEntry

# "**API REGISTRY**" heading, then a markdown table whose header row starts
# with an API column. Matched structurally (a row whose first cell is an
# API-<MOD>-NNN id) rather than by column index, so a plan that adds or
# reorders descriptive columns still parses as long as the row carries an id,
# an HTTP verb and a path.
_API_ID_RE = re.compile(r"^API-[A-Z0-9]+-\d+$")
_VERB_RE = re.compile(r"^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$", re.IGNORECASE)
_PATH_RE = re.compile(r"^/\S*$")
# Both registry spellings this repo's plans use: the markdown-table modules
# (SEC, MDL, FIN) head it "**API REGISTRY**", the older box-drawing ones (CU,
# NOTIF, FILE) write a bare "API REGISTRY" line above a │-delimited table.
_REGISTRY_HEADING_RE = re.compile(r"^\s*\**API\s+REGISTRY\b", re.MULTILINE)
_DELIMITERS = ("|", "\u2502")


def normalize_path(path: str) -> str:
    """Path parameter *names* are the plan author's choice on one side and
    the Java method's parameter name on the other; only their positions are
    part of the contract. `/roles/{id}/modules/{moduleId}` and
    `/roles/{roleId}/modules/{id}` are the same endpoint, and must not be
    reported as drift."""
    collapsed = re.sub(r"\{[^}]*\}", "{}", path.strip())
    return "/" + collapsed.strip("/")


def _key(method: str, path: str) -> tuple[str, str]:
    return method.strip().upper(), normalize_path(path)


def _cells(line: str) -> list[str]:
    """Splits one table row into cells for either delimiter. A markdown row is
    fenced by a leading/trailing pipe; a box-drawing row is not fenced at
    all, so it is split on its own delimiter as-is."""
    stripped = line.strip()
    if "\u2502" in stripped:
        return [c.strip().strip("*").strip() for c in stripped.split("\u2502")]
    if not stripped.startswith("|"):
        return []
    return [c.strip().strip("*").strip() for c in stripped.strip("|").split("|")]


def parse_api_registry(text: str) -> list[ContractEntry]:
    """Parses the API REGISTRY table. Only rows inside (or after) the API
    REGISTRY heading are considered, and only until a row stops looking like
    a registry row — so the plan's other id tables (the DOC-phase contract
    summary, whose paths are context-path-relative and therefore NOT the
    served paths) are never mistaken for it."""
    start = _REGISTRY_HEADING_RE.search(text)
    if not start:
        return []

    entries: list[ContractEntry] = []
    seen_row = False
    for line in text[start.end():].splitlines():
        cells = _cells(line)
        if not cells or not _API_ID_RE.match(cells[0]):
            stripped = line.strip()
            if seen_row and stripped and not any(d in stripped for d in _DELIMITERS):
                break  # table ended
            continue
        verb = next((c for c in cells[1:] if _VERB_RE.match(c)), None)
        path = next((c for c in cells[1:] if _PATH_RE.match(c)), None)
        if not verb or not path:
            continue
        seen_row = True
        operation = cells[1] if not _VERB_RE.match(cells[1]) and not _PATH_RE.match(cells[1]) else None
        entries.append(ContractEntry(
            api_id=cells[0],
            method=verb.upper(),
            path=path,
            operation=operation or None,
        ))
    return entries


def load_api_registry(plan_file: Optional[Path]) -> list[ContractEntry]:
    if plan_file is None or not plan_file.exists():
        return []
    return parse_api_registry(plan_file.read_text(encoding="utf-8", errors="ignore"))


JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/", re.DOTALL)
CLASS_JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/\s*(?:@[^\n]*\n\s*)*(?:public\s+)?(?:final\s+)?class\b", re.DOTALL)


def _method_javadoc(text: str, method_name: str) -> str:
    lines = text.splitlines()
    decl = security_extractor._find_declaration_index(lines, method_name)
    if decl is None:
        return ""
    i = decl - 1
    depth = 0
    while i >= 0:
        stripped = lines[i].strip()
        depth += lines[i].count(")") - lines[i].count("(")
        if stripped.endswith("*/"):
            start = i
            while start >= 0 and "/**" not in lines[start]:
                start -= 1
            return "\n".join(lines[max(start, 0):i + 1])
        if depth > 0 or stripped.startswith(("@", "+", '"')) or not stripped or stripped.startswith(")"):
            i -= 1
            continue
        break
    return ""


def claimed_ids(source_root: Optional[Path], method: str, path: str,
                registered: set[str], id_prefix: str) -> tuple[list[str], Optional[str]]:
    """Ids of this module's shape that the served endpoint's controller
    Javadoc names and no registry row carries: first the method's own Javadoc,
    else the class Javadoc. Several candidates are all listed -- the reader
    decides, the tool does not. A signpost, never a registration."""
    if source_root is None:
        return [], None
    controller_file, method_name = security_extractor.find_controller_for_endpoint(source_root, method, path)
    if not controller_file or not method_name:
        return [], None
    text = controller_file.read_text(encoding="utf-8", errors="ignore")
    id_re = re.compile(rf"\b{re.escape(id_prefix)}\d+\b")
    ids = [i for i in dict.fromkeys(id_re.findall(_method_javadoc(text, method_name))) if i not in registered]
    if ids:
        return ids, f"{controller_file.stem}.{method_name} Javadoc"
    class_doc = CLASS_JAVADOC_RE.search(text)
    ids = [i for i in dict.fromkeys(id_re.findall(class_doc.group(1) if class_doc else "")) if i not in registered]
    return ids, (f"{controller_file.stem} class Javadoc" if ids else None)


def _id_prefix(entries: list[ContractEntry]) -> str:
    prefixes = {e.api_id.rsplit("-", 1)[0] + "-" for e in entries}
    return prefixes.pop() if len(prefixes) == 1 else "API-"


def attach_contract_ids(document: ApiDocument, entries: list[ContractEntry], source_label: str,
                        source_root: Optional[Path] = None) -> None:
    """Stamps each implemented endpoint with its contract id and records both
    directions of drift on the document. Mutates in place, like the other
    enrich/attach extractors."""
    if not entries:
        return

    document.contract_source = source_label
    by_key: dict[tuple[str, str], ContractEntry] = {}
    for entry in entries:
        by_key.setdefault(_key(entry.method, entry.path), entry)
    registered = {e.api_id for e in entries}
    prefix = _id_prefix(entries)

    matched: set[tuple[str, str]] = set()
    for ep in document.endpoints:
        key = _key(ep.method, ep.path)
        entry = by_key.get(key)
        if entry is None:
            ids, claim_source = claimed_ids(source_root, ep.method, ep.path, registered, prefix)
            document.contract_drift.append(ContractDrift(
                kind="undeclared",
                method=ep.method,
                path=ep.path,
                detail="implemented but absent from the API REGISTRY — it has no contract id "
                       "any other artifact can refer to",
                claimed_ids=ids,
                claim_source=claim_source,
            ))
            continue
        ep.api_id = entry.api_id
        matched.add(key)

    for entry in entries:
        key = _key(entry.method, entry.path)
        if key in matched:
            continue
        document.contract_drift.append(ContractDrift(
            kind="unimplemented",
            api_id=entry.api_id,
            method=entry.method,
            path=entry.path,
            detail="declared in the API REGISTRY but no such endpoint is served — a consumer "
                   "resolving this id from a planning document would call a path that does not exist",
        ))
