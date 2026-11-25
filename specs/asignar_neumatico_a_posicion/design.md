# Design — HU-31 asignar_neumatico_a_posicion

## 1. Contrato

**`PUT /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico`** — `EDITOR`. Body: `neumaticoPublicId` (UUID, obligatorio), `conductorPublicId` (UUID, opcional), `reemplazar` (boolean, default false). 200 `UnidadTransporteDetalleDTO` actualizado. Errores: 400 validación; 403 Lector; 404 unidad, `codigo`, neumático o conductor inexistente o de baja; 409 `PosicionOcupadaException` (nueva) si ocupada sin `reemplazar` o por carrera (índices únicos); 409 `NeumaticoNoDisponibleException` (nueva) si el neumático no está en espera ni en stock (conflicto de estado; no 422). HU-32 usará `DELETE` en la misma ruta.

`UnidadTransporteService.asignarNeumatico`, una transacción: unidad + slot → conductor (`ConductorApi`) → si ocupado y `reemplazar`, `NeumaticoApi.desmontar` (queda `EN_ESPERA`, `enEspera=true`, sin slot) → `NeumaticoApi.montar(publicId, slotId)` (valida disponible; `MONTADO`, `slotMontadoId`, sin centro, stock descontado vía puerto) → slot y `updated_*` de la unidad → publica `NeumaticoDesasignado` (si hubo) y `NeumaticoAsignado` (ids + snapshots de serie, patente, código y conductor).

**Apoyo al buscador** (`hasRole('LECTOR')`):
- `GET /api/v1/neumaticos`: `situacion` pasa a repetible (OR) y nuevo `centro` (UUID; inexistente → 200 vacío). `NeumaticoListadoDTO` suma `centroAlmacenamiento` (nombre, nullable), por lote.
- `GET /api/v1/centros-almacenamiento` → lista de activos `publicId`, `nombre`, `cantidadNeumaticosEnStock`, por nombre.
- `GET /api/v1/conductores?q=&page=&size=` → **paginado** (`Page` vía `PaginacionConfig`) de activos `publicId`, `nombre`, `apellido`, `dni`, por apellido; `q` filtra por apellido, nombre o DNI.

**Frontend — montaje guiado en 3 pasos**:
- Popover del slot en la ficha, si `canPerform('transportUnits.assignTire')`: acción "Montar neumático" (vacío) o "Reemplazar neumático" (ocupado). Lector: sin acciones.
- `MountTireDrawer`: **`drawer` no modal** (derecha en escritorio, abajo en mobile); el slot destino se resalta (anillo + pulso `motion-safe`) vía render-prop de `AxleDiagram`. Cabecera fija "AB123CD · E2-IZQ-EXT · Tracción" + indicador Neumático → Conductor → Confirmar (volver conserva lo elegido). Pie fijo Atrás/Continuar.
  1. **Neumático**: "Buscá por número de serie o filtrá por origen", foco en la búsqueda; elegir (clic o Enter) avanza. Si reemplaza, aviso del saliente.
  2. **Conductor**: "¿Querés asociar un conductor al evento?"; `Combobox` con búsqueda en servidor; "Continuar" u "Omitir".
  3. **Confirmar**: resumen origen → destino (+ conductor y saliente), cada bloque con "Cambiar". `alert`: "queda registrado en el historial de forma inmutable"; `checkbox` "Entiendo…" (texto de reemplazo si aplica: es la advertencia de CA3, envía `reemplazar=true`) habilita "Confirmar montaje".
- Éxito: refresca la ficha, invalida `["tires"]`, `notify`, cierra. 409 ocupada (carrera) → refetch y vuelve al paso 3 como reemplazo; 409 no disponible / 404 → paso 1 con aviso inline y refetch.
- `TirePicker` (feature `tires`; reutiliza `useTires`, la búsqueda y `FilterSelect`): select "Origen" (disponibles / en espera / en stock / un centro), lista `RadioGroup` (serie, marca, estado, "En espera" o centro), `TablePagination`. Lo compone `router.tsx` por el prop `renderTirePicker` de `TransportUnitsPage` (como `catalogs`); contrato en `lib/tire-picker.ts`.
- Reutiliza `popover`, `badge`, `select`, `input`, `empty`. **A instalar**: `npx shadcn@latest add drawer combobox radio-group checkbox alert`.

## 2. Archivos

Backend (`.../gestion_neumaticos/`):
- `neumaticos/api/{NeumaticoApi,NeumaticoMontajeDTO,StockCentroAlmacenamiento}.java` — montar/desmontar; puerto de stock (id por publicId, nombres por lote, retirar uno).
- `neumaticos/{service,domain,controller}/**` — reglas de montaje, filtros y nombre del centro.
- `almacenamiento/service/StockCentroAlmacenamientoImpl.java` + listado de centros.
- `conductores/api/{package-info,ConductorApi,ConductorResumenDTO}.java`, `service/ConductorApiImpl.java` + listado de conductores.
- `unidades_transporte/**` — request, servicio y endpoint.
- `unidades_transporte/event/{package-info,NeumaticoAsignado,NeumaticoDesasignado}.java` — `@NamedInterface("event")`.
- `trazabilidad/domain/entity/{EventoAsignacion,EventoDesasignacion}.java`, `listener/MontajeNeumaticoListener.java`.
- `exception/{PosicionOcupadaException,NeumaticoNoDisponibleException,ControllerAdvisor}.java` — 409.

Frontend (`src/`): `components/ui/*` (CLI); `lib/{tire-picker,access}.ts`; `features/tires/{api,hooks}/*` (centros, búsqueda) y `components/tire-picker.tsx`; `features/transport-units/{api,hooks}/*` (conductores, montar) y `components/mount-tire-drawer.tsx` + ficha, popover y página; `app/router.tsx`; i18n `{es,en}/{transport-units,tires}.json`.

Docs: `seguridad.md` 2.10.

## 3. Migración

`V20__asignacion_neumaticos.sql`: `GRANT UPDATE (neumatico_actual_id)` en slots + política `UPDATE` (admin/editor); `GRANT UPDATE (updated_at, updated_by)` en `unidades_transporte` + política firmada como `V19`; índices únicos parciales `slots(neumatico_actual_id)` y `neumaticos(slot_montado_id)` `WHERE NOT NULL`; en `eventos_neumatico` columnas nullable de unidad (id, patente), slot (id, posición) y conductor (id, nombre) + `CHECK` (solo en `ASIGNACION`/`DESASIGNACION`, con unidad y slot obligatorios).

## 4. Desvíos de convención

- Slot por `codigo` en la URL (sin `publicId`; único en la unidad).
- Stock por puerto en `neumaticos.api` (lo implementa `almacenamiento`; la dependencia inversa sería ciclo). Nuevas: `unidades_transporte → conductores.api`, `trazabilidad → unidades_transporte.event`.
- El listado de centros no se pagina; el de conductores sí (cientos).
- `tipoActual`/`tipoOrigen` y kilometraje no cambian al montar (confirmado por el humano).

## 5. Alternativas descartadas

- **Stock por listener**: pierde atomicidad.
- **`Sheet` modal o página propia**: tapan el chasis.
- **Formulario único + `AlertDialog`**: menos guiado.
