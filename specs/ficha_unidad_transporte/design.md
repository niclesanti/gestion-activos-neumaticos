# Design — HU-10 ficha_unidad_transporte

## 1. Contrato

**`GET /api/v1/unidades-transporte/{publicId}`** — `hasRole('LECTOR')` (los tres roles).
- 200 `UnidadTransporteDetalleDTO`: `publicId`, `patente`, `configuracion` (`ConfiguracionUnidadDTO` completo, igual al de `/configuraciones`), `slots`: `SlotDetalleDTO` = campos de `SlotUnidadTransporteDTO` + `neumatico` (null si vacío).
- `NeumaticoMontadoDTO` (cruza módulos, `neumaticos.api`): `publicId`, `numeroSerie` (nullable), `marca` (nombre), `estado` (nombre del enum como `String`, para no exponer `EstadoNeumatico` interno), `kilometrajeTotal` (nullable).
- 404 `EntidadNoEncontradaException` si no existe o está dada de baja (`deletedAt` no null). 400 si `publicId` no es UUID. 401 sin token.
- Sin N+1: unidad, configuración, ejes y slots por entity graph; neumáticos en **una** llamada por lote a `NeumaticoApi`.

**Frontend** (feature `transport-units`):
- Cada fila de `TransportUnitsTable` es clicable y la patente es un botón (teclado/lector de pantalla) que abre `TransportUnitDetailDialog` (Dialog controlado por el `publicId` seleccionado). La query se dispara solo con el dialog abierto.
- Dialog: título = patente; badge de tipo de unidad; bloque "Datos de la unidad" en un **Collapsible** (configuración, tipo, ejes, posiciones ocupadas/total), abierto por defecto; debajo el diagrama. Estados: Skeleton al cargar, `Empty` con reintento ante error, mensaje propio ante 404.
- Diagrama: `AxleDiagram`, extraído de `AxleLayoutPreview`. Recibe la configuración y, opcionalmente, el estado por código de slot (`occupied`) y un render-prop por rueda. Rueda vacía = color de fondo (como hoy); ocupada = relleno `primary`; leyenda "Ocupada / Vacía". `AxleLayoutPreview` queda como envoltorio sin estado (el alta no cambia). Pensado para HU-31: agregará selección/acciones por el mismo render-prop, sin tocar el dibujo.
- Cada rueda del detalle es el trigger (un `button` con `aria-label` = código + estado) de un **Popover** con `openOnHover`: ocupada → serie, marca, estado, kilometraje (o "No registrado"); vacía → código, tipo de eje y "Sin neumático". La lista `sr-only` del diagrama incluye la serie del neumático en cada posición ocupada.
- UI instalada que se reutiliza: `dialog`, `badge`, `skeleton`, `empty`, `separator`, `button`. **A instalar**: `collapsible` y `popover` (`npx shadcn@latest add collapsible popover`; con `base-luma` el CLI genera la variante Base UI).

## 2. Archivos

Backend (`backend/gestion-neumaticos/src/main/java/.../gestion_neumaticos/`):
- `neumaticos/api/package-info.java` — `@NamedInterface("api")`.
- `neumaticos/api/NeumaticoApi.java` — buscar neumáticos por lote de ids internos → mapa id → `NeumaticoMontadoDTO`.
- `neumaticos/api/NeumaticoMontadoDTO.java` — record del contrato.
- `neumaticos/service/NeumaticoApiImpl.java` — implementación (lectura, `readOnly`).
- `neumaticos/repository/NeumaticoRepository.java` — búsqueda por ids con `marca` en el mismo fetch.
- `unidades_transporte/domain/dto/UnidadTransporteDetalleDTO.java`, `SlotDetalleDTO.java` — respuesta.
- `unidades_transporte/repository/UnidadTransporteRepository.java` — búsqueda activa por `publicId` con entity graph.
- `unidades_transporte/mapper/UnidadTransporteMapper.java` — mapeo a detalle.
- `unidades_transporte/service/UnidadTransporteService(Impl).java` — `obtenerDetalle(publicId)`.
- `unidades_transporte/controller/UnidadTransporteController.java` — `GET /{publicId}`.
- `exception/ControllerAdvisor.java` — `MethodArgumentTypeMismatchException` → 400 `ExceptionInfo` (primer endpoint con `{publicId}`; hoy caería en el genérico → 500).
- Tests: fixtures en `TestDataFactory`; snippet REST Docs `unidad-transporte-detalle`.

Frontend (`frontend/gestion-neumaticos/src/`):
- `components/ui/collapsible.tsx`, `components/ui/popover.tsx` — generados por el CLI.
- `features/transport-units/api/transport-units-api.ts` — tipos `MountedTire`, `TransportUnitDetail` y `getTransportUnit`.
- `features/transport-units/hooks/use-transport-unit.ts` — query por `publicId` (`enabled` con dialog abierto).
- `features/transport-units/components/axle-diagram.tsx` — dibujo común con estados y render-prop.
- `features/transport-units/components/axle-layout-preview.tsx` — pasa a usar `AxleDiagram`.
- `features/transport-units/components/slot-popover.tsx` — trigger + contenido por posición.
- `features/transport-units/components/transport-unit-detail-dialog.tsx` — dialog de la ficha.
- `features/transport-units/components/transport-units-table.tsx` — fila/patente abren el dialog.
- `lib/i18n/locales/{es,en}/transport-units.json` — claves `detail.*`, estados de neumático, leyenda.

Docs: `docs/Seguridad/seguridad.md` 2.10 — fila del `GET`.

## 3. Migración

Ninguna: los tres roles ya tienen `SELECT` y políticas sobre `unidades_transporte.*` (V16) y `neumaticos.*` (V10).

## 4. Desvíos de convención

- `NeumaticoApi` recibe ids **internos**: es la referencia que el slot ya guarda (`neumatico_actual_id`, sin FK, según el diagrama). No salen al cliente (la respuesta expone `publicId`).
- Se crea `SlotDetalleDTO` en vez de agregar `neumatico` a `SlotUnidadTransporteDTO`, para no alterar el contrato ni el snippet REST Docs del `POST`.
- Dependencia `unidades_transporte → neumaticos.api`. Si HU-14/HU-31 necesitan la inversa (patente desde `neumaticos`), habrá ciclo: se resolverá con eventos o moviendo la consulta, no ahora.
- Hasta HU-31 no se pueden montar neumáticos: CA2/CA3 se ven en navegador asignando `neumatico_actual_id` por SQL en dev (documentado en `impl_`).

## 5. Alternativas descartadas

- **Hover Card** (`PreviewCard` de Base UI): solo hover/foco, no se abre con toque y no está pensada para el contenido principal; Popover con `openOnHover` da hover + clic + teclado + táctil, que exige CA3.
- **Tooltip**: solo texto breve no interactivo, inaccesible en táctil; no sirve para 4 datos.
- **Página `/transport-units/:publicId`**: el humano pidió dialog y la HU no exige URL compartible; puede sumarse luego con `AxleDiagram`.
