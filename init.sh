#!/usr/bin/env bash
# init.sh — Verificación e inicialización del entorno
#
# Este script lo ejecuta el agente al COMENZAR una sesión y antes de
# declarar cualquier tarea como `done`. Si falla, la sesión no debe avanzar.
#
# Replica los jobs `backend` y `frontend` de .github/workflows/ci.yml:
#   backend : ./mvnw -B verify   (tests con Testcontainers + JaCoCo >= 85 %)
#   frontend: npm ci, lint, typecheck, build (con VITE_API_URL), npm audit
# Si cambia el workflow, cambia este script.
#
# Variables opcionales (solo para iterar; el cierre de una feature exige la corrida completa):
#   INIT_SKIP_BACKEND=1   omite el job backend
#   INIT_SKIP_FRONTEND=1  omite el job frontend
#   INIT_NPM_CI=1         fuerza `npm ci` aunque exista node_modules
#
# Salida esperada: códigos de salida claros y bloques marcados con [OK]/[FAIL].

set -u
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

ok()    { printf "${GREEN}[OK]${NC}    %s\n" "$1"; }
warn()  { printf "${YELLOW}[WARN]${NC}  %s\n" "$1"; }
fail()  { printf "${RED}[FAIL]${NC}  %s\n" "$1"; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT/backend/gestion-neumaticos"
FRONTEND_DIR="$ROOT/frontend/gestion-neumaticos"
cd "$ROOT" || exit 1

EXIT_CODE=0

# Ejecuta un comando como paso verificable: muestra [OK]/[FAIL] y acumula el error.
run_step() {
  local label="$1"; shift
  echo "  → $label"
  if "$@"; then
    ok "$label"
  else
    fail "$label"
    EXIT_CODE=1
    return 1
  fi
}

echo "── 1. Verificando entorno ─────────────────────────────"

# Node (también lo usa este script para validar feature_list.json)
if ! command -v node >/dev/null 2>&1; then
  fail "node no está instalado"
  exit 1
fi
ok "node -> $(node --version)"

if [ "${INIT_SKIP_FRONTEND:-0}" != "1" ]; then
  NODE_MAJOR=$(node -p 'process.versions.node.split(".")[0]')
  if [ "$NODE_MAJOR" -lt 22 ]; then
    fail "Se requiere Node >= 22 (el CI usa 22), hay $(node --version)"
    exit 1
  fi
  if ! command -v npm >/dev/null 2>&1; then
    fail "npm no está instalado"
    exit 1
  fi
  ok "npm -> $(npm --version)"
fi

if [ "${INIT_SKIP_BACKEND:-0}" != "1" ]; then
  if ! command -v java >/dev/null 2>&1 && [ -z "${JAVA_HOME:-}" ]; then
    fail "java no está disponible (se requiere JDK 21)"
    exit 1
  fi
  JAVA_BIN="java"
  [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && JAVA_BIN="$JAVA_HOME/bin/java"
  JAVA_MAJOR=$("$JAVA_BIN" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
  if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 21 ]; then
    fail "Se requiere JDK >= 21, detectado: ${JAVA_MAJOR:-desconocido}"
    exit 1
  fi
  ok "java -> JDK $JAVA_MAJOR"

  # Los tests del backend levantan PostgreSQL con Testcontainers.
  if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    fail "Docker no está corriendo (Testcontainers lo necesita para ./mvnw verify)"
    exit 1
  fi
  ok "Docker disponible"
fi

echo ""
echo "── 2. Verificando archivos base del arnés ──────────────"

for f in AGENTS.md feature_list.json progress/current.md docs/harness/architecture.md docs/harness/conventions.md docs/harness/specs.md docs/harness/verification.md CHECKPOINTS.md .github/workflows/ci.yml; do
  if [ ! -f "$f" ]; then
    fail "Falta archivo base: $f"
    EXIT_CODE=1
  else
    ok "Existe $f"
  fi
done

echo ""
echo "── 3. Validando feature_list.json y specs ─────────────"

node - <<'JS'
const fs = require("fs");
const path = require("path");
const fail = (m) => { console.log(`[FAIL]  ${m}`); process.exit(1); };
let data;
try {
  data = JSON.parse(fs.readFileSync("feature_list.json", "utf8"));
} catch (e) {
  fail(`feature_list.json inválido: ${e.message}`);
}
const valid = new Set(["pending", "spec_ready", "in_progress", "done", "blocked"]);
const features = data.features;
const inProgress = features.filter((f) => f.status === "in_progress");
if (inProgress.length > 1) {
  fail(`Hay ${inProgress.length} features en in_progress (máximo 1)`);
}
const ids = new Set();
const requiresSpec = new Set(["spec_ready", "in_progress", "done"]);
const errors = [];
for (const f of features) {
  if (ids.has(f.id)) fail(`Id duplicado en feature_list.json: ${f.id}`);
  ids.add(f.id);
  if (!valid.has(f.status)) fail(`Estado inválido en feature ${f.id}: ${f.status}`);
  if (f.sdd && requiresSpec.has(f.status)) {
    const dir = path.join("specs", f.name);
    for (const name of ["historia-usuario.md", "design.md", "tasks.md"]) {
      if (!fs.existsSync(path.join(dir, name))) {
        errors.push(`feature ${f.id} (${f.name}) en ${f.status} sin ${dir}/${name}`);
      }
    }
    if (f.status === "done") {
      const tasks = path.join(dir, "tasks.md");
      if (fs.existsSync(tasks) && /^\s*- \[ \]/m.test(fs.readFileSync(tasks, "utf8"))) {
        errors.push(`feature ${f.id} (${f.name}) está done pero ${tasks} tiene tasks sin marcar`);
      }
    }
  }
}
if (errors.length) {
  errors.forEach((e) => console.log(`[FAIL]  ${e}`));
  process.exit(1);
}
console.log(`[OK]    feature_list.json válido (${features.length} features)`);
console.log("[OK]    Specs presentes para features sdd con estado no-pending");
JS

if [ $? -ne 0 ]; then EXIT_CODE=1; fi

echo ""
echo "── 4. Backend (job 'backend' del CI) ───────────────────"

if [ "${INIT_SKIP_BACKEND:-0}" = "1" ]; then
  warn "Backend omitido (INIT_SKIP_BACKEND=1)"
elif [ ! -d "$BACKEND_DIR" ]; then
  fail "No existe $BACKEND_DIR"
  EXIT_CODE=1
else
  # Tests (Testcontainers) + cobertura mínima de JaCoCo (jacoco:check en verify).
  (cd "$BACKEND_DIR" && ./mvnw -B verify)
  if [ $? -eq 0 ]; then
    ok "./mvnw verify (tests + JaCoCo >= 85 %)"
  else
    fail "./mvnw verify falló (tests rotos o cobertura bajo el mínimo)"
    EXIT_CODE=1
  fi
fi

echo ""
echo "── 5. Frontend (job 'frontend' del CI) ─────────────────"

if [ "${INIT_SKIP_FRONTEND:-0}" = "1" ]; then
  warn "Frontend omitido (INIT_SKIP_FRONTEND=1)"
elif [ ! -d "$FRONTEND_DIR" ]; then
  fail "No existe $FRONTEND_DIR"
  EXIT_CODE=1
else
  cd "$FRONTEND_DIR" || exit 1
  FRONTEND_OK=0
  if [ ! -d node_modules ] || [ "${INIT_NPM_CI:-0}" = "1" ]; then
    run_step "npm ci" npm ci || FRONTEND_OK=1
  else
    ok "node_modules presente (npm ci omitido; INIT_NPM_CI=1 lo fuerza)"
  fi
  if [ "$FRONTEND_OK" -eq 0 ]; then
    run_step "npm run lint" npm run lint
    run_step "npm run typecheck" npm run typecheck
    run_step "npm run build (VITE_API_URL=https://api.example.com)" \
      env VITE_API_URL=https://api.example.com npm run build
    # Falla ante vulnerabilidades altas o críticas en dependencias de producción.
    run_step "npm audit --omit=dev --audit-level=high" npm audit --omit=dev --audit-level=high
  fi
  cd "$ROOT" || exit 1
fi

echo ""
echo "── 6. Resumen ──────────────────────────────────────────"

if [ $EXIT_CODE -eq 0 ]; then
  ok "Entorno listo. Puedes empezar a trabajar."
else
  fail "Entorno NO está listo. Resuelve los errores antes de avanzar."
fi

exit $EXIT_CODE
