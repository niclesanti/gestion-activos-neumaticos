# Implementación HU-14 — ficha_neumatico

## Resumen

Backend: `GET /api/v1/neumaticos/{publicId}` (`hasRole('LECTOR')`) devuelve `NeumaticoDetalleDTO` (datos técnicos + `ubicacion`, sin ids internos). 404 si no existe o tiene `deletedAt`; un neumático con `estadoActual` o situación `DE_BAJA` se devuelve con ubicación vacía. El neumático se carga con marca por entity graph (`findByPublicIdAndDeletedAtIsNull`) y la ubicación con una sola consulta al módulo que corresponde a la situación (`MONTADO` → unidad + posición, `EN_STOCK` → centro, `EN_REPARACION` → servicio; `EN_ESPERA` / `DE_BAJA` no consultan a nadie).

Sin ciclos entre módulos: el puerto `neumaticos.api.ProveedorUbicacionNeumatico` (+ `UbicacionOperativaDTO`, `TipoUbicacion`) lo implementan `unidades_transporte` (`UbicacionUnidadProveedor` + `SlotUnidadTransporteRepository.findConUnidadById`), `almacenamiento` (`UbicacionCentroProveedor`) y `reparacion` (`UbicacionServicioProveedor`); `NeumaticoServiceImpl` recibe la lista por inyección. `ModularityTests` sigue en verde. Si la referencia es nula o ya no existe, la ubicación queda vacía (la UI muestra "Ubicación no disponible"); unidad/centro/servicio dados de baja igual se nombran. Sin migración.

Frontend (`features/tires`): `getTire` + tipos `TireDetail`/`TireLocation`, hook `useTire` (habilitado solo con el dialog abierto, sin reintentos), `TireDetailDialog` (título = serie o "Sin número de serie", badges de estado y situación, `Collapsible` "Datos técnicos" abierto, bloque "Ubicación operativa", pie con "Ver historial" y "Ver ubicación" deshabilitados + `aria-disabled` dentro de `Tooltip` "Próximamente", Skeleton / `Empty` con reintento / mensaje propio de 404). `TiresTable`: fila clicable + botón accesible con `aria-label`. `/tires` abierta al Lector (sin `[ROUTES.tires]` en `ROUTE_ACCESS`), acción `tires.create` (admin/editor) y "Registrar neumático" oculto con `canPerform`. i18n es/en (`list.openDetail`, `detail.*`). Docs: `docs/Seguridad/seguridad.md` (2.10, 3.4, Pendiente 2), `docs/estado-actual.md` y `CLAUDE.md`.

Verificación puntual: backend `mvn test` de `NeumaticoServiceImplTest`, `NeumaticoMapperTest`, `NeumaticoControllerTest`, `Ubicacion*ProveedorTest`, `DetalleNeumaticoIntegrationTests`, `NeumaticoApiImplTest`, `BusquedaNeumaticoIntegrationTests`, `RegistroNeumaticoIntegrationTests`, `AutorizacionBaseDatosTests` y `ModularityTests`: todo verde. Frontend: `npm run lint` y `npm run typecheck` limpios. No corrí `verify.sh` ni `mvn verify`.

## Archivos

Backend `main` (`.../gestion_neumaticos/`):
- nuevos: `neumaticos/api/{ProveedorUbicacionNeumatico,UbicacionOperativaDTO,TipoUbicacion}.java`, `neumaticos/domain/dto/NeumaticoDetalleDTO.java`, `unidades_transporte/repository/SlotUnidadTransporteRepository.java`, `unidades_transporte/service/UbicacionUnidadProveedor.java`, `almacenamiento/service/UbicacionCentroProveedor.java`, `reparacion/service/UbicacionServicioProveedor.java`.
- modificados: `NeumaticoRepository`, `NeumaticoMapper` (`toDetalleDTO`), `NeumaticoService(Impl)` (`obtenerDetalle`), `NeumaticoController` (`obtenerDetalle`).

