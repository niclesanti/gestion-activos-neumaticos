#!/usr/bin/env bash
# verify.sh — Corre en local las mismas verificaciones que .github/workflows/ci.yml,
# solo para la parte del repo que cambió en la rama de la feature.
#
# Uso:
#   ./verify.sh              # detecta qué cambió (backend, frontend o ambos)
#   ./verify.sh --backend    # fuerza solo backend
#   ./verify.sh --frontend   # fuerza solo frontend
#   ./verify.sh --all        # fuerza ambos
#
# Qué cambió = todo lo modificado desde el commit donde la rama salió de la base
# (git merge-base): commits de la rama + staged + sin stagear + sin trackear.
#
# Variables:
#   VERIFY_BASE=<rama>   base de comparación (default: origin/develop, luego develop).
#                        No hace git fetch.
#
# Salida: una línea [OK]/[FAIL] por paso. El log completo de cada paso va a
# .verify-logs/<paso>.log; ante un fallo se imprime solo un extracto, para no
# cargar miles de líneas de Maven en el contexto del agente.
# Si cambia el workflow de CI, cambia este script.

set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="backend/gestion-neumaticos"
FRONTEND_DIR="frontend/gestion-neumaticos"
LOG_DIR="$ROOT/.verify-logs"
cd "$ROOT" || exit 1
mkdir -p "$LOG_DIR"

RUN_BACKEND=0
RUN_FRONTEND=0
EXIT_CODE=0

# run_step <nombre-log> <descripción> <comando...>
# Ejecuta en silencio; muestra solo el resultado y, si falla, un extracto del log.
run_step() {
  local name="$1" label="$2"; shift 2
  local log="$LOG_DIR/$name.log"
  if "$@" >"$log" 2>&1; then
    echo "[OK]    $label"
  else
    echo "[FAIL]  $label  (log completo: .verify-logs/$name.log)"
    if grep -qE "ERROR|FAIL|error|✖" "$log"; then
      grep -E "ERROR|FAIL|Tests run:.*Fail|error|✖" "$log" | grep -v "^\[ERROR\] *$" | head -40
    else
      tail -40 "$log"
    fi
    EXIT_CODE=1
    return 1
  fi
}

# ── 1. Coherencia de feature_list.json y specs (siempre; es barato) ──────────
if ! command -v node >/dev/null 2>&1; then
  echo "[FAIL]  node no está instalado"
  exit 1
fi
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
console.log(`[OK]    feature_list.json y specs coherentes (${features.length} features)`);
JS
[ $? -ne 0 ] && EXIT_CODE=1

# ── 2. Qué job correr ────────────────────────────────────────────────────────
case "${1:-}" in
  --backend)  RUN_BACKEND=1 ;;
  --frontend) RUN_FRONTEND=1 ;;
  --all)      RUN_BACKEND=1; RUN_FRONTEND=1 ;;
  "")
    MERGE_BASE=""
    if [ -n "${VERIFY_BASE:-}" ]; then
      MERGE_BASE=$(git merge-base HEAD "$VERIFY_BASE" 2>/dev/null)
    else
      MERGE_BASE=$(git merge-base HEAD origin/develop 2>/dev/null || git merge-base HEAD develop 2>/dev/null)
    fi
    if [ -z "$MERGE_BASE" ]; then
      echo "[WARN]  No se encontró la rama base (origin/develop, develop o VERIFY_BASE); se verifica todo."
      RUN_BACKEND=1; RUN_FRONTEND=1
    else
      CHANGED=$( { git diff --name-only "$MERGE_BASE"; git ls-files --others --exclude-standard; } | sort -u )
      echo "$CHANGED" | grep -q "^$BACKEND_DIR/"       && RUN_BACKEND=1
      echo "$CHANGED" | grep -q "^$FRONTEND_DIR/"      && RUN_FRONTEND=1
      echo "$CHANGED" | grep -q "^\.github/workflows/" && { RUN_BACKEND=1; RUN_FRONTEND=1; }
    fi
    ;;
  *) echo "Uso: $0 [--backend|--frontend|--all]"; exit 2 ;;
esac

if [ "$RUN_BACKEND" -eq 0 ] && [ "$RUN_FRONTEND" -eq 0 ]; then
  echo "[OK]    Sin cambios en backend ni frontend desde la base. Nada más que verificar."
  [ "$EXIT_CODE" -eq 0 ] && exit 0 || exit 1
fi

# ── 3. Backend (job 'backend' del CI) ────────────────────────────────────────
if [ "$RUN_BACKEND" -eq 1 ]; then
  echo "── Backend (job 'backend' del CI) ──"
  if ! docker info >/dev/null 2>&1; then
    echo "[FAIL]  Docker no está corriendo (Testcontainers lo necesita)"
    EXIT_CODE=1
  else
    run_step backend-verify "./mvnw -B verify (tests + JaCoCo >= 85 %)" \
      bash -c "cd '$BACKEND_DIR' && ./mvnw -B verify"
  fi
fi

# ── 4. Frontend (job 'frontend' del CI) ──────────────────────────────────────
if [ "$RUN_FRONTEND" -eq 1 ]; then
  echo "── Frontend (job 'frontend' del CI) ──"
  FE="cd '$FRONTEND_DIR' &&"
  # npm ci solo si falta node_modules o cambió el lockfile (el CI siempre lo corre, en local es caro).
  if [ ! -d "$FRONTEND_DIR/node_modules" ] || [ "$FRONTEND_DIR/package-lock.json" -nt "$FRONTEND_DIR/node_modules" ]; then
    run_step fe-ci "npm ci" bash -c "$FE npm ci" || { echo "[FAIL]  Frontend sin dependencias"; exit 1; }
  fi
  run_step fe-lint      "npm run lint"      bash -c "$FE npm run lint"
  run_step fe-typecheck "npm run typecheck" bash -c "$FE npm run typecheck"
  run_step fe-build     "npm run build"     bash -c "$FE VITE_API_URL=https://api.example.com npm run build"
  run_step fe-audit     "npm audit --omit=dev --audit-level=high" bash -c "$FE npm audit --omit=dev --audit-level=high"
fi

echo "──"
if [ "$EXIT_CODE" -eq 0 ]; then echo "[OK]    Verificación verde."; else echo "[FAIL]  Verificación roja."; fi
exit $EXIT_CODE
