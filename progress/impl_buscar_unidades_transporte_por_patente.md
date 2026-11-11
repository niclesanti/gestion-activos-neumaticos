# Implementación — HU-27 Buscar unidades de transporte por patente

Feature id 1 `buscar_unidades_transporte_por_patente`. Tasks T1 a T28 de `specs/buscar_unidades_transporte_por_patente/tasks.md` (T1-T26 hechas; T27 smoke con `curl` en `progress/smoke_buscar_unidades_transporte_por_patente.md`; T28 son los pasos de abajo, que corre el leader).

## Resumen de lo implementado

Backend
- `config/PaginacionConfig`: constantes `TAMANIO_PAGINA_POR_DEFECTO = 10` y `TAMANIO_PAGINA_MAXIMO = 20`; máximo y pageable de reserva desde ellas. Los 7 `GET .../auditoria` usan la constante en su `@PageableDefault`.
- `unidades_transporte`: `EspecificacionesUnidadTransporte` (`activas`, `patenteContiene` con escape de comodines), `OrdenUnidadesTransporte` (lista blanca `patente`), `UnidadTransporteListadoDTO` + `ConfiguracionUnidadListadoDTO`, `toListadoDTO` / `toConfiguracionListadoDTO` en el mapper, `findAll(Specification, Pageable)` con `@EntityGraph("configuracion")`, `UnidadTransporteService.buscar` (+ impl, `readOnly`, `warn` en orden rechazado) y `GET /api/v1/unidades-transporte` (`hasRole('LECTOR')`). Sin migración (T4 confirmado: `V16` da `SELECT` + `USING (true)` a los tres roles).
- `TestDataFactory.paginaAuditoria()` pasa de `PageRequest.of(0, 20)` a `(0, 10)` (solo es el eco del mock).

Frontend
- `lib/api/page.ts`: `DEFAULT_PAGE_SIZE = 10`. Nuevos `components/common/table-pagination.tsx` y `sortable-table-head.tsx`; `features/audit/components/audit-records-table.tsx` refactorizada para usarlos (sin cambio de comportamiento); `pagination.previous/next` pasaron de `audit.json` a `common.json`.
- `features/transport-units`: `searchTransportUnits` / `TransportUnitsQuery` en la api, `useTransportUnits`, `TransportUnitsTable` (búsqueda con debounce, orden por patente, skeleton, error con reintento, vacíos con y sin término, paginación), página reemplaza el placeholder, i18n `list.*` / `unitTypes.*` (es/en), `ROUTE_ACCESS` ya no restringe `/transport-units` (el Lector entra, sin botón de alta).
- Docs: `docs/Seguridad/seguridad.md` (2.10, auditoría, 3.4, Pendiente 2) y `CLAUDE.md`.

## Resultado de `./init.sh`

`./init.sh` en `[OK]` (backend `./mvnw verify` con JaCoCo, y frontend lint, typecheck, build y audit), sin reintentos. Smoke con `curl` en `progress/smoke_buscar_unidades_transporte_por_patente.md` (todo según lo esperado).

## Trazabilidad CA -> verificación

| CA | Verificación |
|---|---|
| CA1 | `BusquedaUnidadTransporteIntegrationTests`: `parcialSinDistinguirMayusculas`, `ignoraEspaciosYGuiones`, `comodinesSeBuscanLiterales`, `devuelveLaConfiguracionResumida`; `UnidadTransporteControllerTest.Buscar.buscaConLector`; `UnidadTransporteServiceImplTest.Buscar.buscaConElPageableRecibido`, `mapeaLaPaginaConToListadoDTO`; `UnidadTransporteMapperTest.toListadoDTO_*` |
| CA2 | `BusquedaUnidadTransporteIntegrationTests`: `excluyeLasDadasDeBaja`, `sinTermino_devuelveTodasLasActivas` |
| CA3 | integración `sinTermino_devuelveTodasLasActivas`; servicio `terminoEnBlanco_noFiltraPorPatente`; controlador `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto` |
| CA4 | integración `sinCoincidencias_devuelvePaginaVacia`; controlador `sinResultados_devuelvePaginaVacia` |
| CA5 | integración `pagina`, `cargaLaPaginaSinNMasUno`; controlador `buscaConLector`, `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto`; servicio `mapeaLaPaginaConToListadoDTO` |
| CA6 | controlador `tamanioMayorAlMaximo_seAcota` |
| CA7 | `OrdenUnidadesTransporteTest` (4 casos); servicio `ordenNoPermitido_lanzaParametroInvalido`; controlador `ordenNoPermitido` |
| CA8 | controlador `buscaConLector`, `buscaConEditorYAdministrador`; integración `lectorLeeConSuRolDeBase` (rol `gn_lector` real) |
| CA9 | controlador `sinToken` |
| CA10 a CA15, CA17 | lint + typecheck + build + smoke del navegador (pasos abajo) |
| CA16 | `AuditoriaControllersTest`: `usaLosValoresPorDefecto` (10), `acotaElTamanioDePagina` (20), `enlazaParametros` (15); smoke con `curl` |

## Pasos del smoke test en el navegador (para el leader, `http://localhost:3000`)

Antes: `docker compose up -d --build` (backend y frontend nuevos). Usuarios del seed dev: `victoria.administrador` / `Admin.1234`, `victoria.editor` / `Editor.1234`, `victoria.lector` / `Lector.1234`. En cada paso revisar consola y requests (pestaña Network).

1. Como `victoria.editor`: en `/transport-units`, registrar con el diálogo `AB123CD` (Tractor común), `AB456EF` (Semirremolque) y `XYZ789`. Tras cada alta la tabla se actualiza sola, sin recargar. (CA15)
2. Con el campo vacío: la request sale como `GET /api/v1/unidades-transporte?page=0&size=10&sort=patente,asc` (sin `patente`) y la tabla muestra todas ordenadas por patente. (CA11)
3. Escribir `b12`: tras el debounce sale una sola request con `patente=b12&page=0`; la tabla muestra `AB123CD` con su configuración y su tipo (Tractor); hacer clic en la fila no hace nada. (CA10)
4. Escribir `qqq`: aparece "No se encontraron unidades de transporte" con "Revisá la patente o probá con menos caracteres." Al borrar el texto vuelve el listado completo. (CA12, CA11)
5. Con más de 10 unidades activas (registrar las que falten por el diálogo o por `curl`): aparece "N unidades · página 1 de 2"; Anterior deshabilitado y Siguiente pide `page=1`; escribir un término vuelve a `page=0`; alternar el orden de la columna Patente pide `sort=patente,desc`. (CA13)
6. Cerrar sesión e ingresar como `victoria.lector`: el ítem "Unidades de transporte" aparece en la navegación (sidebar y menú mobile), la página lista y busca, y no hay botón "Registrar unidad". (CA14)
7. Regresión de auditoría, como `victoria.administrador` en `/audit`, pestaña Unidades de transporte y otra (p. ej. Proveedores). (CA17)
   - La request lleva `size=10`.
   - Con más de 10 registros, el resumen "N registros · página X de Y" y Anterior / Siguiente funcionan como antes (los textos Anterior / Siguiente ahora vienen de `common`).
   - Los encabezados Creado / Última modificación alternan el orden (`sort=createdAt,desc|asc`) y muestran su ícono.
   - La búsqueda por texto filtra y vuelve a la página 0.
   - Abrir un registro muestra su ficha.
