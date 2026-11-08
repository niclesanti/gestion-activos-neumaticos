#!/usr/bin/env bash
# Hook PostToolUse (Edit|Write): verificación rápida según el archivo editado.
# La verificación completa (tests + cobertura + lint + build + audit) la hace ./init.sh.
#
#   backend  *.java / *.sql / *.properties / pom.xml -> ./mvnw -q -B test-compile
#   frontend *.ts / *.tsx / *.css / *.json            -> npm run typecheck
#   resto (docs, harness, specs, progress)            -> nada

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

FILE=$(node -e '
let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{
  try{const j=JSON.parse(d);process.stdout.write((j.tool_input&&j.tool_input.file_path)||"")}catch(e){}
})')

# Normaliza separadores de Windows.
FILE="${FILE//\\//}"

case "$FILE" in
  */backend/gestion-neumaticos/*.java|*/backend/gestion-neumaticos/*.sql|*/backend/gestion-neumaticos/*.properties|*/backend/gestion-neumaticos/pom.xml)
    cd "$ROOT/backend/gestion-neumaticos" || exit 0
    echo "[harness] backend: ./mvnw test-compile"
    ./mvnw -q -B test-compile 2>&1 | tail -15
    ;;
  */frontend/gestion-neumaticos/src/*|*/frontend/gestion-neumaticos/scripts/*)
    cd "$ROOT/frontend/gestion-neumaticos" || exit 0
    echo "[harness] frontend: npm run typecheck"
    npm run typecheck 2>&1 | tail -15
    ;;
esac
exit 0
