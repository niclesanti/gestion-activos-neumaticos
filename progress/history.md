# Bitácora histórica (append-only)

> Cada vez que se cierra una sesión, su resumen se añade aquí.
> No edites entradas anteriores. Solo añades al final.

---

## 2026-11-07 — Bootstrap del arnés SDD
- **Agente:** Claude Code (con el humano).
- **Cambios:** `AGENTS.md`, `CHECKPOINTS.md`, `init.sh`, `feature_list.json`, `docs/harness/`, `.claude/agents/`, `.claude/hooks/`, `.claude/settings.local.json`, sección de rol `leader` al inicio de `CLAUDE.md`.
- **Resultado:** arnés adaptado a Spring Boot + React/TS. `init.sh` replica los jobs de `.github/workflows/ci.yml`.

---

## 2026-11-09 — HU-27 Buscar unidades de transporte por patente (feature id 1, `done`)
- **Agentes:** leader, spec_author, implementer, reviewer (spec aprobado por el humano).
- **Cambios:** `GET /api/v1/unidades-transporte?patente=&page=&size=&sort=` (`hasRole('LECTOR')`, `UnidadTransporteListadoDTO` con `ConfiguracionUnidadListadoDTO`, solo activas, `@EntityGraph`, lista blanca de orden `patente`, sin migración). Paginación unificada 10 por defecto / 20 máximo (`PaginacionConfig`, también en los 7 `GET .../auditoria`). Frontend: `TransportUnitsTable` abierta al Lector; `TablePagination` y `SortableTableHead` extraídos a `components/common/` y usados por `features/audit`; `DEFAULT_PAGE_SIZE`. Docs: `CLAUDE.md` y `docs/Seguridad/seguridad.md` (2.10, 3.4, Pendiente).
- **Resultado:** `./init.sh` en `[OK]`; smoke de `curl` y de navegador en verde; reviewer aprobó (observaciones menores aplicadas: constantes de uso único inline, orden de imports).
- **Referencias:** `specs/buscar_unidades_transporte_por_patente/`, `progress/impl_buscar_unidades_transporte_por_patente.md`, `progress/smoke_buscar_unidades_transporte_por_patente.md`, `progress/review_buscar_unidades_transporte_por_patente.md`.

## 2026-11-12 — HU-10 Ficha detalle de una unidad de transporte (feature id 3, `done`)
- **Agentes:** leader, spec_author, implementer, reviewer (spec aprobado por el humano).
- **Cambios:** `GET /api/v1/unidades-transporte/{publicId}` (`hasRole('LECTOR')`, `UnidadTransporteDetalleDTO` con `SlotDetalleDTO` y neumático montado vía nuevo contrato `neumaticos.api` `NeumaticoApi`, sin N+1, 404 si no existe o está de baja, 400 por UUID inválido, sin migración). Frontend: `TransportUnitDetailDialog` (Collapsible de datos + `AxleDiagram` extraído de `AxleLayoutPreview`, slots ocupados rellenos / vacíos con el fondo, `SlotPopover` con serie, marca, estado y kilometraje), filas de la tabla clickeables, i18n es/en, componentes `collapsible` y `popover` de shadcn. Pensado para reutilizarse en HU-31.
- **Resultado:** `./verify.sh` en `[OK]`; prueba en navegador (temas claro/oscuro, es/en, responsive) en verde; reviewer aprobó.
- **Referencias:** `specs/ficha_unidad_transporte/`, `progress/impl_ficha_unidad_transporte.md`, `progress/review_ficha_unidad_transporte.md`.

## 2026-11-14 — Observabilidad: métricas y logs en local (sin HU en `feature_list.json`)
- **Agentes:** leader, implementer (plan aprobado por el humano; el documento de decisiones hizo de spec).
- **Cambios:** Actuator `health,prometheus` en puerto de gestión 8081 con `SecurityActuatorConfig`, logs ECS en archivo, Grafana Alloy + Prometheus + Loki + Grafana en `compose.yaml` bajo profiles `observabilidad` / `observabilidad-local` (Grafana en `127.0.0.1:3001`), dashboard 19004 provisionado, `infra/observabilidad/`, variables en `.env.example`. Docs actualizadas (decisiones-observabilidad, decisiones-despliegue, despliegue-seguro, CLAUDE.md).
- **Resultado:** `./verify.sh` en `[OK]` (tras `mvnw clean`: un `target/` compilado por un IDE con JDK 25 rompe surefire); en Chrome, el dashboard muestra datos reales y Loki los logins rechazados con etiquetas `service_name`, `environment` y `level`. Pendiente la parte de nube.
- **Referencias:** `progress/impl_observabilidad.md`, `docs/Obs&Tel/decisiones-observabilidad.md`.

