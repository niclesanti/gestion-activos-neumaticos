# AGENTS.md — Mapa del repositorio

> Punto de entrada para cualquier agente. Es un mapa, no un reglamento: lee solo lo que necesites.
> Stack y convenciones del código: `CLAUDE.md` (ya cargado en cada sesión).

| Archivo / carpeta | Qué contiene | Cuándo leerlo |
|---|---|---|
| `CLAUDE.md` | Stack, arquitectura, convenciones | Se carga solo |
| `feature_list.json` | Historias (HU) con estado: `pending` → `spec_ready` → `in_progress` → `done` / `blocked` | Al elegir la feature |
| `docs/harness/flujo.md` | Flujo SDD completo, protocolo del leader | Solo el leader (sesión principal) |
| `docs/harness/specs.md` | Formato y topes de `historia-usuario.md`, `design.md`, `tasks.md` | `spec_author` |
| `docs/harness/verification.md` | Niveles de verificación y `./verify.sh` | `implementer`, `reviewer` |
| `docs/harness/conventions.md` | Convenciones que más se violan + checklist del reviewer | `implementer`, `reviewer` |
| `specs/<name>/` | Spec aprobado de la feature | `implementer`, `reviewer` |
| `docs/estado-actual.md` | Qué existe, por módulo y branch | Solo si necesitás saber qué hay |
| `progress/history.md` | Bitácora de sesiones anteriores (memoria compartida) | Solo si necesitás contexto histórico |
| `progress/impl_*.md`, `review_*.md` | Informes de implementación y revisión | Quien los produce o consume |
| `docs/Entidades del dominio/*.puml` | Diagramas de entidades por módulo | Antes de modelar un módulo |
| `docs/Seguridad/` | Diseño de seguridad, tabla `@PreAuthorize` (2.10) | Al tocar auth, endpoints o migraciones |
| `verify.sh` | Replica el CI en local para lo que cambió (`.github/workflows/ci.yml`) | Lo corre el leader una vez al terminar |
| `.claude/agents/` | `spec_author`, `implementer`, `reviewer` | Al orquestar |
