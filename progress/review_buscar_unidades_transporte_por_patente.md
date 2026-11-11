# Review — feature 1 `buscar_unidades_transporte_por_patente` (HU-27)

**Veredicto:** APPROVED

`./init.sh` ejecutado por el revisor: exit 0 (backend `./mvnw verify` con JaCoCo, y frontend lint, typecheck, build y audit con 0 vulnerabilidades).

## Trazabilidad criterios de aceptación ↔ tests
- CA1: [x] `BusquedaUnidadTransporteIntegrationTests.parcialSinDistinguirMayusculas|ignoraEspaciosYGuiones|comodinesSeBuscanLiterales|devuelveLaConfiguracionResumida`; `UnidadTransporteControllerTest.Buscar.buscaConLector`; `UnidadTransporteServiceImplTest.Buscar`; `UnidadTransporteMapperTest.toListadoDTO_*`
- CA2: [x] `excluyeLasDadasDeBaja`, `sinTermino_devuelveTodasLasActivas`
- CA3: [x] `sinTermino_devuelveTodasLasActivas`, `terminoEnBlanco_noFiltraPorPatente`, `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto`
- CA4: [x] `sinCoincidencias_devuelvePaginaVacia` (integración), `sinResultados_devuelvePaginaVacia` (controlador)
- CA5: [x] `pagina`, `cargaLaPaginaSinNMasUno` (<= 2 sentencias, sin N+1), `buscaConLector`
- CA6: [x] `Buscar.tamanioMayorAlMaximo_seAcota`
- CA7: [x] `OrdenUnidadesTransporteTest`, `ordenNoPermitido_lanzaParametroInvalido`, `Buscar.ordenNoPermitido` (400)
- CA8: [x] `buscaConLector`, `buscaConEditorYAdministrador` (parametrizado), `lectorLeeConSuRolDeBase` (rol `gn_lector` real)
- CA9: [x] `Buscar.sinToken`
- CA10-CA15, CA17 (UI sin runner): [x] lint + typecheck + build (init.sh verde) y smoke de navegador documentado en `progress/smoke_buscar_unidades_transporte_por_patente.md` (pasos 1-7 con requests de red verificadas, sin errores de consola)
- CA16: [x] `AuditoriaControllersTest.usaLosValoresPorDefecto` (10), `acotaElTamanioDePagina` (20), `enlazaParametros` (15); curl en el smoke

## Tasks completas
- T1-T28: [x] (0 casillas `[ ]` en `tasks.md`; el código coincide con lo descrito: `PaginacionConfig` con constantes, 7 controladores de auditoría con `TAMANIO_PAGINA_POR_DEFECTO`, `EspecificacionesUnidadTransporte`, `OrdenUnidadesTransporte`, DTOs, `@EntityGraph`, servicio, controlador, componentes comunes)

## Puntos verificados
- DTOs `UnidadTransporteListadoDTO` / `ConfiguracionUnidadListadoDTO`: sin slots; no hay `@BatchSize` en el módulo (grep vacío); configuración cargada con `@EntityGraph` en `UnidadTransporteRepository.findAll(Specification, Pageable)`.
- `UnidadTransporteController.buscar`: `@PreAuthorize("hasRole('LECTOR')")`, ruta `/api/v1/unidades-transporte`, `@Operation`/`@ApiResponses`.
- Whitelist de sort `patente` -> `ParametroInvalidoException` (400) con `warn` antes de lanzar y sin tocar el repositorio.
- Servicio: `debug` de entrada y total, `warn` en rechazo, `readOnly`, logging con `{}`; es consulta, sin `info`.
- Paginación 10/20 en `PaginacionConfig` y `DEFAULT_PAGE_SIZE` en `lib/api/page.ts`; CLAUDE.md ya no dice 100 (grep 0), `seguridad.md` línea 354 dice 10 por defecto y tope 20.
- Componentes compartidos `components/common/table-pagination.tsx` y `sortable-table-head.tsx` usados por auditoría y transport-units; `SortableHead` y el pie inline de auditoría eliminados.
- i18n es/en: mismas claves en `transport-units.json`; `pagination.*` movido a `common`.
- Docs: tabla 2.10 de `docs/Seguridad/seguridad.md` (línea 174), 3.4 y Pendiente; "Current state" y "Authentication (frontend)" de CLAUDE.md actualizados.
- Sin migración nueva (no hay tabla nueva); sin dependencias nuevas.

## Checkpoints
- C1: [x]
- C2: [x]  (`progress/current.md` es del leader)
- C3: [x]
- C4: [x]
- C4b: [x]
- C5: [ ]  pendiente del leader al cerrar: entrada en `progress/history.md`, pasar la feature a `done`, commit (fuera del alcance del revisor)
- C6: [x]

## Observaciones menores (no bloquean)
1. `frontend/.../transport-units/components/transport-units-table.tsx:35` `const SKELETON_ROWS = 5` se usa una sola vez (regla de constantes de CLAUDE.md: uso único = local). Mismo patrón preexistente en `audit-records-table.tsx:47`; conviene bajarla a local en ambos en una limpieza posterior.
2. `UnidadTransporteController.java`: el import de `config.PaginacionConfig` quedó antes de `common.auditoria.*` (orden no alfabético, cosmético).