Backend `test`: `TestDataFactory` (`SLOT_ID`, `CENTRO_ID`, `SERVICIO_ID`, `neumaticoEn`, `neumaticoDetalle`), `NeumaticoServiceImplTest.ObtenerDetalle`, `NeumaticoMapperTest`, `NeumaticoControllerTest.Detalle` (REST Docs `neumatico-detalle`), `Ubicacion{Unidad,Centro,Servicio}ProveedorTest`, `neumaticos/DetalleNeumaticoIntegrationTests`.

Frontend (`src/`): `features/tires/{api/tires-api.ts,hooks/use-tire.ts,components/tire-detail-dialog.tsx,components/tires-table.tsx,pages/tires-page.tsx}`, `lib/access.ts`, `lib/i18n/locales/{es,en}/tires.json`. Sin componentes shadcn nuevos (ya estaban `dialog`, `badge`, `skeleton`, `empty`, `separator`, `collapsible`, `tooltip`).

## Trazabilidad

- CA1 (datos y situación): `NeumaticoServiceImplTest.ObtenerDetalle.montado|enStock|enReparacion`, `NeumaticoMapperTest.toDetalleDTOCopia...`, `NeumaticoControllerTest.Detalle.detalleConLector` (+ REST Docs), `DetalleNeumaticoIntegrationTests.montado`; UI: verificación en navegador (abajo).
- CA2 (montado / stock / reparación): `ObtenerDetalle.montado|enStock|enReparacion`, `Ubicacion{Unidad,Centro,Servicio}ProveedorTest`, `DetalleNeumaticoIntegrationTests.montado|montadoEnUnidadDeBaja|enStock|enReparacion` (como Lector, bajo RLS); UI: navegador.
- CA3 (en espera): `ObtenerDetalle.sinUbicacion[EN_ESPERA]`, `DetalleNeumaticoIntegrationTests.enEspera`; UI: texto "En espera".
- CA4 (de baja): `ObtenerDetalle.sinUbicacion[DE_BAJA]|estadoDeBaja`, `DetalleNeumaticoIntegrationTests.deBaja`; UI: "De baja" y sin unidad/centro/servicio.
- CA5 (acciones "Próximamente"): solo UI (botones `disabled` + `aria-disabled` + tooltip); navegador.
- CA6 (datos no registrados): `DetalleNeumaticoIntegrationTests.datosNoRegistrados`, `NeumaticoMapperTest.toDetalleDTOConDatosNulos`; UI: "No registrado" / "Sin número de serie".
- CA7 (200, sin ids internos, tres roles): `NeumaticoControllerTest.Detalle.detalleConLector|detalleConEditorYAdministrador`, `DetalleNeumaticoIntegrationTests.*` (lector).
- CA8 (404 / 400 / 401): `ObtenerDetalle.inexistente`, `DetalleNeumaticoIntegrationTests.bajaLogicaOInexistente`, `Detalle.inexistente|identificadorInvalido|sinToken`.
- CA9 (Lector): verificación de UI (navegador). La parte de backend: `Detalle.detalleConLector` y las integraciones corren como lector.
- Sin N+1: `DetalleNeumaticoIntegrationTests.sinNMasUno` (<= 4 sentencias). Referencia nula o inexistente: `ObtenerDetalle.referenciaNula|referenciaInexistente`, `DetalleNeumaticoIntegrationTests.referenciaInexistente`.

## Pasos para el navegador

Usuarios de seed: `victoria.editor` / `Editor.1234` y `victoria.lector` / `Lector.1234`. Ruta: `/tires`.

### Preparar datos (SQL contra la base de desarrollo en Docker)

Hoy nada asigna `slot_montado_id`, `centro_almacenamiento_id` ni `servicio_reparacion_id`: se asignan por SQL. Requisitos: tener cargados al menos 5 neumáticos (por "Registrar neumático", números de serie `T-MONT`, `T-STOCK`, `T-REP`, `T-ESPERA`, `T-BAJA`), una unidad de transporte (p. ej. patente `AB123CD`), un centro de almacenamiento y un servicio de reparación (desde Configuración). Abrir psql como superusuario del contenedor (no está sujeto a RLS):