## 2026-11-18 — HU-28 Buscar neumáticos por número de serie o atributos técnicos (feature id 2, `done`)
- **Agentes:** leader, spec_author (agente genérico con su prompt: el nombre `spec_author` no carga por el guion bajo), implementer, reviewer (spec aprobado por el humano).
- **Cambios:** `GET /api/v1/neumaticos?numeroSerie=&marcaId=&estado=&situacion=&page=&size=&sort=` (`hasRole('LECTOR')`, `NeumaticoListadoDTO`, solo activos, serie sin distinguir mayúsculas con comodines escapados, orden por `numeroSerie`, `@EntityGraph`, sin migración). Frontend: `/tires` con `TireFilters`, `TiresTable` y paginación; la casilla "Solo en espera" de la historia se reemplazó por un filtro de situación operativa (decisión del humano, anotada en `feature_list.json`). `ROUTE_ACCESS` sin cambios: `/tires` sigue cerrada al Lector (decisión pendiente).
- **Resultado:** backend `mvn clean verify` en `[OK]` (742 tests, cobertura OK); frontend lint, typecheck y build en `[OK]`; **`npm audit` en `[FAIL]`** por un aviso de seguridad en `picomatch` (cadena `vite`), ajeno a la HU y pendiente de resolver (la HU se cerró con esa excepción por decisión del humano); prueba en navegador en verde; reviewer aprobó.
- **Referencias:** `specs/buscar_neumaticos/`, `progress/spec_buscar_neumaticos.md`, `progress/impl_buscar_neumaticos.md`, `progress/review_buscar_neumaticos.md`.

## 2026-11-25 — HU-14 Consultar la ficha detalle de un neumático (feature id 4, `done`)
- **Agentes:** leader, spec-author (agente genérico con su prompt: no estaba disponible como tipo), implementer, reviewer (spec aprobado por el humano).
- **Cambios:** `GET /api/v1/neumaticos/{publicId}` (`hasRole('LECTOR')`, `NeumaticoDetalleDTO` con `UbicacionOperativaDTO`, sin ids internos, 404 solo por inexistente o baja lógica; un neumático de baja se devuelve). La ubicación (unidad + posición, centro, servicio) se resuelve sin ciclos con el puerto `neumaticos.api.ProveedorUbicacionNeumatico`, implementado por `unidades_transporte`, `almacenamiento` y `reparacion`. Frontend: `TireDetailDialog` abierto desde la fila o el número de serie (como la ficha de la unidad, HU-10), con "Ver historial" y "Ver ubicación" deshabilitados ("Próximamente"; el criterio de navegación queda parcial hasta HU-33/HU-34). `/tires` se abre al Lector en solo lectura (revierte lo decidido en HU-28): sin `[ROUTES.tires]` en `ROUTE_ACCESS`, acción `tires.create` (admin/editor) y "Registrar neumático" oculto.
- **Resultado:** backend `mvn clean verify` en `[OK]` (778 tests, cobertura OK, `ModularityTests` OK); `./verify.sh --frontend` en `[OK]` (lint, typecheck, build y `npm audit`, que ya no falla); prueba en navegador en verde (montado, en stock, en reparación, en espera, de baja, no registrado, ubicación no disponible, 404, Lector); reviewer aprobó (observaciones menores sin aplicar: variable local para el 404 en el dialog y renombrar `TireLocation`).
- **Referencias:** `specs/ficha_neumatico/`, `progress/spec_ficha_neumatico.md`, `progress/impl_ficha_neumatico.md`, `progress/review_ficha_neumatico.md`.

## 2026-11-25 — HU-31 Asignar un neumático a una posición de una unidad de transporte (feature id 5, `done`)
- **Agentes:** leader, spec-author (agente genérico con su prompt), implementer, reviewer (spec aprobado por el humano tras rediseñar la UI en 3 pasos).
- **Cambios:** `PUT /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico` (`EDITOR`; monta, reemplaza con `reemplazar=true`, descuenta stock del centro vía puerto `neumaticos.api.StockCentroAlmacenamiento`; eventos `unidades_transporte.event.NeumaticoAsignado/Desasignado` → `EventoAsignacion/Desasignacion` en trazabilidad); 409 `PosicionOcupadaException` / `NeumaticoNoDisponibleException`; `conductores.api` + `GET /conductores?q=` paginado; `GET /centros-almacenamiento` sin paginar; `GET /neumaticos` con `situacion` repetible y `centro`; migración `V20`. Frontend: `MountTireDrawer` no modal en 3 pasos (neumático → conductor opcional → confirmación inmutable con checkbox), `TirePicker` compuesto por `router.tsx` (`lib/tire-picker.ts`); shadcn `drawer`, `combobox`, `radio-group`, `checkbox`, `alert`.
- **Resultado:** `verify.sh` `[OK]`; navegador en verde (montaje desde stock, reemplazo, Lector), sin capturas: no se vio el pulso del slot ni mobile; reviewer aprobó con observaciones menores (`Clock` en vez de `Instant.now()`, stock 0 inconsistente → 422).
- **Referencias:** `specs/asignar_neumatico_a_posicion/`, `progress/impl_asignar_neumatico_a_posicion.md`, `progress/review_asignar_neumatico_a_posicion.md`.
- **2.º ciclo (2026-11-26, cambios de UI pedidos por el humano):** cuerpo del drawer en `ScrollArea`; `TirePicker` con `Tabs` "En espera" (por defecto) / "En stock" + select de centro, tamaño de página del backend; conductor con búsqueda + lista paginada (se quitaron `combobox`/`input-group`); `ToastViewport` `z-100` (toasts sobre dialogs y drawer en toda la app). `verify.sh` `[OK]`, navegador en verde, reviewer aprobó. Pendiente anotado: búsqueda de conductores sensible a acentos (`unaccent`).
