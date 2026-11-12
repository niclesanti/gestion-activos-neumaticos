# Spec Driven Development (SDD)

> Flujo adaptado a historias de usuario (Scrumban): historia → design → tasks → código.
> No se escribe código hasta que un humano aprueba el spec. El spec es para **revisarse**:
> si no se puede leer a fondo en pocos minutos, es demasiado largo.

```
specs/<name>/                  # <name> = campo `name` de feature_list.json
├── historia-usuario.md        # QUÉ   (≤ 4 KB)
├── design.md                  # CÓMO  (≤ 6 KB)
└── tasks.md                   # PASOS (≤ 3 KB)
```

Los topes son máximos, no objetivos: una HU chica ocupa mucho menos.

## Estados (`feature_list.json` es el tablero Kanban, WIP = 1)

| Estado        | Significado                                          |
|---------------|------------------------------------------------------|
| `pending`     | Sin spec. Actúa el `spec_author`.                    |
| `spec_ready`  | Spec redactado, esperando aprobación humana.         |
| `in_progress` | Spec aprobado, `implementer` trabajando.            |
| `done`        | `verify.sh` verde, reviewer aprobó, leader cerró.    |
| `blocked`     | Atascado; razón en `progress/impl_<name>.md`.        |

## `historia-usuario.md` — el qué

```markdown
# HU-<n> - <título>

**Historia**: Como <rol>, quiero <objetivo>, para <beneficio>.

**Criterios de Aceptación**:
- CA1: Dado <contexto>, cuando <acción>, entonces <resultado observable>.

**Notas / Supuestos**: <aclaraciones de negocio, dependencias, fuera de alcance>.
```

- La fuente es `feature_list.json` (`historia`, `descripcion`, `acceptance`): se **transcribe** y solo se agrega la numeración `CA<n>`. No se cambia la intención ni se inventan criterios.
- Un `CA` = un comportamiento observable y verificable por un test concreto. Si mezcla varios resultados, se parte.
- Sin lenguaje ambiguo ("rápido", "correctamente"): status HTTP, campo, mensaje, estado persistido.
- Backend: contrato HTTP (método, ruta, rol, status, cuerpo) o regla de dominio. Frontend: comportamiento observable.
- **Las Notas no llevan decisiones técnicas** (nombres de clases, serialización, escapes, etc.): van en `design.md`.
- **Lo que no pertenece a la HU no se mete como `CA`.** Si surge un cambio relacionado (p. ej. unificar un comportamiento de otro módulo), se propone como entrada aparte en `feature_list.json` o se marca "fuera de alcance — decide el humano".
- Una HU demasiado grande se parte en varias entradas antes de especificarla.

## `design.md` — el cómo (solo lo que hay que decidir o revisar)

Secciones fijas:

1. **Contrato**: endpoint(s), DTO (campos), errores, rol requerido. Frontend: pantalla/comportamiento.
2. **Archivos**: una línea por archivo a crear/modificar, con ruta y propósito.
3. **Migración**: número, tablas, RLS, privilegios, auditoría (si hay).
4. **Decisiones que se apartan de las convenciones** de `CLAUDE.md` / `conventions.md` (y dependencias nuevas con justificación).
5. **Alternativas descartadas**: 1 a 3, una línea de motivo cada una.

**No incluye**: código, contenido de métodos, mensajes de log, nombres de constantes, descripción del código actual, ni lo que ya dicen las convenciones: eso lo resuelve el implementer.

## `tasks.md` — checklist ejecutable

Una línea por task, en el orden natural del stack (migración → entidad/repo → mapper/DTO → servicio → controlador → tests backend → frontend → i18n → docs):

```markdown
- [ ] T1 — Crear migración `V20` con RLS y privilegios. Archivos: `db/migration/V20__...sql`. Cubre: CA2.
```

- No repite `design.md` (firmas, rutas largas, lógica) ni nombra cada test: los nombres de test los decide el implementer.
- **No lleva matriz de trazabilidad**: se arma una sola vez, en `progress/impl_<name>.md`.
- El implementer marca `[x]`; el reviewer rechaza si queda alguna `[ ]` sin justificación.

## Trazabilidad

Cada `CA<n>` tiene al menos un test concreto (JUnit) o, en UI sin runner de tests, la verificación documentada en `impl_<name>.md` (lint + typecheck + comprobación en navegador). El `spec_author` prefiere criterios de frontend verificables por esos medios. Si resulta insuficiente, se propone un runner de tests como feature aparte.
