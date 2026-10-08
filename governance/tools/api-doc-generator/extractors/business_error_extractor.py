"""Binds a module's business error codes to the endpoints that can raise them by walking controller -> service -> domain call bodies in the module's own source; a code it cannot reach with certainty stays unbound."""

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

from extractors.error_mapping_extractor import THROW_RE, http_status_label
from models.api_doc_model import AccessDeniedOverride, ApiDocument, BusinessError, BusinessErrorWalk, Endpoint

MAX_DEPTH = 8

BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")
STRING_RE = re.compile(r'"(?:[^"\\\n]|\\.)*"')
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
CLASS_DECL_RE = re.compile(r"\b(?:class|enum|record|interface)\s+(\w+)")
FIELD_DECL_RE = re.compile(r"(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?([\w.]+)(?:<[^;=()]*>)?\s+(\w+)\s*(?:=[^;]*)?;")
METHOD_HEAD_RE = re.compile(
    r"(?<![\w.])(?:(?:public|protected|private|static|final|synchronized|default)\s+)*"
    r"(?:<[^>]+>\s+)?([\w.]+(?:<[^(){};]*>)?(?:\[\])*)\s+(\w+)\s*\(")
METHOD_TAIL_RE = re.compile(r"\s*(?:throws\s+[\w.,\s]+?)?\s*\{")
LOCAL_DECL_TEMPLATE = r"(?<![\w.])(?:final\s+)?([A-Z]\w*)(?:<[^;=()]*>)?\s+{name}\s*(?:=|;|:)"
CALL_RE = re.compile(r"(?<![\w.])(\w+)\s*\.\s*(\w+)\s*\(|(?<![\w.])(\w+)\s*\(")
CHAINED_STATIC_RE = re.compile(r"(?<![\w.])([A-Z]\w*)\s*\.\s*(\w+)\s*\([^()]*\)\s*\.\s*(\w+)\s*\(")
METHOD_REF_RE = re.compile(r"(?<![\w.])(\w+)\s*::\s*(\w+)")
DETAIL_RE = re.compile(r"ErrorDetail\s*\.\s*(?:of|ofField)\s*\(\s*(?:[^,()]*,\s*)?\w+\.([A-Z][A-Z0-9_]*)")
WITH_DETAILS_STATUS_RE = re.compile(r"LocalizedException\s*\.\s*withDetails\s*\(\s*Status\.(\w+)")
# The multi-error constructor -- new LocalizedException(Status.X, <List<ErrorDetail>>).
# Its second argument is a collected list, NOT an error-code constant, which is
# exactly what tells it apart from the single-code form THROW_RE already reads.
# This is the Status every ErrorDetail gathered for that throw is answered with,
# and the ONLY place it is stated: a check method that merely RETURNS an
# ErrorDetail names no Status of its own, so without reading the aggregate throw
# its rows can only be published as "not determined".
AGGREGATE_THROW_RE = re.compile(
    r"new\s+LocalizedException\s*\(\s*Status\.(\w+)\s*,\s*(?!\s*\w+\s*\.\s*[A-Z][A-Z0-9_]*\s*[,)])")
CATCH_ACCESS_DENIED_RE = re.compile(r"catch\s*\(\s*(?:[\w.]+\s*\|\s*)*AccessDeniedException\b[^)]*\)\s*\{")
PACKAGE_PREFIX_LITERAL_RE = re.compile(r'"([a-z][\w]*(?:\.[\w]+)+\.)"')
JAVA_KEYWORDS = {
    "if", "for", "while", "switch", "catch", "return", "new", "super", "this", "synchronized",
    "try", "else", "do", "throw", "case", "instanceof", "assert",
}
MODIFIERS = {"public", "protected", "private", "static", "final", "synchronized", "default", "abstract"}
CONSTRUCTOR = "<init>"
NEW_RE = re.compile(r"\bnew\s+([A-Z]\w*)\s*\(")
# A throw whose code is a plain identifier (a parameter or a field), not a Class.CONSTANT:
# the code is supplied by whoever calls the helper, so the throw site is the caller's.
IDENT_THROW_RE = re.compile(r"new\s+LocalizedException\s*\(\s*Status\.(\w+)\s*,\s*([a-z]\w*)\s*[,)]")
FIELD_ASSIGN_TEMPLATE = r"\bthis\.{name}\s*=\s*(\w+)\s*;"
STATIC_CALL_RE = re.compile(r"(?<![\w.])([A-Z]\w*)\s*\.\s*(\w+)\s*\(")
INSTANCE_CALL_RE = re.compile(r"(?<![\w.])([A-Za-z_]\w*)\s*\.\s*(\w+)\s*\(")
CODE_ARG_RE = re.compile(r"^\s*\w+\s*\.\s*([A-Z][A-Z0-9_]*)\s*$")