```
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

(si `POSTGRES_USER`/`POSTGRES_DB` no existieran en el contenedor, usar los valores `DB_USER`/`DB_NAME` del `.env`).

```sql
-- Montado: slot E1-IZQ (primer slot) de la unidad AB123CD
UPDATE neumaticos.neumaticos
SET situacion_operativa_vigente = 'MONTADO', en_espera = false,
    slot_montado_id = (SELECT s.id FROM unidades_transporte.slots_unidad_transporte s
                       JOIN unidades_transporte.unidades_transporte u ON u.id = s.unidad_id
                       WHERE u.patente = 'AB123CD' ORDER BY s.id LIMIT 1)
WHERE numero_serie = 'T-MONT';

-- En stock: primer centro de almacenamiento
UPDATE neumaticos.neumaticos
SET situacion_operativa_vigente = 'EN_STOCK', en_espera = false,
    centro_almacenamiento_id = (SELECT id FROM almacenamiento.centros_almacenamiento ORDER BY id LIMIT 1)
WHERE numero_serie = 'T-STOCK';

-- En reparación: primer servicio de reparación
UPDATE neumaticos.neumaticos
SET situacion_operativa_vigente = 'EN_REPARACION', en_espera = false,
    servicio_reparacion_id = (SELECT id FROM reparacion.servicios_reparacion ORDER BY id LIMIT 1)
WHERE numero_serie = 'T-REP';

-- De baja (la ficha igual se ve; el listado de HU-28 lo oculta, por lo que se abre desde la tabla ANTES de este UPDATE
-- o con el dialog ya abierto y "Reintentar"; también se puede probar por la API)
UPDATE neumaticos.neumaticos
SET situacion_operativa_vigente = 'DE_BAJA', en_espera = false
WHERE numero_serie = 'T-BAJA';
```

`T-ESPERA` se deja como está (en espera). Para el caso "ubicación no disponible": `UPDATE neumaticos.neumaticos SET situacion_operativa_vigente='EN_STOCK', centro_almacenamiento_id=NULL WHERE numero_serie='T-ESPERA';`.

Limpieza (volver todo a "en espera" y sin referencias):

```sql
UPDATE neumaticos.neumaticos
SET situacion_operativa_vigente = 'EN_ESPERA', en_espera = true,
    slot_montado_id = NULL, centro_almacenamiento_id = NULL, servicio_reparacion_id = NULL
