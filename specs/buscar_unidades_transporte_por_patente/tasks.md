# Tasks — HU-27 Buscar unidades de transporte por patente

> El orden sigue el stack: paginación común, (sin migración), especificaciones y orden, entidad y repositorio, servicio, controlador, tests del backend, componentes comunes y refactor de auditoría, API, hook y tabla del frontend, i18n, acceso, documentación y verificación.
> Abreviaturas: `gn.` = `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos`; `ut.` = `gn.unidades_transporte`.
> Cada test nombrado acá es el que se cita en la trazabilidad de `progress/impl_buscar_unidades_transporte_por_patente.md`.

## Backend: paginación unificada (10 / 20)

- [ ] T1 — En `gn/config/PaginacionConfig`:
  - Agregar `public static final int TAMANIO_PAGINA_POR_DEFECTO = 10` y `TAMANIO_PAGINA_MAXIMO = 20`.
  - `setMaxPageSize(TAMANIO_PAGINA_MAXIMO)` + `setFallbackPageable(PageRequest.of(0, TAMANIO_PAGINA_POR_DEFECTO))`.
  - Actualizar el Javadoc.
  - Cubre: CA5, CA6, CA16.
- [ ] T2 — En los 7 `GET …/auditoria`, cambiar el tamaño por `@PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "updatedAt", direction = DESC)`. Los controladores son `UsuarioController`, `NeumaticoController`, `ConductorController`, `ProveedorController`, `ServicioReparacionController`, `UnidadTransporteController` y `CentroAlmacenamientoController`. Cubre: CA16.
- [ ] T3 — Ajustar `gn/common/auditoria/AuditoriaControllersTest`:
  - `enlazaParametros`: `size=15`, espera 15.
  - `usaLosValoresPorDefecto`: espera `PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO` (10); el DisplayName dice "10 por página".
  - `acotaElTamanioDePagina`: espera `PaginacionConfig.TAMANIO_PAGINA_MAXIMO` (20); DisplayName "se acota a 20".
  - Cubre: CA16.

## Backend: búsqueda