@dataclass
class MethodInfo:
    name: str
    return_type: str
    params: list[str]
    body: str
    body_start: int
    params_raw: str = ""


@dataclass
class ClassInfo:
    name: str
    package: str
    path: Path
    raw: str
    clean: str
    fields: dict[str, str] = field(default_factory=dict)
    methods: dict[str, list[MethodInfo]] = field(default_factory=dict)


def _mask(text: str) -> str:
    """Comments and string contents become spaces of equal length, so every
    index into the masked text is an index into the original."""
    def blank(m: re.Match) -> str:
        return re.sub(r"[^\n]", " ", m.group(0))
    masked = BLOCK_COMMENT_RE.sub(blank, text)
    masked = LINE_COMMENT_RE.sub(blank, masked)
    return STRING_RE.sub(lambda m: '"' + " " * (len(m.group(0)) - 2) + '"', masked)


def _brace_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _paren_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        ch = text[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _arg_count(text: str, open_index: int) -> int:
    end = _paren_end(text, open_index)
    inner = text[open_index + 1:end - 1]
    if not inner.strip():
        return 0
    depth = 0
    count = 1
    for ch in inner:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        elif ch == "," and depth == 0:
            count += 1
    return count


def _param_types(params: str) -> list[str]:
    if not params.strip():
        return []
    types = []
    depth = 0
    current = ""
    for ch in params:
        if ch in "<([":
            depth += 1
        elif ch in ">)]":
            depth -= 1
        if ch == "," and depth == 0:
            types.append(current.strip())
            current = ""
        else:
            current += ch
    types.append(current.strip())
    cleaned = []
    for t in types:
        t = re.sub(r"@[\w.]+(?:\([^)]*\))?", "", t).strip()
        cleaned.append(t.split()[0] if t.split() else t)
    return cleaned


def _parse_class(path: Path) -> Optional[ClassInfo]:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    clean = _mask(raw)
    decl = CLASS_DECL_RE.search(clean)
    if not decl:
        return None
    pkg = PACKAGE_RE.search(clean)
    info = ClassInfo(name=decl.group(1), package=pkg.group(1) if pkg else "", path=path, raw=raw, clean=clean)
    for m in FIELD_DECL_RE.finditer(clean):
        info.fields.setdefault(m.group(2), m.group(1).split(".")[-1])
    for m in METHOD_HEAD_RE.finditer(clean):
        return_type, name = m.group(1), m.group(2)
        if return_type in MODIFIERS and name == info.name:
            name, return_type = CONSTRUCTOR, info.name
        elif name in JAVA_KEYWORDS or return_type.split("<")[0] in JAVA_KEYWORDS | MODIFIERS | {"else"}:
            continue
        params_end = _paren_end(clean, m.end() - 1)
        tail = METHOD_TAIL_RE.match(clean, params_end)
        if not tail:
            continue
        params = clean[m.end():params_end - 1]
        open_index = tail.end() - 1
        body = clean[open_index:_brace_end(clean, open_index)]
        info.methods.setdefault(name, []).append(
            MethodInfo(name, return_type, _param_types(params), body, open_index, params))
    return info


@dataclass
class CodeHelpers:
    """Helpers that throw a code their caller supplies (e.g. com.erp.common's DomainRules,
    OwnedLookups, StatusTransitions). Read from source, never listed by name:
    - params: (Class, method) -> [(parameter index carrying the code, Status)]
    - bound:  Class -> (constructor parameter index carrying the code, {method: Status})
    - fixed:  Class -> [(code, Status)] thrown with a constant, reached by instantiating the class
    - injected: Class -> {method: [(code, Status)]} -- a shared (package segment "common") @Component held in a field:
      the constant throws its method reaches through its own private methods and static calls into other shared
      classes (tenant-maturity C4 review round 1: IdempotentResponses -> IdempotencyKeyDomain)
    """
    params: dict[tuple[str, str], list[tuple[int, str]]] = field(default_factory=dict)
    bound: dict[str, tuple[int, dict[str, str]]] = field(default_factory=dict)
    fixed: dict[str, list[tuple[str, str]]] = field(default_factory=dict)
    injected: dict[str, dict[str, list[tuple[str, str]]]] = field(default_factory=dict)


def _param_names(params_raw: str) -> list[str]:
    names = []
    depth = 0
    current = ""
    for ch in params_raw + ",":
        if ch in "<([":
            depth += 1
        elif ch in ">)]":
            depth -= 1
        if ch == "," and depth == 0:
            words = re.sub(r"@[\w.]+(?:\([^)]*\))?", "", current).split()
            if words:
                names.append(words[-1])
            current = ""
        else:
            current += ch
    return names


def _call_args(text: str, open_index: int) -> list[str]:
    """Top-level argument texts of the call whose '(' is at open_index."""
    inner = text[open_index + 1:_paren_end(text, open_index) - 1]
    args, depth, current = [], 0, ""
    for ch in inner:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        if ch == "," and depth == 0:
            args.append(current)
            current = ""
        else:
            current += ch
    if current.strip():
        args.append(current)
    return args


INJECTED_MAX_DEPTH = 4


def _is_shared_component(info: ClassInfo) -> bool:
    return "common" in info.package.split(".") and re.search(r"@(?:Component|Service)\b", info.clean) is not None


def _reachable_constant_throws(info: ClassInfo, method: MethodInfo, shared: dict[str, ClassInfo],
                               seen: set[tuple[str, int]], depth: int) -> list[tuple[str, str]]:
    """Constant throws in `method`, its own class's methods it calls, and static (also chained-factory) calls into
    other shared classes -- never a field's or a module's method."""
    key = (info.name, method.body_start)
    if key in seen or depth > INJECTED_MAX_DEPTH:
        return []
    seen.add(key)
    found = [(m.group(2), m.group(1)) for m in THROW_RE.finditer(method.body)]
    targets: list[tuple[ClassInfo, str]] = []
    for m in CALL_RE.finditer(method.body):
        if m.group(3) and m.group(3) in info.methods and m.group(3) not in JAVA_KEYWORDS:
            targets.append((info, m.group(3)))
    for m in STATIC_CALL_RE.finditer(method.body):
        holder, name = m.groups()
        if holder in shared and holder != info.name:
            targets.append((shared[holder], name))
    for m in CHAINED_STATIC_RE.finditer(method.body):
        holder, _, name = m.groups()
        if holder in shared:
            targets.append((shared[holder], name))
    for target_info, name in targets:
        for target in target_info.methods.get(name, []):
            for entry in _reachable_constant_throws(target_info, target, shared, seen, depth + 1):
                if entry not in found:
                    found.append(entry)
    return found


def index_code_helpers(roots: list[Path]) -> CodeHelpers:
    helpers = CodeHelpers()
    shared: dict[str, ClassInfo] = {}
    for root in roots:
        for path in sorted(Path(root).rglob("*.java")):
            info = _parse_class(path)
            if not info:
                continue
            if "common" in info.package.split("."):
                shared.setdefault(info.name, info)
            for name in sorted(info.methods):
                for mi in info.methods[name]:
                    names = _param_names(mi.params_raw)
                    for m in IDENT_THROW_RE.finditer(mi.body):
                        status, ident = m.groups()
                        if ident in names and name != CONSTRUCTOR:
                            entry = (names.index(ident), status)
                            slot = helpers.params.setdefault((info.name, name), [])
                            if entry not in slot:
                                slot.append(entry)
                            continue
                        for ctor in info.methods.get(CONSTRUCTOR, []):
                            assigned = re.search(FIELD_ASSIGN_TEMPLATE.format(name=re.escape(ident)), ctor.body)
                            ctor_names = _param_names(ctor.params_raw)
                            if assigned and assigned.group(1) in ctor_names:
                                _, methods = helpers.bound.setdefault(
                                    info.name, (ctor_names.index(assigned.group(1)), {}))
                                methods.setdefault(name, status)
                                break
                    for m in THROW_RE.finditer(mi.body):
                        entry = (m.group(2), m.group(1))
                        if entry not in helpers.fixed.setdefault(info.name, []):
                            helpers.fixed[info.name].append(entry)
    for name, info in shared.items():
        if not _is_shared_component(info):
            continue
        for method_name, overloads in info.methods.items():
            if method_name == CONSTRUCTOR:
                continue
            reached: list[tuple[str, str]] = []
            for mi in overloads:
                for entry in _reachable_constant_throws(info, mi, shared, set(), 0):
                    if entry not in reached:
                        reached.append(entry)
            if reached:
                helpers.injected.setdefault(name, {})[method_name] = reached
    return helpers


def _helper_throws(cls: ClassInfo, body: str, site: str, helpers: Optional[CodeHelpers],
                   classes: dict[str, ClassInfo]) -> list[BusinessError]:
    """Codes a body raises through a helper: the code constant is an argument of the call (or
    of the constructor that built the helper object held in a field), the Status is the
    helper's own throw. A shared class instantiated here contributes its constant throws."""
    if not helpers:
        return []
    found: list[BusinessError] = []

    def add(code_arg: str, status: str) -> None:
        m = CODE_ARG_RE.match(code_arg)
        if m:
            found.append(BusinessError(code=m.group(1), value=m.group(1), throw_site=site, kind="thrown",
                                       status=status))

    for m in STATIC_CALL_RE.finditer(body):
        holder, name = m.groups()
        for index, status in helpers.params.get((holder, name), []):
            args = _call_args(body, m.end() - 1)
            if index < len(args):
                add(args[index], status)
    for m in INSTANCE_CALL_RE.finditer(body):
        receiver, name = m.groups()
        holder = cls.fields.get(receiver)
        if holder not in classes:
            for code, status in helpers.injected.get(holder, {}).get(name, []):
                found.append(BusinessError(code=code, value=code, throw_site=site, kind="thrown", status=status))
        if holder not in helpers.bound:
            continue
        index, methods = helpers.bound[holder]
        if name not in methods:
            continue
        init = re.search(r"\b" + re.escape(receiver) + r"\s*=\s*new\s+" + re.escape(holder) + r"\s*\(", cls.clean)
        if init:
            args = _call_args(cls.clean, init.end() - 1)
            if index < len(args):
                add(args[index], methods[name])
    for m in NEW_RE.finditer(body):
        holder = m.group(1)
        if holder in classes:
            continue
        for code, status in helpers.fixed.get(holder, []):
            found.append(BusinessError(code=code, value=code, throw_site=site, kind="thrown", status=status))
    return found


def index_module_source(source_root: Path) -> dict[str, ClassInfo]:
    classes: dict[str, ClassInfo] = {}
    for path in sorted(source_root.rglob("*.java")):
        info = _parse_class(path)
        if info:
            classes.setdefault(info.name, info)
    return classes


def aggregate_detail_status(body: str) -> Optional[str]:
    """The Status a method answers a collected ErrorDetail list with, when its
    body states exactly one. Two different aggregate statuses in one body
    resolve to nothing rather than to whichever came first."""
    statuses = set(WITH_DETAILS_STATUS_RE.findall(body)) | set(AGGREGATE_THROW_RE.findall(body))
    return next(iter(statuses)) if len(statuses) == 1 else None


def _throws_in(body: str, site: str, inherited_detail_status: Optional[str] = None) -> list[BusinessError]:
    """`inherited_detail_status` is the aggregate Status of a caller already on
    this walk: a domain check that RETURNS an ErrorDetail is reached only
    through the method that throws the collected list, so that caller's Status
    is this detail's Status. Read from a real throw in a really-walked caller,
    never assumed."""
    found: list[BusinessError] = []
    for m in THROW_RE.finditer(body):
        status, code = m.groups()
        found.append(BusinessError(code=code, value=code, throw_site=site, kind="thrown", status=status))
    status = aggregate_detail_status(body) or inherited_detail_status
    for m in DETAIL_RE.finditer(body):
        found.append(BusinessError(code=m.group(1), value=m.group(1), throw_site=site, kind="detail", status=status))
    return found


def _throw_signature(methods: list[MethodInfo]) -> set[tuple[str, Optional[str], str]]:
    sig = set()
    for mi in methods:
        for be in _throws_in(mi.body, ""):
            sig.add((be.code, be.status, be.kind))
    return sig


def _pick_overload(methods: list[MethodInfo], argc: Optional[int]) -> tuple[Optional[MethodInfo], bool]:
    """(method, ambiguous). Overloads are told apart by arity; when arity
    cannot settle it and the candidates differ in what they throw, nothing is picked."""
    if argc is not None:
        exact = [m for m in methods if len(m.params) == argc
                 or (m.params and m.params[-1].endswith("...") and argc >= len(m.params) - 1)]
        if exact:
            methods = exact
    if len(methods) == 1:
        return methods[0], False
    if len({frozenset(_throw_signature([m])) for m in methods}) == 1:
        return methods[0], False
    return None, True


def _receiver_class(receiver: str, cls: ClassInfo, method: MethodInfo, classes: dict[str, ClassInfo]) -> Optional[str]:
    if receiver == "this":
        return cls.name
    if receiver in classes:
        return receiver
    field_type = cls.fields.get(receiver)
    if field_type in classes:
        return field_type
    local = re.search(LOCAL_DECL_TEMPLATE.format(name=re.escape(receiver)), method.params_raw + " ; " + method.body)
    if local and local.group(1) in classes:
        return local.group(1)
    return None


def _calls_in(cls: ClassInfo, method: MethodInfo, classes: dict[str, ClassInfo]) -> list[tuple[str, str, Optional[int]]]:
    """(class, method, argc) for every call in the body that resolves to a
    class in this module's own source. Anything else -- repositories, mappers,
    shared code, unknown receivers -- is not followed."""
    body = method.body
    calls: list[tuple[str, str, Optional[int]]] = []
    for m in CALL_RE.finditer(body):
        if m.group(3):
            name = m.group(3)
            if name in JAVA_KEYWORDS or name not in cls.methods:
                continue
            calls.append((cls.name, name, _arg_count(body, m.end() - 1)))
            continue
        receiver, name = m.group(1), m.group(2)
        target = _receiver_class(receiver, cls, method, classes)
        if target and name in classes[target].methods:
            calls.append((target, name, _arg_count(body, m.end() - 1)))
    for m in CHAINED_STATIC_RE.finditer(body):
        holder, factory, name = m.groups()
        if holder not in classes:
            continue
        factories = classes[holder].methods.get(factory, [])
        if factories and all(f.return_type.split("<")[0] == holder for f in factories) and name in classes[holder].methods:
            calls.append((holder, name, _arg_count(body, m.end() - 1)))
    for m in METHOD_REF_RE.finditer(body):
        receiver, name = m.groups()
        target = _receiver_class(receiver, cls, method, classes)
        if target and name in classes[target].methods:
            calls.append((target, name, None))
    for m in NEW_RE.finditer(body):
        target = m.group(1)
        if target in classes and CONSTRUCTOR in classes[target].methods:
            calls.append((target, CONSTRUCTOR, _arg_count(body, m.end() - 1)))
    return calls


def site_label(cls_name: str, method_name: str) -> str:
    return f"new {cls_name}()" if method_name == CONSTRUCTOR else f"{cls_name}.{method_name}"


def walk_endpoint(classes: dict[str, ClassInfo], controller: str, method_name: str,
                  max_depth: int = MAX_DEPTH,
                  helpers: Optional[CodeHelpers] = None) -> tuple[list[BusinessError], BusinessErrorWalk]:
    walk = BusinessErrorWalk()
    errors: list[BusinessError] = []
    seen: set[tuple[str, str, int]] = set()
    queue: list[tuple[str, str, Optional[int], int, Optional[str]]] = [(controller, method_name, None, 0, None)]
    while queue:
        cls_name, name, argc, depth, detail_status = queue.pop(0)
        cls = classes.get(cls_name)
        if not cls or name not in cls.methods:
            continue
        method, ambiguous = _pick_overload(cls.methods[name], argc)
        if ambiguous:
            walk.unresolved.append(f"{cls_name}.{name} (overloads differ)")
            continue
        key = (cls_name, name, method.body_start)
        if key in seen:
            continue
        seen.add(key)
        site = site_label(cls_name, name)
        walk.visited.append(site)
        detail_status = aggregate_detail_status(method.body) or detail_status
        errors.extend(_throws_in(method.body, site, detail_status))
        errors.extend(_helper_throws(cls, method.body, site, helpers, classes))
        if depth >= max_depth:
            if _calls_in(cls, method, classes):
                walk.depth_limit_hit = True
            continue
        for target_cls, target_name, target_argc in _calls_in(cls, method, classes):
            queue.append((target_cls, target_name, target_argc, depth + 1, detail_status))
    return errors, walk


def find_access_denied_overrides(classes: dict[str, ClassInfo]) -> list[AccessDeniedOverride]:
    overrides: list[AccessDeniedOverride] = []
    for name in sorted(classes):
        cls = classes[name]
        for m in CATCH_ACCESS_DENIED_RE.finditer(cls.clean):
            block = cls.clean[m.end() - 1:_brace_end(cls.clean, m.end() - 1)]
            throw = THROW_RE.search(block)
            if not throw:
                continue
            prefixes = sorted(set(PACKAGE_PREFIX_LITERAL_RE.findall(cls.raw)))
            overrides.append(AccessDeniedOverride(
                advisor=name, status=throw.group(1), code=throw.group(2), value=throw.group(2),
                scope_prefix=prefixes[0] if len(prefixes) == 1 else None))
    return overrides


def count_throw_sites(classes: dict[str, ClassInfo],
                      helpers: Optional[CodeHelpers] = None) -> dict[str, list[str]]:
    """Code constant -> every Class.method naming it at a throw site or as an
    ErrorDetail, across the whole module source. The expected side of the
    walk's coverage ratio."""
    sites: dict[str, list[str]] = {}
    for name in sorted(classes):
        cls = classes[name]
        for method_name in sorted(cls.methods):
            for mi in cls.methods[method_name]:
                site = site_label(name, method_name)
                for be in _throws_in(mi.body, site) + _helper_throws(cls, mi.body, site, helpers, classes):
                    if be.throw_site not in sites.setdefault(be.code, []):
                        sites[be.code].append(be.throw_site)
    return sites


def _status_rank(http_status: Optional[str]) -> int:
    if not http_status:
        return 999
    head = http_status.split()[0]
    return int(head) if head.isdigit() else 998


def attach_business_errors(document: ApiDocument, classes: dict[str, ClassInfo],
                           controller_of: dict[int, tuple[str, str]],
                           status_http: dict[str, str],
                           helpers: Optional[CodeHelpers] = None) -> None:
    """controller_of maps id(endpoint) -> (ControllerClass, method). Resolves
    each bound code's wire value and HTTP status through the tables the document
    already carries, and records per-code binding counts on document.error_codes."""
    values = {c.name: c.value for c in document.error_codes}
    registered_status = {c.name: c.status for c in document.error_codes if c.status}
    sites = count_throw_sites(classes, helpers)
    # A code's registered Status is the one at its first throw site in source-path order (what
    # error_mapping_extractor reads for direct throws); a helper call site competes on that order.
    first_direct: dict[str, Path] = {}
    first_helper: dict[str, tuple[Path, str]] = {}
    for cls in sorted(classes.values(), key=lambda c: c.path):
        for m in THROW_RE.finditer(cls.raw):
            first_direct.setdefault(m.group(2), cls.path)
        for method_name in sorted(cls.methods):
            for mi in cls.methods[method_name]:
                for be in _helper_throws(cls, mi.body, "", helpers, classes):
                    if be.code not in first_helper or cls.path < first_helper[be.code][0]:
                        first_helper[be.code] = (cls.path, be.status)
    # Only the module's own catalog takes a Status this way; the shared framework codes keep theirs.
    module_codes = {c.name for c in document.error_codes
                    if not re.search(r"(^|[\\/])common[\\/]", c.source_file or "")}
    helper_status = {code: status for code, (path, status) in first_helper.items()
                     if code not in first_direct or path < first_direct[code]}
    bound_counts: dict[str, int] = {}

    for ep in document.endpoints:
        located = controller_of.get(id(ep))
        if not located:
            continue
        errors, walk = walk_endpoint(classes, *located, helpers=helpers)
        ep.business_walk = walk
        unique: dict[tuple[str, Optional[str], str, str], BusinessError] = {}
        for be in errors:
            be.value = values.get(be.code, be.code)
            if not be.status:
                # Last resort: the Status this very code is REGISTERED with in
                # the module catalog (read by error_mapping_extractor from a
                # throw site that names both). A detail-kind row reached through
                # a path that states no Status of its own still answers with the
                # code's own status -- printing "not determined" for a code whose
                # status the same run already published two sections above is a
                # gap in this generator, not in the application.
                be.status = registered_status.get(be.code)
            if be.status:
                be.http_status = http_status_label(status_http.get(be.status))
            unique.setdefault((be.code, be.status, be.throw_site, be.kind), be)
        ep.business_errors = sorted(unique.values(), key=lambda b: (_status_rank(b.http_status), b.code, b.throw_site, b.kind))
        for code in {b.code for b in ep.business_errors}:
            bound_counts[code] = bound_counts.get(code, 0) + 1

    via_codes = {o.code: o.advisor for o in document.access_denied_overrides}
    if document.auth_entry_point:
        via_codes[document.auth_entry_point.code] = document.auth_entry_point.handler
    for code in document.error_codes:
        if code.name in helper_status and code.name in module_codes:
            # The pairing is spelled out at a helper's call site (the code is its argument, the
            # Status its throw's) -- the same evidence a direct throw gives.
            code.status = helper_status[code.name]
            code.http_status = http_status_label(status_http.get(code.status))
        code.throw_sites = sites.get(code.name, [])
        code.bound_endpoints = bound_counts.get(code.name, 0)
        if code.name in via_codes:
            via = sum(1 for ep in document.endpoints if any(pe.code.startswith(code.name + " ") or pe.code == code.name
                                                            for pe in ep.possible_errors))
            code.bound_endpoints = via
            code.bound_via = f"Other Possible Responses, via {via_codes[code.name]}"

    document.source_facts["throw_sites"] = sum(len(v) for v in sites.values())
    document.source_facts["endpoints_with_business_errors"] = sum(1 for ep in document.endpoints if ep.business_errors)
    document.source_facts["endpoints_walked_past_controller"] = sum(
        1 for ep in document.endpoints if ep.business_walk and len(ep.business_walk.visited) > 1)