WHERE numero_serie IN ('T-MONT', 'T-STOCK', 'T-REP', 'T-ESPERA', 'T-BAJA');
```

Para el 404 por baja lógica: `UPDATE neumaticos.neumaticos SET deleted_at = now(), deleted_by = created_by WHERE numero_serie = 'T-ESPERA';` con el dialog abierto y "Reintentar" o recargando la tabla (se revierte con `deleted_at = NULL, deleted_by = NULL`).

### Qué comprobar (como editor y como lector)

- CA1: clic en una fila, o Tab hasta el número de serie + Enter, abre el dialog con título = serie, badges de estado y situación, bloque "Datos técnicos" (colapsable, abierto: serie, marca, kilometraje, km desde la última reparación, eje de origen y actual, recapados, comentarios) y bloque "Ubicación operativa".
- CA2: `T-MONT` muestra Unidad `AB123CD` y Posición `E1-IZQ`; `T-STOCK` el nombre del centro; `T-REP` el nombre del servicio.
- CA3: `T-ESPERA` muestra "En espera" sin unidad/centro/servicio.
- CA4: `T-BAJA` muestra badge "De baja" y texto "De baja" en la ubicación, sin unidad/centro/servicio (también un neumático con estado técnico "De baja").
- CA5: "Ver historial" y "Ver ubicación" están deshabilitados; hover o Tab sobre ellos muestra el tooltip "Próximamente".
- CA6: un neumático sin serie, kilometraje, recapados o comentario muestra "No registrado" / "Sin número de serie".
- CA9 (lector): con `victoria.lector` aparece "Neumáticos" en la navegación, entra a `/tires`, abre la ficha y NO ve el botón "Registrar neumático". Con editor/administrador el botón sigue visible. Quien no debe ver la acción de alta: Lector.
- Estados: con la red cortada (o backend caído) el dialog muestra el error con "Reintentar"; Skeleton mientras carga; baja lógica → "El neumático ya no existe".
- Regresión: filtros, orden y paginación de la tabla siguen igual; el alta de neumáticos invalida la lista.
- Idioma es/en y tema claro/oscuro.

## Dudas o desvíos

- Desvío menor respecto de `design.md` (sin cambiar el contrato): un neumático con `estadoActual = DE_BAJA` pero situación distinta (p. ej. `MONTADO`) se trata como de baja y no consulta ubicación, para cumplir el CA4 literal ("estadoActual o situación").
- El tooltip de las acciones deshabilitadas cuelga de un `span` enfocable (`TooltipTrigger render`), porque un `button` deshabilitado no recibe hover ni foco; no se verificó visualmente (sin navegador en este entorno).
- No toqué `feature_list.json`, `.claude/` ni nada de `specs/` salvo marcar `[x]` las tasks en `specs/ficha_neumatico/tasks.md`. `npm audit` sigue rojo por `picomatch` (ajeno a esta HU).

## Bloqueos

Ninguno. Los servidores MCP no conectaron: exploré con Glob/Grep/Read.

## Verificación del leader (2026-11-25)

- **Backend:** `mvn -B clean verify` con JDK 21 (equivale a `./mvnw -B verify`; el wrapper no descarga Maven en esta máquina): `[OK]`, 778 tests, 0 fallas, cobertura JaCoCo cumplida, `ModularityTests` en verde.
- **Frontend (`./verify.sh --frontend`):** `[OK]` en coherencia de `feature_list.json`/specs, lint, typecheck, build y `npm audit` (el aviso de `picomatch` ya no aparece). Verificación verde.

## Prueba en el stack real

Stack reconstruido con `docker compose up -d --build`; Chrome (puppeteer-core) contra `http://localhost:3000`. Datos de prueba creados por API (unidad `HQ141AA`, servicio "Gomería HU14 prueba", centro "Depósito HU14 prueba" y neumáticos `T14-*`) con las ubicaciones asignadas por SQL como indica la sección anterior; **todo borrado al terminar** (la base quedó con 0 neumáticos, como antes).

| Caso | Resultado |
|---|---|
| Montado (`T14-MONT`) | Unidad `HQ141AA`, Posición `E1-IZQ` (CA2) |
| En stock (`T14-STOCK`) | Centro de almacenamiento "Depósito HU14 prueba" (CA2) |
| En reparación (`T14-REP`) | Servicio de reparación "Gomería HU14 prueba" (CA2) |
| En espera con todos los datos (`T14-ESPERA`) | "En espera" sin unidad/centro/servicio; km `120.500`, `20.500`, eje Tracción/Dirección, 2 recapados, comentario (CA1, CA3) |
| Situación `DE_BAJA` | Badge y ubicación "De baja", datos técnicos visibles (CA4) |
| Estado técnico `DE_BAJA` con situación en espera | Badge "De baja", ubicación "De baja" (CA4) |
| Sin serie / km / recapados / comentario | Título "Sin número de serie"; "No registrado" en cada dato (CA6) |
| `EN_STOCK` sin centro / `MONTADO` con slot inexistente | "Ubicación no disponible", sin error |
| Baja lógica (`deleted_at`) en un neumático nunca abierto | "El neumático ya no existe" (CA8, 404) |
| Acciones | "Ver historial" y "Ver ubicación" con `disabled` y `aria-disabled="true"`; el tooltip dice "Próximamente" (CA5) |
| Apertura | clic en la celda de la fila, clic en el botón y teclado (foco + Enter) abren la ficha |
| Lector | navegación: Inicio, Unidades de transporte, Neumáticos y Configuración; entra a `/tires`, abre la ficha y **no** ve "Registrar neumático"; el editor sí lo ve (CA9) |
| Consola del navegador | sin errores |

Observación: el detalle usa la caché de 30 s del `QueryClient` (igual que la ficha de la unidad, HU-10): reabrir la misma ficha dentro de ese plazo muestra el dato ya cargado, y el alta invalida `["tires"]`. No es un defecto; las pruebas de baja lógica y de ubicación se hicieron sobre filas que no se habían abierto antes.