- [ ] T4 — Confirmar que no hace falta migración: `V16` da `SELECT` + `*_select USING (true)` a los tres roles sobre `unidades_transporte` y `configuraciones_unidad`, y `V19` no cambió el `SELECT`. Si algo de esto no se cumple, parar y avisar al leader (no editar migraciones existentes). Cubre: CA8.
- [ ] T5 — Crear `ut/domain/EspecificacionesUnidadTransporte`:
  - `activas()`: `deletedAt IS NULL`.
  - `patenteContiene(String)`: `null` → `unrestricted`; si no, `LIKE '%…%'` con `\`, `%` y `_` escapados y `\` como carácter de escape.
  - Cubre: CA1, CA2, CA3.
- [ ] T6 — Crear `ut/domain/OrdenUnidadesTransporte.esValido(Pageable)` con la lista blanca `{patente}`. Cubre: CA7.
- [ ] T7 — DTOs, mapper y carga sin N+1:
  - Crear `ut/domain/dto/ConfiguracionUnidadListadoDTO(String nombre, TipoUnidad tipoUnidad)` y `ut/domain/dto/UnidadTransporteListadoDTO(UUID publicId, String patente, ConfiguracionUnidadListadoDTO configuracion)`.
  - Agregar a `UnidadTransporteMapper` `toListadoDTO(UnidadTransporte)` y `toConfiguracionListadoDTO(ConfiguracionUnidad)`.
  - Redeclarar en `UnidadTransporteRepository` el `findAll(Specification, Pageable)` con `@EntityGraph(attributePaths = "configuracion")`. No se toca la entidad: no hay `@BatchSize` ni carga de slots.
  - Cubre: CA1, CA5.
- [ ] T8 — Agregar `buscar(String patente, Pageable pageable)` a `UnidadTransporteService` (con Javadoc) y a `UnidadTransporteServiceImpl`, con `@Transactional(readOnly = true)`. Comportamiento:
  - `debug` de entrada y del total.
  - Orden no permitido: `warn` + `ParametroInvalidoException`, sin consultar el repositorio.
  - Normalizar con `Patentes.normalizar`; un término en blanco pasa a `null`.
  - Consultar con `activas().and(patenteContiene(…))`.
  - Mapear con `map(unidadTransporteMapper::toListadoDTO)`.
  - Cubre: CA1, CA2, CA3, CA4, CA5, CA7.
- [ ] T9 — Agregar en `UnidadTransporteController` el `@GetMapping` raíz:
  - Firma: `buscar(@RequestParam(required = false) String patente, @ParameterObject @PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "patente") Pageable pageable)`, que devuelve `Page<UnidadTransporteListadoDTO>`.
  - Con `@PreAuthorize("hasRole('LECTOR')")`.
  - Con `@Operation`/`@ApiResponses` (200, 400, 401) y `security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`.
  - Cubre: CA1, CA3, CA5, CA6, CA7, CA8, CA9.

## Backend: tests

- [ ] T10 — `ut/domain/OrdenUnidadesTransporteTest`:
  - `esValido_sinOrden_true`
  - `esValido_porPatente_true` (asc y desc)
  - `esValido_porOtroCampo_false`
  - `esValido_patenteYOtroCampo_false`
  - Cubre: CA7.
- [ ] T11 — `UnidadTransporteServiceImplTest`, `@Nested class Buscar`:
  - `buscaConElPageableRecibido`: con `" ab-1 "` se llama una vez a `findAll(any(Specification.class), eq(pageable))`. El término normalizado no se puede afirmar sobre una `Specification` opaca; lo prueba T13 `ignoraEspaciosYGuiones`. Cubre: CA1.
  - `terminoEnBlanco_noFiltraPorPatente`: `null`, `""`, `"  "`, `"-"`. Cubre: CA3.
  - `ordenNoPermitido_lanzaParametroInvalido`, con `verifyNoInteractions(unidadTransporteRepository)`. Cubre: CA7.
  - `mapeaLaPaginaConToListadoDTO`: contenido igual a `toListadoDTO` de cada unidad, con `totalElements`, número y tamaño de página preservados. Cubre: CA1, CA5.
- [ ] T11b — `UnidadTransporteMapperTest`:
  - `toListadoDTO_mapeaPatenteYConfiguracionResumida`: `publicId`, `patente`, `configuracion.nombre` y `configuracion.tipoUnidad`.
  - `toListadoDTO_conFuenteNula_devuelveNull`.
  - `toListadoDTO_sinConfiguracion_dejaConfiguracionNula`.
  - `toConfiguracionListadoDTO_conFuenteNula_devuelveNull`.
  - Cubre: CA1.
- [ ] T12 — `UnidadTransporteControllerTest`: agregar `PaginacionConfig.class` al `@Import` y crear `@Nested @DisplayName("GET /api/v1/unidades-transporte") class Buscar`. Cubre: CA1, CA3–CA9.
  - `buscaConLector` → 200. Cubre: CA1, CA5, CA8.
    - El cuerpo trae `content[0].publicId`, `content[0].patente`, `content[0].configuracion.nombre`, `content[0].configuracion.tipoUnidad` y `page.totalElements`, y no trae `content[0].slots` ni `content[0].configuracion.id` (`jsonPath(...).doesNotExist()`).
    - El servicio recibe `patente` y un `Pageable` con el `page` y el `size` pedidos.
    - REST Docs `unidad-transporte-buscar`, con `queryParameters` (`patente`, `page`, `size`, `sort`) y `responseFields`.
  - `buscaConEditor`, `buscaConAdministrador` → 200. Cubre: CA8.
  - `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto`: el servicio recibe `patente = null` y un `Pageable` con `page 0`, `size = TAMANIO_PAGINA_POR_DEFECTO` (10) y `sort patente: ASC`. Cubre: CA3, CA5.
  - `sinResultados_devuelvePaginaVacia` → 200, `content` vacío, `page.totalElements = 0`. Cubre: CA4.
  - `tamanioMayorAlMaximo_seAcota`: con `size=500` el servicio recibe `size = TAMANIO_PAGINA_MAXIMO` (20). Cubre: CA6.
  - `ordenNoPermitido`: el servicio mockeado lanza `ParametroInvalidoException` → 400 `ExceptionInfo`. Cubre: CA7.
  - `sinToken` → 401 `ExceptionInfo`. Cubre: CA9.
  - Además, los tests existentes del controlador (alta, configuraciones) siguen verdes con `PaginacionConfig` importada.
- [ ] T13 — Crear `ut/BusquedaUnidadTransporteIntegrationTests`.
  - **Configuración**: `@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")` + `@Import(TestcontainersConfiguration.class)`, con el mismo cableado que `RegistroUnidadTransporteIntegrationTests`: limpieza en `@BeforeEach`, datos insertados como owner y contexto de seguridad como **Lector**.
  - **Datos**:
    - Activas: `AB123CD`, `AB456EF`, `XYZ789` y `AC100DE`, con configuraciones distintas (tractor común, semirremolque).
    - Dada de baja: `AB123ZZ` (`deleted_at = now()`, `deleted_by` = un UUID).
    - Para CA5, una configuración que genere > 10 unidades no hace falta: alcanza con `PageRequest.of(1, 2, …)`.
  - **Casos**:
    - `parcialSinDistinguirMayusculas`: `"b12"` → solo `AB123CD`. Cubre: CA1.
    - `ignoraEspaciosYGuiones`: `"ab 1"` y `"AB-12"` → `AB123CD`. Cubre: CA1.
    - `comodinesSeBuscanLiterales`: `"%"` y `"_"` → vacío. Cubre: CA1, CA4.
    - `devuelveLaConfiguracionResumida`: cada elemento trae `nombre` y `tipoUnidad` de su configuración. Cubre: CA1.
    - `excluyeLasDadasDeBaja`: `"AB123"` → solo `AB123CD`, `totalElements = 1`. Cubre: CA2.
    - `sinTermino_devuelveTodasLasActivas`: `null` y `"  "` → las 4 unidades, ordenadas por patente, sin `AB123ZZ`. Cubre: CA2, CA3.
    - `sinCoincidencias_devuelvePaginaVacia`: `"QQQ"` → `content` vacío, `totalElements = 0`. Cubre: CA4.
    - `pagina`: `PageRequest.of(1, 2, Sort.by("patente"))` → 2 elementos (`AB456EF`… según el orden alfabético), `totalElements = 4`, `totalPages = 2`. Cubre: CA5.
    - `cargaLaPaginaSinNMasUno`: después de `statistics.clear()`, `buscar(null, PageRequest.of(0, 20, …))` ejecuta ≤ 2 sentencias (`getPrepareStatementCount()`: página con la configuración unida + count), sin consultas por fila. Cubre: CA5.
    - `lectorLeeConSuRolDeBase`: la consulta corre como `gn_lector` sin `permission denied` (puede ser el mismo flujo de los anteriores, documentado así). Cubre: CA8.
