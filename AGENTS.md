# AGENTS.md — Mapa de navegación para agentes de IA

> Este archivo es el **punto de entrada** para cualquier agente que trabaje en este
> repositorio. NO es una biblia de reglas: es un **mapa**. Lee solo lo que
> necesites cuando lo necesites (divulgación progresiva).
>
> Proyecto: aplicación web de gestión del ciclo de vida de neumáticos (backend
> Spring Boot 4 / Java 21 + frontend React 19 / TypeScript). La descripción
> completa del stack y de las convenciones del código está en `CLAUDE.md`.

---

## 1. Antes de empezar (obligatorio)

1. Ejecuta `./init.sh` y verifica que termina sin errores. Si falla, **para**
   y resuelve el entorno antes de tocar código. (Requiere Docker en marcha:
   los tests del backend usan Testcontainers.)
2. Lee `progress/current.md` para entender en qué estado quedó la última sesión.
3. Lee `feature_list.json`. Toda feature nueva (`"sdd": true`) pasa por
   **Spec Driven Development** — ver `docs/harness/specs.md` y §4 de este archivo.
4. Lee `docs/harness/specs.md` antes de tocar cualquier spec o feature `sdd: true`.

## 2. Mapa del repositorio

| Archivo / carpeta                     | Qué contiene                                                                | Cuándo leerlo |
|---------------------------------------|-----------------------------------------------------------------------------|---------------|
| `CLAUDE.md`                           | Stack, arquitectura, convenciones y estado actual del proyecto              | Siempre (se carga solo) |
| `feature_list.json`                   | Lista de tareas con estado (`pending` / `spec_ready` / `in_progress` / `done` / `blocked`) | Siempre, al empezar |
| `progress/current.md`                 | Estado de la sesión actual                                                  | Siempre, al empezar |
| `progress/history.md`                 | Bitácora append-only de sesiones anteriores                                 | Si necesitas contexto histórico |
| `specs/<feature>/`                    | `historia-usuario.md` + `design.md` + `tasks.md` (historia de usuario + diseño + tareas, estilo Kiro)                   | Antes de implementar cualquier feature con `"sdd": true` |
| `docs/harness/architecture.md`        | Qué significa "hacer un buen trabajo" (puntero a las reglas de `CLAUDE.md`) | Antes de implementar |
| `docs/harness/conventions.md`         | Reglas de estilo y nombres (backend y frontend)                             | Antes de escribir código |
| `docs/harness/specs.md`               | Proceso SDD con historias de usuario (Scrumban): los 3 archivos, puerta de aprobación humana              | Antes de redactar o leer un spec |
| `docs/harness/verification.md`        | Cómo verificar el trabajo (incluye trazabilidad criterios de aceptación ↔ tests)       | Antes de declarar una tarea como `done` |
| `docs/Entidades del dominio/*.puml`   | Diagramas de entidades por módulo (fuente de verdad del modelo)             | Antes de modelar entidades de un módulo |
| `docs/Seguridad/`                     | Diseño de seguridad (`seguridad.md`, tabla `@PreAuthorize` en 2.10), pruebas y despliegue | Al tocar auth, endpoints o migraciones |
| `CHECKPOINTS.md`                      | Criterios objetivos de "estado final correcto"                              | Para auto-evaluarte |
| `.claude/agents/`                     | Definiciones de subagentes (`leader`, `spec_author`, `implementer`, `reviewer`) | Si orquestas trabajo |
| `.github/workflows/ci.yml`            | Pipeline de CI; `init.sh` lo replica en local                               | Si cambias verificaciones |
| `backend/gestion-neumaticos/`         | API Spring Boot (código en `src/main`, tests en `src/test`)                 | Para implementar backend |
| `frontend/gestion-neumaticos/`        | App Vite + React + TS (código en `src/`)                                    | Para implementar frontend |

## 3. Reglas duras (no negociables)

- **Una sola feature a la vez.** No mezcles cambios de varias tareas en la misma sesión.
- **No declares una tarea `done` sin verificaciones verdes.** Ejecuta `./init.sh`
  (backend `./mvnw verify` con cobertura JaCoCo ≥ 85 %, frontend lint + typecheck +
  build + audit) y asegúrate de que termina en `[OK]`.
- **No saltes la fase de spec.** Toda feature con `"sdd": true` debe pasar
  por `spec_author` y obtener aprobación humana antes de tocar código.
- **No saltes la puerta de aprobación humana.** El leader detiene el flujo
  en `spec_ready` y espera.
- **Respeta las convenciones de `CLAUDE.md`**: límites de módulo (Modulith),
  `@PreAuthorize` en cada endpoint, migración Flyway con RLS por cada tabla,
  logging en servicios, REST `/api/v1/...`, i18n es/en y kebab-case en el frontend.
- **Documenta lo que haces** en `progress/current.md` mientras trabajas, no al final.
- **Deja el repositorio limpio** antes de cerrar la sesión (ver §5).
- **Si no sabes algo, busca en `docs/` y en `CLAUDE.md`** antes de inventarlo.

## 4. Flujo de trabajo (SDD)

```
pending → [spec_author] → spec_ready → ⏸ HUMANO → in_progress → [implementer → reviewer] → done
```

1. El leader detecta la primera feature `pending` con `"sdd": true`.
2. El leader lanza `spec_author`, que crea
   `specs/<name>/{historia-usuario,design,tasks}.md` y marca el status como
   `spec_ready`.
3. **Pausa.** El humano lee el spec en `specs/<name>/` y aprueba (o pide cambios).
4. Una vez aprobado, el leader cambia el status a `in_progress` y lanza `implementer`.
5. El implementer ejecuta `tasks.md` una a una, marcándolas `[x]`.
5b. Prueba en el stack real: `curl` si fue solo backend; Claude in Chrome (la hace el leader) si
   tocó el frontend. Resultado en `progress/smoke_<name>.md`.
6. El reviewer verifica trazabilidad `CA<n>` ↔ test, tasks completas y el smoke test;
   aprueba o rechaza.
7. Si aprueba, el implementer marca `done` y mueve el resumen a
   `progress/history.md`.

## 5. Cierre de sesión (lifecycle)

Antes de terminar:

1. Ejecuta `./init.sh` — todo verde.
2. Si la tarea está acabada: marca `status: "done"` en `feature_list.json`.
3. Mueve el resumen de `progress/current.md` al final de `progress/history.md`.
4. Vacía `progress/current.md` dejando solo la plantilla.
5. Actualiza la sección "Current state" de `CLAUDE.md` (y `docs/Seguridad/seguridad.md`
   si tocaste auth) cuando la feature cambie el estado del proyecto.
6. No dejes archivos temporales, ni `System.out.println` / `console.log` de debug,
   ni TODOs sin contexto.

## 6. Si te bloqueas

- Relee la sección relevante de `docs/` y de `CLAUDE.md`.
- Si la herramienta no hace lo que esperas, **no inventes un workaround**:
  documenta el bloqueo en `progress/current.md` y para la sesión.
