# Spec Driven Development (SDD)

> Este proyecto sigue un flujo Kiro-style adaptado a historias de usuario (Scrumban): historia → design → tasks → code.
> El código no se escribe hasta que el spec está aprobado por un humano.

## Estructura

Cada feature nueva (`"sdd": true` en `feature_list.json`) tiene una carpeta
dedicada en cuanto deja `pending`:

```
specs/<feature-name>/
├── historia-usuario.md   # QUÉ se necesita (historia de usuario + criterios de aceptación)
├── design.md         # CÓMO se construirá (decisiones técnicas)
└── tasks.md          # PASOS concretos a implementar
```

El `feature-name` coincide con el campo `name` de `feature_list.json`.

## Estados de una feature

| Estado         | Significado                                                    |
|----------------|----------------------------------------------------------------|
| `pending`      | Sin spec. El `spec_author` es el primero en actuar.            |
| `spec_ready`   | Spec redactado. Esperando aprobación humana. NO se toca código.  |
| `in_progress`  | Spec aprobado. `implementer` trabajando.                       |
| `done`         | Verificaciones verdes, `reviewer` aprobó, sesión cerrada.      |
| `blocked`      | Atascado. Razón en `progress/current.md`.                      |

## La puerta de aprobación humana

El flujo automático se detiene **una vez**: cuando el `spec_author` termina
sus tres archivos, marca la feature como `spec_ready` y para. El humano
lee `specs/<feature>/` y dice "aprobado" (o pide cambios).

Solo entonces el `leader` transiciona `spec_ready → in_progress` y lanza
el `implementer`.

```
pending → [spec_author] → spec_ready → ⏸ HUMANO → in_progress → [implementer → reviewer] → done
```

## historia-usuario.md — la historia y sus criterios de aceptación

Trabajamos con **historias de usuario** (Scrumban). El archivo reproduce la HU
con el mismo formato que usa el equipo (ver `HU-43.md` en la raíz como modelo):

```markdown
# HU-<n> - <título>

**Historia**: Como <rol>, quiero <objetivo>, para <beneficio>.

**Criterios de Aceptación**:

- CA1: Dado <contexto>, cuando <acción o evento>, entonces <resultado observable>.
- CA2: Dado ..., cuando ..., entonces ...

**Notas / Supuestos**: <aclaraciones, dependencias, fuera de alcance>.
```

Reglas duras:

- Si el humano ya escribió la HU, el `spec_author` la **transcribe tal cual** y solo
  agrega la numeración `CA<n>`; no cambia su intención ni agrega criterios inventados.
- Cada criterio de aceptación tiene un id estable: `CA1`, `CA2`, ...
- Cada `CA<n>` tiene que ser verificable por al menos un test concreto.
- Un criterio = un solo comportamiento observable. Si un criterio mezcla varios
  resultados ("entonces A y además B"), se parte en dos.
- Evitá lenguaje ambiguo ("rápido", "amigable", "correctamente"): expresá el
  resultado concreto (status HTTP, mensaje, campo, estado persistido).
- Los criterios de backend se expresan en términos de contrato HTTP
  (método, ruta, rol, status, cuerpo) o de reglas de dominio observables.
- Los criterios de frontend se expresan en términos de comportamiento
  observable (qué muestra, qué request dispara, qué valida el esquema zod).
- Una HU demasiado grande para entrar en una sola feature se parte en varias
  historias antes de especificarla (cada una con su entrada en `feature_list.json`).

Ejemplo:

```markdown
# HU-30 - Listar centros de almacenamiento

**Historia**: Como usuario Lector, quiero ver el listado de centros de almacenamiento,
para consultar su ubicación y cantidad de neumáticos en stock.

**Criterios de Aceptación**:

- CA1: Dado un usuario con nivel Lector o superior, cuando solicita
  `GET /api/v1/centros-almacenamiento`, entonces el sistema responde 200 con una
  página de centros (publicId, nombre, domicilio, cantidad en stock).
- CA2: Dado un usuario sin sesión, cuando solicita el listado, entonces el sistema
  responde 401 con un cuerpo `ExceptionInfo`.

**Notas / Supuestos**: el alta ya existe; no incluye filtros.
```

## Scrumban y el tablero