- [ ] T14 — `./mvnw -B verify` en verde: JaCoCo ≥ 85 % LINE y BRANCH; `ModularityTests`, `AutorizacionBaseDatosTests` y los tests de auditoría en verde. Cubre: CA1–CA9, CA16.

## Frontend: componentes comunes y auditoría

- [ ] T15 — `lib/api/page.ts`: agregar `export const DEFAULT_PAGE_SIZE = 10`, con un comentario que lo ate a `PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO`. Cubre: CA13, CA17.
- [ ] T16 — Crear `components/common/table-pagination.tsx` (`TablePagination`) y `components/common/sortable-table-head.tsx` (`SortableTableHead`), genéricos y sin dominio (firmas en `design.md`). Agregar `pagination.previous` y `pagination.next` a `common.json` (es y en). Cubre: CA13, CA17.
- [ ] T17 — Refactorizar `features/audit/components/audit-records-table.tsx`:
  - Borrar `SortableHead` y el pie inline, y usar `SortableTableHead` + `TablePagination`.
  - Quitar `pagination.previous` y `pagination.next` de `audit.json` (es y en).
  - `DEFAULT_AUDIT_QUERY.size = DEFAULT_PAGE_SIZE`.
  - El resto del comportamiento no cambia.
  - Cubre: CA16, CA17.

## Frontend: unidades de transporte

