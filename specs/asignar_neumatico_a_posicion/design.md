# Design — HU-31 asignar_neumatico_a_posicion

## 1. Contrato

**`PUT /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico`** — `hasRole('EDITOR')`. Body: `neumaticoPublicId` (UUID, obligatorio), `conductorPublicId` (UUID, opcional), `reemplazar` (boolean, default false). 200 `UnidadTransporteDetalleDTO` actualizado. Errores: 400 validación; 403 Lector; 404 unidad, `codigo`, neumático o conductor inexistente o de baja; 409 `PosicionOcupadaException` (nueva) si ocupada sin `reemplazar` o por carrera (índices únicos); 422 `OperacionNoPermitidaException` si el neumático no está disponible. HU-32 usará `DELETE` en la misma ruta.

Orquesta `UnidadTransporteService.asignarNeumatico` en una transacción: unidad + slot → conductor (`ConductorApi`) → si ocupado y `reemplazar`, `NeumaticoApi.desmontar` (queda `EN_ESPERA`, `enEspera=true`, sin slot) → `NeumaticoApi.montar(publicId, slotId)` (valida disponible, `MONTADO`, `enEspera=false`, `slotMontadoId`, sin centro y con stock del centro descontado vía puerto) → slot y `updated_*` de la unidad → publica `NeumaticoDesasignado` (si hubo) y `NeumaticoAsignado` (ids + snapshots de serie, patente, código, conductor, usuario y fecha).

**Apoyo al buscador** (`hasRole('LECTOR')`):
- `GET /api/v1/neumaticos`: `situacion` pasa a repetible (OR) y nuevo `centro` (UUID; inexistente → 200 vacío). `NeumaticoListadoDTO` suma `centroAlmacenamiento` (nombre, nullable), resuelto por lote por página.
- `GET /api/v1/centros-almacenamiento` → lista de activos `publicId`, `nombre`, `cantidadNeumaticosEnStock`, por nombre.
- `GET /api/v1/conductores` → lista de activos `publicId`, `nombre`, `apellido`, `dni`, por apellido.

**Frontend**:
- Ficha (`TransportUnitDetailDialog`), si `canPerform('transportUnits.assignTire')` (admin/editor): rueda vacía = botón "Asignar neumático en E1-IZQ" que abre `AssignTireDrawer`; el popover de una ocupada suma "Reemplazar neumático". Lector: sin cambios.
- `AssignTireDrawer`: **`drawer` no modal** (derecha en escritorio, abajo en mobile) para seguir viendo el chasis; la rueda destino se resalta (anillo + pulso `motion-safe`) vía render-prop de `AxleDiagram`. Contiene: código y tipo de eje (al reemplazar, aviso con la serie montada), el picker, `Combobox` "Conductor (opcional)" (busca apellido/DNI) y pie con resumen "Montar X en E2-IZQ-EXT de AB123CD" + "Confirmar". Ocupada → `AlertDialog` y envío con `reemplazar=true`. Éxito: actualiza la ficha, invalida `["tires"]`, `notify` y cierra; 409 → refetch; 404/422 → `notify`.
- `TirePicker` (feature `tires`; reutiliza `useTires`, la búsqueda por serie y `FilterSelect` de `TireFilters`): select "Origen" (todos los disponibles / en espera / en stock / un centro) y lista `RadioGroup` con serie, marca, estado y "En espera" o el centro; `TablePagination`. Lo compone `router.tsx` por el prop `renderTirePicker` de `TransportUnitsPage` (como `catalogs`), con el contrato en `lib/tire-picker.ts`.
- Se reutiliza lo instalado (`alert-dialog`, `popover`, `badge`, `select`, `input`, `empty`). **A instalar** (Base UI): `npx shadcn@latest add drawer combobox radio-group`.

## 2. Archivos

Backend (`.../gestion_neumaticos/`):
- `neumaticos/api/{NeumaticoApi,NeumaticoMontajeDTO,StockCentroAlmacenamiento}.java` — montar/desmontar; puerto de stock (id activo por publicId, nombres por lote, retirar uno).
- `neumaticos/{service,domain,controller}/**` — reglas de montaje, filtros y nombre del centro.
- `almacenamiento/service/StockCentroAlmacenamientoImpl.java` + listado de centros.
- `conductores/api/{package-info,ConductorApi,ConductorResumenDTO}.java`, `service/ConductorApiImpl.java` + listado de conductores.
- `unidades_transporte/**` — request DTO, montaje en slot, servicio y endpoint.
- `unidades_transporte/event/{package-info,NeumaticoAsignado,NeumaticoDesasignado}.java` — `@NamedInterface("event")`.
- `trazabilidad/domain/entity/{EventoAsignacion,EventoDesasignacion}.java`, `listener/MontajeNeumaticoListener.java`.
- `exception/{PosicionOcupadaException,ControllerAdvisor}.java` — 409.
- Tests: snippet REST Docs `unidad-transporte-asignar-neumatico`.

Frontend (`src/`): `components/ui/*` (CLI); `lib/{tire-picker,access}.ts`; `features/tires/{api/storage-centers-api.ts,api/tires-api.ts,hooks/use-storage-centers.ts,components/tire-picker.tsx}`; `features/transport-units/{api/drivers-api.ts,hooks/use-assign-tire.ts,hooks/use-drivers.ts,components/assign-tire-drawer.tsx}` + ficha, popover, tabla y página; `app/router.tsx`; i18n `{es,en}/{transport-units,tires}.json`.

Docs: `docs/Seguridad/seguridad.md` 2.10 (4 rutas).

## 3. Migración

`V20__asignacion_neumaticos.sql`: `GRANT UPDATE (neumatico_actual_id)` en slots + política `UPDATE` (admin/editor); `GRANT UPDATE (updated_at, updated_by)` en `unidades_transporte` + política firmada como `V19`; índices únicos parciales `slots(neumatico_actual_id)` y `neumaticos(slot_montado_id)` `WHERE NOT NULL`; en `eventos_neumatico` columnas `unidad_transporte_id`, `patente_unidad_transporte`, `slot_id`, `posicion_slot`, `conductor_id`, `nombre_conductor` (nullable) + `CHECK` (nulas fuera de `ASIGNACION`/`DESASIGNACION`; unidad y slot obligatorios en ellas).

## 4. Desvíos de convención

- Slot por `codigo` en la URL: no tiene `publicId` y es único en la unidad.
- Retirar del stock por puerto en `neumaticos.api` (implementa `almacenamiento`): `almacenamiento → neumaticos` ya existe; la inversa sería ciclo. Nuevas: `unidades_transporte → conductores.api`, `trazabilidad → unidades_transporte.event`.
- Los listados de centros y conductores no se paginan (como `GET /proveedores`): son catálogos chicos.
- `tipoActual`/`tipoOrigen` y kilometraje no cambian al montar (la HU no lo pide; decide el humano).

## 5. Alternativas descartadas

- **`POST /neumaticos/{id}/asignaciones`**: el slot es de `unidades_transporte`; la ruta elegida deja HU-32 simétrica.
- **Descontar stock por listener**: pierde la atomicidad de la transacción.
- **`Sheet` modal o página propia**: tapan el chasis.
