#!/usr/bin/env bash
# =====================================================================
# rename-project.sh — re-brand this backend template to a new project.
#
# The base package (com.erp) and every "erp"-derived identity token are the
# single variable of this template. Run this ONCE on a fresh clone to make the
# project yours; after that, `erp` is your project name everywhere.
#
#   ./scripts/rename-project.sh <new-base-name>
#   e.g.  ./scripts/rename-project.sh acme      -> com.acme, AcmeMainApplication,
#                                                  acme-system, acme_db, "Acme System"
#
# What it changes (everywhere except .git / target / node_modules / this script):
#   com.erp            -> com.<new>            (package decls, imports, mainClass, logging)
#   src/main/java/com/erp/  directory          -> src/main/java/com/<new>/
#   ErpMainApplication -> <New>MainApplication (class + file)
#   erpMainJpaConfig   -> <new>MainJpaConfig   (bean name)
#   erp-system         -> <new>-system         (artifactId, app name, OpenAPI, emails)
#   ERP System         -> <New> System         (display strings, i18n)
#   erp_db / erp-backend / erp-postgres -> <new>_db / <new>-backend / <new>-postgres
#
# Idempotent-ish: safe to inspect the diff before committing. After running,
# rebuild:  mvn -q -DskipTests compile
# =====================================================================
set -euo pipefail

NEW="${1:-}"
if [[ -z "$NEW" ]]; then
  echo "usage: $0 <new-base-name>   (lowercase letters/digits, e.g. acme)" >&2
  exit 1
fi
if [[ ! "$NEW" =~ ^[a-z][a-z0-9]*$ ]]; then
  echo "error: name must be lowercase letters/digits starting with a letter (got '$NEW')" >&2
  exit 1
fi
if [[ "$NEW" == "erp" ]]; then
  echo "nothing to do — the project is already 'erp'." ; exit 0
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Pascal-case: acme -> Acme
PASCAL="$(printf '%s' "${NEW:0:1}" | tr '[:lower:]' '[:upper:]')${NEW:1}"

echo "Renaming project:  erp -> ${NEW}   (class prefix Erp -> ${PASCAL})"
echo "Repo root: $ROOT"

# 1) Move the Java package directory (main + test if present)
for src in src/main/java src/test/java; do
  if [[ -d "$src/com/erp" ]]; then
    mkdir -p "$src/com"
    mv "$src/com/erp" "$src/com/${NEW}"
    echo "  moved $src/com/erp -> $src/com/${NEW}"
  fi
done

# 2) Rename the main application class file
OLD_MAIN="$(find src -name 'ErpMainApplication.java' 2>/dev/null | head -1 || true)"
if [[ -n "$OLD_MAIN" ]]; then
  NEW_MAIN="$(dirname "$OLD_MAIN")/${PASCAL}MainApplication.java"
  mv "$OLD_MAIN" "$NEW_MAIN"
  echo "  renamed $(basename "$OLD_MAIN") -> $(basename "$NEW_MAIN")"
fi

# 3) Token substitutions across every text file (excluding vcs/build/this script)
mapfile -d '' FILES < <(grep -rlIZ \
  --exclude-dir=.git --exclude-dir=target --exclude-dir=node_modules --exclude-dir=scripts \
  -e 'com\.erp' -e 'ErpMainApplication' -e 'erpMainJpaConfig' \
  -e 'erp-system' -e 'erp_db' -e 'erp-backend' -e 'erp-postgres' \
  -e 'ERP System' -e 'ERP Development' . 2>/dev/null || true)

for f in "${FILES[@]}"; do
  sed -i \
    -e "s/com\.erp/com.${NEW}/g" \
    -e "s/ErpMainApplication/${PASCAL}MainApplication/g" \
    -e "s/erpMainJpaConfig/${NEW}MainJpaConfig/g" \
    -e "s/erp-system/${NEW}-system/g" \
    -e "s/erp_db/${NEW}_db/g" \
    -e "s/erp-backend/${NEW}-backend/g" \
    -e "s/erp-postgres/${NEW}-postgres/g" \
    -e "s/ERP System/${PASCAL} System/g" \
    -e "s/ERP Development/${PASCAL} Development/g" \
    "$f"
done
echo "  updated ${#FILES[@]} file(s)"

echo ""
echo "Done. Next:"
echo "  1) review the changes (git diff, if this is a git repo)"
echo "  2) mvn -q -DskipTests compile      # verify it still builds on JDK 25"
echo "  3) update DB_NAME in .env if you want a matching database name"