- [ ] T18 — En `features/transport-units/api/transport-units-api.ts`, agregar `TransportUnitsQuery`, `DEFAULT_TRANSPORT_UNITS_QUERY` (con `size: DEFAULT_PAGE_SIZE`) el tipo de fila nuevo `TransportUnitListItem` (`publicId`, `patente`, `configuracion: { nombre, tipoUnidad }`; no reutiliza `TransportUnit` del alta y no lleva schema zod porque es una respuesta) y `searchTransportUnits(query)`, que devuelve `Page<TransportUnitListItem>`. La función llama a `GET /api/v1/unidades-transporte`, omite `patente` si está vacío y manda `sort=patente,<dir>`. Cubre: CA10, CA11, CA13.
- [ ] T19 — Crear `features/transport-units/hooks/use-transport-units.ts`: `useTransportUnits(query)` con la clave `["transport-units", "list", query]` y `keepPreviousData`. Confirmar, sin cambiarlo, que el `predicate` de `useRegisterTransportUnit` invalida esta clave. Cubre: CA10, CA15.
- [ ] T20 — Crear `features/transport-units/components/transport-units-table.tsx` (`TransportUnitsTable`). Cubre: CA10, CA11, CA12, CA13.
  - Búsqueda con `useDebouncedValue`; un término nuevo vuelve a la página 0.
  - Tabla con Patente (`SortableTableHead`), Configuración y Tipo. Las filas no son clicables (el detalle es otra HU).
  - Skeleton mientras carga y error con reintento.
  - Estados vacíos distintos con y sin término.
  - `TablePagination` al pie.
- [ ] T21 — En `features/transport-units/pages/transport-units-page.tsx`, reemplazar el `EmptyState` placeholder y el `TODO` por `<TransportUnitsTable />`, y ajustar el comentario del botón de alta. Cubre: CA10, CA14.
- [ ] T22 — En `lib/i18n/locales/{es,en}/transport-units.json`, quitar `empty` y agregar `list.*` y `unitTypes.*` (claves en `design.md`); los textos en es van en voseo. Cubre: CA10, CA12, CA13.
- [ ] T23 — En `lib/access.ts`, quitar `[ROUTES.transportUnits]` de `ROUTE_ACCESS` y actualizar el comentario de la matriz. Cubre: CA14.
- [ ] T24 — Correr todo en verde:
  - `npm run lint` (con `-- --fix` para el orden de los imports)
  - `npm run typecheck`
  - `VITE_API_URL=https://api.example.com npm run build`
  - `npm audit --omit=dev --audit-level=high`
  - Cubre: CA10–CA15 y CA17 (verificación estática).

## Documentación

- [ ] T25 — En `docs/Seguridad/seguridad.md`:
  - 2.10: fila nueva `GET /api/v1/unidades-transporte`, `hasRole('LECTOR')`, los tres roles.
  - Sección de auditoría: "tope 100" pasa a "10 por defecto, tope 20".
  - 3.4: tabla y párrafo (el Lector entra a Unidades de transporte, en solo lectura).
  - Pendiente, punto 2.
  - Cubre: CA8, CA14, CA16.
- [ ] T26 — En `CLAUDE.md`:
  - "Auditing": "max size 100" pasa a 10/20, con las constantes de `PaginacionConfig`.
  - "Current state" de `unidades_transporte`: búsqueda paginada con `UnidadTransporteListadoDTO` (configuración cargada con `@EntityGraph`), lista blanca `patente` y el listado en el frontend; quitar "No listing yet".
  - "Authentication (frontend)": `ROLE_LECTOR` también ve `/transport-units`.
  - Frontend: `TablePagination`, `SortableTableHead` y `DEFAULT_PAGE_SIZE`.
  - Cubre: — (documentación).

## Verificación en el stack real

- [ ] T27 — (implementer) Prueba con `curl` contra `docker compose up -d --build`, sin imprimir tokens. Cubre: CA1, CA3, CA4, CA5, CA6, CA7, CA8, CA9, CA16.
  - Como `victoria.lector`:
    - `GET /api/v1/unidades-transporte` → 200 y `page.size = 10`.
    - `?patente=ab%201` → 200 con coincidencias, cada una solo con `publicId`, `patente` y `configuracion{nombre, tipoUnidad}`.
    - `?patente=QQQ` → 200, página vacía.
    - `?size=500` → `page.size = 20`.
    - `?sort=id,asc` → 400.
  - Sin token → 401.
  - Como `victoria.administrador`: `GET /api/v1/proveedores/auditoria` → `page.size = 10`; con `?size=500` → `page.size = 20`.
  - El resultado va a `progress/smoke_buscar_unidades_transporte_por_patente.md`.
