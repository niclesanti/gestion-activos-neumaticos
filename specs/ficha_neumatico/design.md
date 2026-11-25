# Design — HU-14 ficha_neumatico

## 1. Contrato

**`GET /api/v1/neumaticos/{publicId}`** — `hasRole('LECTOR')` (los tres roles).
- 200 `NeumaticoDetalleDTO`: `publicId`, `numeroSerie`, `marca` (nombre), `estadoActual`, `situacionOperativaVigente`, `kilometrajeTotal`, `kilometrajeUltimaReparacion`, `tipoOrigen`, `tipoActual`, `cantidadRecapados`, `comentario` (nulables salvo enums y marca) y `ubicacion` (`UbicacionOperativaDTO`: `unidad`, `posicion`, `centroAlmacenamiento`, `servicioReparacion`, todos nulables). Sin ids internos.
- `ubicacion` solo se completa para `MONTADO` (unidad + posición), `EN_STOCK` (centro) y `EN_REPARACION` (servicio); en `EN_ESPERA` y `DE_BAJA` va vacía sin consultar a otros módulos. Si la situación lo pide pero el dato no se resuelve (id nulo o inexistente), queda vacía y la UI muestra "Ubicación no disponible".
- Errores: 404 `EntidadNoEncontradaException` (inexistente o `deletedAt` no nulo; un neumático de baja *operativa* sí se devuelve), 400 por `publicId` no UUID (ya resuelto en `ControllerAdvisor`), 401.
- Sin N+1: neumático con marca por entity graph + una sola consulta al módulo de la situación vigente.
- Las rutas literales (`/marcas`, `/auditoria`) tienen precedencia sobre `/{publicId}`.

**Resolución de ubicación sin ciclos:** `neumaticos.api` define el puerto `ProveedorUbicacionNeumatico` (`tipo()` ∈ `UNIDAD`, `CENTRO_ALMACENAMIENTO`, `SERVICIO_REPARACION`; `buscar(idInterno)` → `Optional<UbicacionOperativaDTO>`). Lo implementan, en su `service/`, `unidades_transporte` (slot → patente + código), `almacenamiento` y `reparacion`. `neumaticos` recibe la lista de proveedores por inyección y elige por situación: no importa ningún módulo de ubicación. Mismo patrón que `security.NivelAccesoVigente` ← `usuarios`.

**Apertura al Lector (decisión de la humana):** `ROUTE_ACCESS` sin `[ROUTES.tires]`; nueva acción `tires.create` (admin/editor) en `ACTION_ACCESS`; `tires-page` oculta "Registrar neumático" con `canPerform`. Lo que consume la página para el Lector ya admite el rol: listado y `GET /marcas` (`hasRole('LECTOR')`) y `SELECT` en base (V10). El alta (`useSuppliers`, etc.) vive dentro del diálogo oculto: no se monta para el Lector.

**Frontend** (feature `tires`):
- Cada fila de `TiresTable` es clicable y el número de serie (o "Sin número de serie") es un botón accesible que abre `TireDetailDialog` (controlado por el `publicId` seleccionado; query solo con el dialog abierto). Estados: Skeleton, `Empty` con reintento ante error, mensaje propio ante 404.
- Dialog: título = serie; `Badge` de estado y de situación. Bloque "Datos técnicos" en `Collapsible` abierto (marca, kilometraje, km desde la última reparación, tipo de eje de origen y actual, recapados, comentario) y bloque "Ubicación operativa" con el texto según situación. Pie: "Ver historial" y "Ver ubicación" `disabled` + `aria-disabled` dentro de `Tooltip` "Próximamente".
- UI instalada: `dialog`, `badge`, `skeleton`, `empty`, `separator`, `button`, `collapsible`, `tooltip`. Nada para instalar.

## 2. Archivos

Backend (`.../gestion_neumaticos/`):
- `neumaticos/api/{ProveedorUbicacionNeumatico,UbicacionOperativaDTO,TipoUbicacion}.java` — puerto y contrato.
- `neumaticos/domain/dto/NeumaticoDetalleDTO.java` — respuesta.
- `neumaticos/repository/NeumaticoRepository.java` — activo por `publicId` con marca.
- `neumaticos/mapper/NeumaticoMapper.java` — `toDetalleDTO`.
- `neumaticos/service/NeumaticoService(Impl).java` — `obtenerDetalle(publicId)`, `readOnly`.
- `neumaticos/controller/NeumaticoController.java` — `GET /{publicId}`.
- `unidades_transporte/repository/*` + `unidades_transporte/service/UbicacionUnidadProveedor.java` — slot por id con su unidad.
- `almacenamiento/service/UbicacionCentroProveedor.java`, `reparacion/service/UbicacionServicioProveedor.java`.
- Tests: fixtures en `TestDataFactory`; snippet REST Docs `neumatico-detalle`.

Frontend (`frontend/gestion-neumaticos/src/`):
- `features/tires/api/tires-api.ts` — tipos `TireDetail`, `TireLocation` y `getTire`.
- `features/tires/hooks/use-tire.ts` — query `["tires","detail",publicId]`.
- `features/tires/components/tire-detail-dialog.tsx` — ficha.
- `features/tires/components/tires-table.tsx` — fila/serie abren el dialog.
- `features/tires/pages/tires-page.tsx` — `canPerform("tires.create")`; quitar el comentario sobre el lector.
- `lib/access.ts` — ruta abierta, acción `tires.create`, comentario de la matriz.
- `lib/i18n/locales/{es,en}/tires.json` — `detail.*`.

Docs: `docs/Seguridad/seguridad.md` (2.10 fila del `GET` y nota de HU-28, 3.4, Pendiente 2), `docs/estado-actual.md`, `CLAUDE.md` (donde diga que `/tires` es solo admin/editor).

## 3. Migración

Ninguna: `V10` da `SELECT` a los tres roles en `neumaticos.*`; `V16`, `V17` y `V18` en slots/unidades, servicios y centros (verificar en la integración como lector).

## 4. Desvíos de convención

- Puerto en `neumaticos.api` implementado por otros módulos: mismo patrón que `security`, pero con tres implementaciones; evita el ciclo con `unidades_transporte` (que ya usa `NeumaticoApi`). `ModularityTests` debe seguir verde.
- Las implementaciones reciben ids **internos** (los que guarda `Neumatico`, sin FK); no salen al cliente.
- Una unidad, centro o servicio dado de baja igual se nombra: la ficha refleja dónde está el neumático.
- Hasta HU-31 nada asigna `slot_montado_id`, `centro_almacenamiento_id` ni `servicio_reparacion_id`: los tres casos se prueban con integración (datos insertados como owner) y en navegador asignando por SQL en dev, documentado en `impl_`.

## 5. Alternativas descartadas

- **`neumaticos` consulta a `unidades_transporte`/`almacenamiento`/`reparacion`**: crea ciclo con `unidades_transporte`.
- **Eventos / copia de la ubicación en `neumaticos`**: duplica datos y exige migración y sincronización.
- **Página `/tires/:publicId`**: la humana pidió Dialog, como HU-10.