El `feature_list.json` es el tablero Kanban del repo y cada feature es una historia:

| Columna del tablero | Estado en `feature_list.json` |
|---------------------|--------------------------------|
| Backlog / Por hacer | `pending`                      |
| Especificación lista (esperando OK) | `spec_ready`   |
| En curso            | `in_progress`                  |
| Hecho               | `done`                         |
| Bloqueada           | `blocked`                      |

El límite de trabajo en curso (WIP) es **1**: una sola historia en `in_progress`
(`init.sh` lo verifica). Se tira de la siguiente historia solo cuando la anterior
llega a `done`; no hay sprints dentro del arnés, el flujo es continuo.
La definición de "hecho" es: criterios de aceptación cubiertos, tasks completas,
reviewer APPROVED y `./init.sh` verde.

## design.md — decisiones técnicas

Captura **antes** de tocar código:

- Qué archivos se crean / modifican (backend y frontend, con ruta completa).
- Qué módulo Modulith es dueño del cambio y qué API pública cruza módulos.
- Qué migración Flyway se agrega (número, tablas, RLS, privilegios, auditoría).
- Qué firmas nuevas aparecen (endpoints, servicios, DTOs, hooks, componentes).
- Qué excepciones se reutilizan o se añaden; qué rol exige cada endpoint.
- Qué strings de i18n y qué entradas de `ROUTES` / `NAV_GROUPS` / `access.ts` cambian.
- Qué alternativa se descartó y por qué (mínimo una).

NO es ingeniería desde primeros principios — apóyate en
`docs/harness/architecture.md`, `docs/harness/conventions.md` y `CLAUDE.md`.
El `design.md` documenta los puntos donde tu feature roza la frontera de esas reglas.

## tasks.md — checklist ejecutable

Pasos discretos en orden, cada uno con checkbox. Cada task referencia al
menos un `CA<n>` que cubre. Las tasks respetan el orden natural del stack:
migración → entidad/repositorio → mapper/DTO → servicio → controlador →
tests del backend → función de API / esquema / hook / componente del frontend →
i18n → documentación (`CLAUDE.md`, `docs/Seguridad/seguridad.md`).

Ejemplo:

```markdown
- [ ] T1 — Crear `V20__...sql` con RLS y privilegios mínimos. Cubre: CA2.
- [ ] T2 — Añadir `listar` en `CentroAlmacenamientoController` con `@PreAuthorize("hasRole('LECTOR')")`. Cubre: CA1, CA2.
- [ ] T3 — Añadir `CentroAlmacenamientoControllerTest.listar_devuelve200` (`@WebMvcTest`). Cubre: CA1.
- [ ] T4 — Añadir `CentroAlmacenamientoControllerTest.listar_comoSinRol_devuelve401`. Cubre: CA2.
```

El `implementer` marca `[x]` cada task al completarla. El `reviewer`
rechaza si queda alguna `[ ]` sin justificación documentada.

## Trazabilidad (regla dura)

- Cada test nuevo debe poder mapearse a un `CA<n>` de su spec.
- Cada `CA<n>` debe tener al menos un test concreto.
- El `reviewer` comprueba esta correspondencia explícitamente y rechaza
  si falta.
- El frontend no tiene runner de tests: un `CA<n>` de UI se cubre con la
  verificación que documenta el implementer (lint, typecheck, build y la
  comprobación manual concreta que hizo), y el `spec_author` debe preferir
  criterios de frontend verificables por esos medios. Si esto resulta
  insuficiente para una feature, se propone agregar un runner de tests como
  feature aparte (no se instala dentro de otra feature).

El `implementer` documenta el mapa en `progress/impl_<name>.md`:

```markdown
## Trazabilidad
- CA1 → `CentroAlmacenamientoControllerTest.listar_devuelve200`
- CA2 → `CentroAlmacenamientoControllerTest.listar_comoSinRol_devuelve401`
- CA3 → (frontend) `npm run typecheck` + verificación manual: la tabla se refresca tras el alta
```

## Cuándo NO aplica SDD

Las features con `"sdd": false` o sin el campo `sdd` (las ya implementadas antes
del arnés, ids 1–7) NO tienen spec. SDD solo se aplica hacia adelante.