- [ ] T28 — (implementer) Dejar en `progress/impl_buscar_unidades_transporte_por_patente.md` los pasos de la prueba en el navegador para el **leader** (Claude in Chrome, `http://localhost:3000`). En cada paso hay que revisar la consola y las requests. Cubre: CA10–CA15, CA17.
  1. Como `victoria.editor`, registrar unidades con el diálogo (p. ej. `AB123CD`, `AB456EF`, `XYZ789`). Después de cada alta, la tabla se actualiza sola. → CA15
  2. Entrar a Unidades de transporte con el campo vacío: la request sale sin `patente` y con `size=10`, y la tabla muestra todas las unidades ordenadas por patente. → CA11
  3. Escribir `b12`. Tras el debounce sale una sola request con `patente=b12&page=0`, y la tabla muestra `AB123CD` con su configuración y su tipo; hacer clic en la fila no hace nada. → CA10
  4. Escribir `qqq`: aparece el estado "No se encontraron unidades de transporte". Al borrar el texto vuelve el listado completo. → CA12, CA11
  5. Con más de 10 unidades activas (registrar las que falten por `curl` o por el diálogo):
     - aparece el resumen "N unidades · página 1 de 2";
     - Anterior está deshabilitado y Siguiente pide `page=1`;
     - escribir un término vuelve a `page=0`;
     - alternar el orden de Patente pide `sort=patente,desc`.
     → CA13
  6. Cerrar sesión e ingresar como `victoria.lector`. El ítem "Unidades de transporte" aparece en la navegación, la página lista las unidades y no hay botón "Registrar unidad". → CA14
  7. **Regresión de auditoría**: como `victoria.administrador`, entrar a `/audit`, pestaña Unidades de transporte (y una más, p. ej. Proveedores). → CA17
     - la request lleva `size=10`;
     - con más de 10 registros, el resumen y Anterior / Siguiente funcionan como antes;
     - los encabezados Creado / Última modificación alternan el orden (`sort=createdAt,desc|asc`) y muestran su ícono;
     - la búsqueda por texto filtra y vuelve a la página 0;
     - abrir un registro muestra su ficha.
  El resultado se agrega a `progress/smoke_buscar_unidades_transporte_por_patente.md`.

## Matriz de trazabilidad (CA → verificación)

| CA | Verificación |
|---|---|
| CA1 | T13 `parcialSinDistinguirMayusculas`, `ignoraEspaciosYGuiones`, `comodinesSeBuscanLiterales`, `devuelveLaConfiguracionResumida`; T12 `buscaConLector`; T11 `buscaConElPageableRecibido`, `mapeaLaPaginaConToListadoDTO`; T11b; T27 |
| CA2 | T13 `excluyeLasDadasDeBaja`, `sinTermino_devuelveTodasLasActivas` |
| CA3 | T13 `sinTermino_devuelveTodasLasActivas`; T11 `terminoEnBlanco_noFiltraPorPatente`; T12 `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto`; T27 |
| CA4 | T13 `sinCoincidencias_devuelvePaginaVacia`; T12 `sinResultados_devuelvePaginaVacia`; T27 |
| CA5 | T13 `pagina`, `cargaLaPaginaSinNMasUno`; T12 `buscaConLector`, `sinPatente_usaOrdenPorPatenteYTamanioPorDefecto`; T11 `mapeaLaPaginaConToListadoDTO`; T27 |
| CA6 | T12 `tamanioMayorAlMaximo_seAcota`; T27 |
| CA7 | T10; T11 `ordenNoPermitido_lanzaParametroInvalido`; T12 `ordenNoPermitido`; T27 |
| CA8 | T12 `buscaConLector`, `buscaConEditor`, `buscaConAdministrador`; T13 `lectorLeeConSuRolDeBase`; T27 |
| CA9 | T12 `sinToken`; T27 |
| CA10 | T24 + T28, paso 3 |
| CA11 | T24 + T28, pasos 2 y 4 |
| CA12 | T24 + T28, paso 4 |
| CA13 | T24 + T28, paso 5 |
| CA14 | T24 + T28, paso 6 |
| CA15 | T24 + T28, paso 1 |
| CA16 | T3 `AuditoriaControllersTest.usaLosValoresPorDefecto`, `acotaElTamanioDePagina`, `enlazaParametros`; T27 |
| CA17 | T24 + T28, paso 7 |
