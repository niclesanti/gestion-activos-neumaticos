# Diseño — HU-27 Buscar unidades de transporte por patente

## Resumen

- Un único endpoint de listado con filtro opcional en el módulo `unidades_transporte`, que es dueño del agregado: `GET /api/v1/unidades-transporte?patente=&page=&size=&sort=`.
- La respuesta usa un DTO de listado propio (`UnidadTransporteListadoDTO`: `publicId`, `patente` y la configuración con `nombre` y `tipoUnidad`), sin slots. La carga es en 2 consultas fijas por página (count + página con la configuración), sin N+1.
- **No hay migración**: los permisos y la RLS vigentes ya cubren la lectura de los tres roles.
- Se **unifica el criterio de paginación** en 10 por defecto y 20 como máximo, con una sola fuente de verdad en el backend (`PaginacionConfig`) y otra en el frontend (`lib/api/page.ts`). Esto alcanza también a la consulta de auditoría (HU-43).
- El pie de paginación y el encabezado ordenable salen de `features/audit` a `src/components/common/`. Los usan auditoría y unidades de transporte (y HU-28 después).
- La página `/transport-units` reemplaza su placeholder por la tabla paginada con búsqueda y se abre al Lector.

## Backend (`backend/gestion-neumaticos/`)

Paquete base `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos` (abreviado `gn.`); `ut.` = `gn.unidades_transporte`.

### Contrato HTTP

```
GET /api/v1/unidades-transporte?patente=ab1&page=0&size=10&sort=patente,asc
Authorization: Bearer <jwt>
@PreAuthorize("hasRole('LECTOR')")   → Lector, Editor y Administrador
```

| Caso | Status | Cuerpo |
|---|---|---|
| OK (con o sin resultados) | 200 | `Page<UnidadTransporteListadoDTO>` `VIA_DTO` |
| `sort` por un campo distinto de `patente` | 400 | `ExceptionInfo` (`ParametroInvalidoException`) |
| sin token / token inválido | 401 | `ExceptionInfo` (`JsonSecurityErrorHandler`) |

- `patente` (opcional): se normaliza con `Patentes.normalizar` (mayúsculas, sin espacios ni guiones). Si falta, viene vacía o queda vacía después de normalizar, no se filtra (devuelve todas las activas).
- `page` (desde 0). `size` es 10 por defecto y 20 como máximo; un valor mayor se acota en silencio a 20 (ver "Paginación unificada").
- `sort`: por defecto `patente,asc`, con lista blanca `{patente}`. Como `patente` es única, el orden es total y la paginación es estable.

Ejemplo de respuesta:

```json
{
  "content": [
    { "publicId": "0193…", "patente": "AB123CD",
      "configuracion": { "nombre": "Tractor común", "tipoUnidad": "TRACTOR" } }
  ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

### DTO de listado

Verificado en el código:

- `ConfiguracionUnidadResumenDTO` es `(Long id, String nombre, TipoUnidad tipoUnidad)`. Trae `id` de más, así que **no se reutiliza**.
- El campo de tipo en `ConfiguracionUnidad` se llama `tipoUnidad` (enum `TipoUnidad`: `TRACTOR` / `SEMIRREMOLQUE`).

DTOs nuevos en `ut.domain.dto`, uno por archivo como el resto del paquete:

- `record ConfiguracionUnidadListadoDTO(String nombre, TipoUnidad tipoUnidad)`.
- `record UnidadTransporteListadoDTO(UUID publicId, String patente, ConfiguracionUnidadListadoDTO configuracion)`, siguiendo la convención de `ProveedorListadoDTO`.

El mapeo va por MapStruct en `UnidadTransporteMapper`:

- `UnidadTransporteListadoDTO toListadoDTO(UnidadTransporte unidadTransporte);`
- `ConfiguracionUnidadListadoDTO toConfiguracionListadoDTO(ConfiguracionUnidad configuracion);`

El segundo es el método que MapStruct usa para el campo anidado. Se declara explícito para que no choque con el mapeo implícito a `ConfiguracionUnidadResumenDTO` de `toResponseDTO`, donde el tipo destino distinto ya desambigua.

### Carga sin N+1: 2 consultas fijas por página

- La única asociación que se lee es `configuracion` (`@ManyToOne LAZY`, *to-one*).
- Se carga con `@EntityGraph(attributePaths = "configuracion")` sobre el `findAll(Specification, Pageable)` redeclarado en `UnidadTransporteRepository`. Un join a una asociación *to-one* no rompe el `LIMIT/OFFSET` en SQL.
- Queda la consulta de la página con la configuración unida más el `count` de Spring Data.
- Los slots no se tocan, así que no se cargan: no hay `@BatchSize` ni ningún cambio en la entidad.
- El mapeo ocurre dentro del `@Transactional(readOnly = true)` del servicio, con `open-in-view` apagado.
- El test de integración cuenta las sentencias con las estadísticas de Hibernate para fijar el "≤ 2 consultas".

### Paginación unificada (cambio A)

**Cómo está hoy** (verificado en el código):

- `config/PaginacionConfig` fija `setMaxPageSize(100)` con una variable local y no define el pageable de reserva.
- Los siete `GET …/auditoria` (`UsuarioController`, `NeumaticoController`, `ConductorController`, `ProveedorController`, `ServicioReparacionController`, `UnidadTransporteController`, `CentroAlmacenamientoController`) declaran cada uno `@PageableDefault(size = 20, sort = "updatedAt", direction = DESC)`.
- No hay `@MaxPageSize` ni propiedades `spring.data.web.pageable.*`, que no aplican con `@EnableSpringDataWebSupport`.
- En el frontend, `features/audit/api/audit-api.ts` tiene `DEFAULT_AUDIT_QUERY.size = 20`.
- `AuditoriaControllersTest` afirma lo que existe hoy:
  - `enlazaParametros` pide `size=50` y espera 50.
  - `usaLosValoresPorDefecto` espera 20.
  - `acotaElTamanioDePagina` (DisplayName "se acota a 100") pide 5000 y espera 100.

**Diseño: una sola fuente de verdad por lado.**

- Backend, `config/PaginacionConfig`:
  - `public static final int TAMANIO_PAGINA_POR_DEFECTO = 10;` y `public static final int TAMANIO_PAGINA_MAXIMO = 20;`. Son `public` porque las usan otros módulos y deben ser constantes de compilación para poder ir en anotaciones (reglas 1 y 2 de constantes; mismo precedente que `OpenApiConfig.BEARER_AUTH`).
  - El customizer hace `resolver.setMaxPageSize(TAMANIO_PAGINA_MAXIMO)` y `resolver.setFallbackPageable(PageRequest.of(0, TAMANIO_PAGINA_POR_DEFECTO))`.
  - Los ocho endpoints paginados (siete de auditoría + la búsqueda nueva) declaran `@PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = …)`. Así ningún controlador repite el literal: `@PageableDefault` gana sobre el pageable de reserva y por eso también tiene que leer la constante.
  - El Javadoc de la clase pasa a documentar "10 por defecto, 20 como máximo, para todos los listados".
- Frontend, `src/lib/api/page.ts`:
  - `export const DEFAULT_PAGE_SIZE = 10`, con un comentario que lo ate a `PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO`.
  - `DEFAULT_AUDIT_QUERY.size` y `DEFAULT_TRANSPORT_UNITS_QUERY.size` lo usan.
  - No se agrega una constante de máximo: la interfaz nunca pide más que el valor por defecto, y una constante sin uso no es constante.

**`size > 20`: se acota en silencio a 20, sin 400.** Es lo que hace hoy auditoría con el tope de 100 (`acotaElTamanioDePagina` lo afirma), y es el comportamiento nativo de `PageableHandlerMethodArgumentResolver.setMaxPageSize`. Responder 400 obligaría a validar `size` a mano en cada controlador o en un resolver propio, rompería el contrato vigente de auditoría y no aporta seguridad: el tope ya cumple su función de impedir páginas gigantes.

### Archivos

| Archivo | Cambio |
|---|---|
| `gn/config/PaginacionConfig.java` | Constantes `TAMANIO_PAGINA_POR_DEFECTO = 10` y `TAMANIO_PAGINA_MAXIMO = 20`, máximo y pageable de reserva desde esas constantes, Javadoc. |
| Los 7 controladores con `GET …/auditoria` (listados arriba) | `@PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "updatedAt", direction = Sort.Direction.DESC)`. Solo cambia el `size`. |
| `ut/domain/EspecificacionesUnidadTransporte.java` | **Nuevo**. Clase final con constructor privado, como `Patentes`. `static Specification<UnidadTransporte> activas()` (`deletedAt IS NULL`) y `static Specification<UnidadTransporte> patenteContiene(String patenteNormalizada)`: si es `null` devuelve `Specification.unrestricted()`; si no, `cb.like(root.get("patente"), "%" + escapado + "%", '\\')`, escapando `\`, `%` y `_` como en `EspecificacionesAuditoria.contiene`. No hace falta `lower()`: la columna y el término están en mayúsculas. |
| `ut/domain/OrdenUnidadesTransporte.java` | **Nuevo**. Lista blanca del orden, inspirada en `OrdenAuditoria` pero sin lanzar: `static boolean esValido(Pageable)` devuelve `true` si todos los `Sort.Order` son de `patente` o no hay orden. La lista blanca es un `Set.of("patente")` `private static final` (regla 3 de constantes). Devuelve un booleano para que el servicio loguee el rechazo en `warn` antes de lanzar. |
| `ut/domain/dto/ConfiguracionUnidadListadoDTO.java` | **Nuevo**. `record (String nombre, TipoUnidad tipoUnidad)`. |
| `ut/domain/dto/UnidadTransporteListadoDTO.java` | **Nuevo**. `record (UUID publicId, String patente, ConfiguracionUnidadListadoDTO configuracion)`. |
| `ut/mapper/UnidadTransporteMapper.java` | Nuevos `toListadoDTO(UnidadTransporte)` y `toConfiguracionListadoDTO(ConfiguracionUnidad)`. |
| `ut/repository/UnidadTransporteRepository.java` | Redeclarar `@EntityGraph(attributePaths = "configuracion") Page<UnidadTransporte> findAll(Specification<UnidadTransporte> spec, Pageable pageable);`. Alcanza también a `listarAuditoria`: un join extra a un catálogo de 3 filas, inocuo. |
| `ut/service/UnidadTransporteService.java` | Nuevo método con Javadoc: `Page<UnidadTransporteListadoDTO> buscar(String patente, Pageable pageable);`, con `@throws ParametroInvalidoException` si se ordena por otro campo. |
| `ut/service/UnidadTransporteServiceImpl.java` | Implementación `@Transactional(readOnly = true)`: (1) `log.debug` de entrada con `patente` y `pageable`; (2) si `!OrdenUnidadesTransporte.esValido(pageable)`, `log.warn("Búsqueda de unidades rechazada: orden no permitido {}", pageable.getSort())` y `throw new ParametroInvalidoException("Solo se puede ordenar por patente")`, con el mensaje inline porque se usa una vez; (3) normaliza con `Patentes.normalizar` y pasa a `null` si queda en blanco; (4) `repository.findAll(activas().and(patenteContiene(termino)), pageable).map(unidadTransporteMapper::toListadoDTO)`; (5) `log.debug` del total. No cambia estado, así que no hay `info`. |
| `ut/controller/UnidadTransporteController.java` | Nuevo `@GetMapping` en la raíz, con `@PreAuthorize("hasRole('LECTOR')")` y `@Operation`/`@ApiResponses` (200, 400, 401) con `security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`. Firma: `ResponseEntity<Page<UnidadTransporteListadoDTO>> buscar(@Parameter(description = …) @RequestParam(required = false) String patente, @ParameterObject @PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "patente") Pageable pageable)`. El `listarAuditoria` del mismo controlador cambia su `size` como los demás. |

No se tocan la entidad `UnidadTransporte`, `UnidadTransporteResponseDTO`, `exception/` ni `security/`. `ParametroInvalidoException` ya está mapeada a 400. No hay API pública nueva entre módulos.

### Base de datos

**Sin migración.** Verificado en `V16` + `V19`:

- `GRANT SELECT` sobre `unidades_transporte` y `configuraciones_unidad` para `gn_administrador`, `gn_editor` y `gn_lector`.
- Política restrictiva `*_requiere_autenticacion` y permisiva `*_select USING (true)` para los tres roles en ambas tablas.
- `V19` agregó `deleted_at` (el filtro de "activas") sin tocar el `SELECT`.

Sin índice nuevo:

- La búsqueda por contenido (`LIKE '%término%'`) no puede usar un B-tree, y la flota es de decenas a cientos de unidades.
- El orden por defecto usa `uk_unidades_transporte_patente`.

### Tests del backend

| Clase | Tipo | Qué agrega o cambia |
|---|---|---|
| `gn/common/auditoria/AuditoriaControllersTest` | `@WebMvcTest` | **Ajustar** a 10/20: `enlazaParametros` pide `size=15` (≤ 20) y espera 15; `usaLosValoresPorDefecto` espera 10 (y su DisplayName dice "10 por página"); `acotaElTamanioDePagina` espera 20 (DisplayName "se acota a 20"). Afirmar contra las constantes de `PaginacionConfig` en lugar de literales. |
| `ut/controller/UnidadTransporteControllerTest` | `@WebMvcTest` | Agregar `PaginacionConfig.class` al `@Import`, como `AuditoriaControllersTest`, y un `@Nested @DisplayName("GET /api/v1/unidades-transporte") class Buscar` (ver `tasks.md`). REST Docs `unidad-transporte-buscar` con `queryParameters` y `responseFields` (`content[].publicId`, `content[].patente`, `content[].configuracion.nombre`, `content[].configuracion.tipoUnidad`, `page.*`). |
| `ut/service/UnidadTransporteServiceImplTest` | Mockito | `@Nested class Buscar`: usa el `Pageable` recibido, término en blanco = sin filtro, rechaza un orden no permitido sin consultar el repositorio, mapea la página con `toListadoDTO`. |
| `ut/mapper/UnidadTransporteMapperTest` | unitario | `toListadoDTO` con unidad completa; fuente `null` → `null`; configuración `null` → `configuracion` `null`; `toConfiguracionListadoDTO(null)` → `null`. Cubre las ramas del `*MapperImpl` (convención de cobertura de mappers). |
| `ut/domain/OrdenUnidadesTransporteTest` | unitario | `esValido` en los casos válidos e inválidos. |
| `ut/BusquedaUnidadTransporteIntegrationTests` | Testcontainers | **Nuevo**. Mismo cableado que `RegistroUnidadTransporteIntegrationTests`: owner `JdbcTemplate` para preparar los datos, y `autenticar(NivelAcceso)` para que `RolBaseDatosDataSource` cambie a `gn_<nivel>`. Con `@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")`. Inserta como owner unidades activas y una dada de baja (`deleted_at`, `deleted_by`) y llama a `UnidadTransporteService.buscar` como Lector. Valida contra PostgreSQL real la coincidencia parcial, la normalización, el escape de comodines, la exclusión de bajas, el listado completo, el resultado vacío, la paginación, la configuración resumida de cada fila y que una página completa se carga con ≤ 2 sentencias (`Statistics.getPrepareStatementCount()`: página con configuración + count). |

`AutorizacionBaseDatosTests`, `ModularityTests` y el resto de los tests del módulo deben seguir verdes. Los tests de servicio que construyen `PageRequest.of(0, 20)` a mano no dependen del tope y no cambian.

## Frontend (`frontend/gestion-neumaticos/src/`)

### Componentes compartidos extraídos de auditoría (cambio D)

| Archivo | Contenido |
|---|---|
| `components/common/table-pagination.tsx` | **Nuevo**. `TablePagination({ page, totalPages, totalElements, summary, onPageChange })`. `page` es el número de página (base 0) que devuelve `Page<T>`. Renderiza el pie que hoy está inline en `audit-records-table.tsx`: el resumen con `aria-live="polite"` y los botones Anterior / Siguiente con `ChevronLeftIcon` / `ChevronRightIcon`, deshabilitados en los extremos. No renderiza nada si `totalElements === 0`. `summary: string` llega ya traducido por el feature, porque "registros" o "unidades" es texto de dominio. Las etiquetas Anterior / Siguiente salen del namespace `common` (`pagination.previous` / `pagination.next`). |
| `components/common/sortable-table-head.tsx` | **Nuevo**. `SortableTableHead<F extends string>({ field, label, activeField, direction, onToggle })`, que es el `SortableHead` de auditoría vuelto genérico: `TableHead` con `aria-sort` y un `Button` ghost con `ArrowUpDownIcon` / `ArrowUpIcon` / `ArrowDownIcon`. `direction` es `"asc" \| "desc"`. No contiene texto propio. |
| `lib/i18n/locales/{es,en}/common.json` | Agregar `pagination.previous` y `pagination.next` (es: "Anterior" / "Siguiente"). |
| `features/audit/components/audit-records-table.tsx` | **Refactor sin cambio de comportamiento**: borrar el `SortableHead` local y el pie inline, y usar `SortableTableHead` y `TablePagination` (`summary={t("pagination.summary", {...})}`). |
| `lib/i18n/locales/{es,en}/audit.json` | Quitar `pagination.previous` y `pagination.next`, que ahora están en `common`. `pagination.summary_*` se queda en `audit`. |
| `features/audit/api/audit-api.ts` | `DEFAULT_AUDIT_QUERY.size = DEFAULT_PAGE_SIZE` (pasa de 20 a 10). |
| `lib/api/page.ts` | `export const DEFAULT_PAGE_SIZE = 10` (ver "Paginación unificada"). |

Los componentes comunes no importan ningún feature (`boundaries/dependencies`) y no tienen dominio.

### Unidades de transporte

| Archivo | Cambio |
|---|---|
| `lib/access.ts` | **Quitar** `[ROUTES.transportUnits]: ADMIN_AND_EDITOR` de `ROUTE_ACCESS` (una ruta ausente queda abierta a cualquier usuario autenticado) y actualizar el comentario de la matriz: el Lector ve Inicio, Unidades de transporte y Configuración. |
| `features/transport-units/api/transport-units-api.ts` | Tipo nuevo para la fila, sin reutilizar `TransportUnit` (que es la respuesta del alta): `TransportUnitListItem = { publicId: string; patente: string; configuracion: { nombre: string; tipoUnidad: UnitType } }`, que refleja `UnidadTransporteListadoDTO`. No lleva schema zod: es una respuesta, y el repo usa zod solo para validar formularios. También nuevos: `TransportUnitsQuery = { patente: string; page: number; size: number; direction: "asc" \| "desc" }`, `DEFAULT_TRANSPORT_UNITS_QUERY` (`patente: ""`, `page: 0`, `size: DEFAULT_PAGE_SIZE`, `direction: "asc"`) y `searchTransportUnits(query)`, que llama a `apiClient.get<Page<TransportUnitListItem>>("/api/v1/unidades-transporte", { params: { patente: query.patente \|\| undefined, page, size, sort: \`patente,${direction}\` } })`. |
| `features/transport-units/hooks/use-transport-units.ts` | **Nuevo**. `useTransportUnits(query)` → `useQuery({ queryKey: ["transport-units", "list", query], queryFn, placeholderData: keepPreviousData })`. La clave queda bajo `["transport-units"]`, que `useRegisterTransportUnit` ya invalida (su `predicate` solo excluye `"configurations"`), así que ese hook no se toca (CA15). |
| `features/transport-units/components/transport-units-table.tsx` | **Nuevo**. `TransportUnitsTable`, con el mismo patrón que `audit-records-table.tsx`. Campos y estado:<br>• campo de búsqueda: `Input` con label "Patente", `SearchIcon`, `maxLength={15}` como el alta, `type="search"`;<br>• `useDebouncedValue` (300 ms);<br>• estado `{ page, direction }`, que vuelve a `page: 0` al cambiar el término.<br>Tabla shadcn `Table`:<br>• columnas: Patente (`SortableTableHead`), Configuración (`configuracion.nombre`) y Tipo (`Badge` con `unitTypes.<TIPO>`); las filas no son clicables (sin `onClick` ni `cursor-pointer`: el detalle es otra HU);<br>• skeleton mientras `isPending`; opacidad y `aria-busy` mientras `isFetching`.<br>Estados y pie:<br>• error con "Reintentar";<br>• `Empty` con término → `list.noResults`; sin término → `list.empty`;<br>• `TablePagination` con `summary={t("list.pagination.summary", {...})}`. |
| `features/transport-units/pages/transport-units-page.tsx` | Reemplazar el `EmptyState` placeholder y su `TODO` por `<TransportUnitsTable />`, y actualizar el comentario del botón: el Lector ahora llega a la ruta y `canPerform` le oculta "Registrar unidad". |
| `lib/i18n/locales/{es,en}/transport-units.json` | Quitar `empty` (era el placeholder). Agregar:<br>• `list.search.{label,placeholder}`<br>• `list.columns.{plate,configuration,type}`<br>• `list.noResults.{title,description}`<br>• `list.empty.{title,description}`<br>• `list.error.{title,description,retry}`<br>• `list.pagination.{summary_one,summary_other}`<br>• `unitTypes.{TRACTOR,SEMIRREMOLQUE}`<br>Textos en voseo:<br>• "No se encontraron unidades de transporte" / "Revisá la patente o probá con menos caracteres."<br>• "Todavía no hay unidades de transporte registradas"<br>• "{{count}} unidad · página {{page}} de {{pages}}" / "{{count}} unidades · …" |

No cambian `ROUTES`, `NAV_GROUPS` (la navegación se filtra sola con `ROUTE_ACCESS`), `ACTION_ACCESS` (buscar es de solo lectura y lo pueden todos) ni el router.

### Decisión confirmada: abrir `/transport-units` al Lector

La HU pide "cualquier nivel de acceso", la base ya le da `SELECT` al Lector y el endpoint va con `hasRole('LECTOR')`. Se quita la entrada de `ROUTE_ACCESS`, porque la convención es "ruta ausente = cualquier usuario autenticado". El botón de alta ya está protegido por `canPerform(accessLevel, "transportUnits.create")`.

## Documentación

- `docs/Seguridad/seguridad.md`:
  - 2.10: fila `GET /api/v1/unidades-transporte` (búsqueda por patente, HU-27) | `hasRole('LECTOR')` | Los tres roles.
  - Sección de auditoría (línea con "el tamaño de página tiene tope 100"): pasar a "10 por defecto y tope 20 (`PaginacionConfig`)".
  - 3.4: el Lector entra a Unidades de transporte en solo lectura (tabla y párrafo).
  - Pendiente, punto 2: Unidades de transporte ya tiene lectura para el Lector en las tres capas.
- `CLAUDE.md`:
  - Viñeta "Auditing": cambiar "max size 100" por "default size 10, max 20 (`PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO` / `TAMANIO_PAGINA_MAXIMO`, shared by every paginated endpoint)".
  - "Current state" de `unidades_transporte`: el `GET` paginado con búsqueda (`UnidadTransporteListadoDTO` con `ConfiguracionUnidadListadoDTO`, configuración cargada con `@EntityGraph` en la misma consulta) y la lista blanca `patente`; en el frontend, el listado (quitar "No listing yet").
  - "Authentication (frontend)": `ROLE_LECTOR` también ve `/transport-units`.
  - Frontend: mencionar `components/common/table-pagination.tsx`, `sortable-table-head.tsx` y `DEFAULT_PAGE_SIZE` en `lib/api/page.ts`.

## Alternativas descartadas

1. **Reutilizar `UnidadTransporteResponseDTO` (con slots) para el listado.** Se evaluó en una revisión anterior: era viable con `@BatchSize` sobre los slots (3 consultas, hasta 260 slots por página). El humano la descartó. **Listado y detalle van separados**:
   - La tabla muestra solo patente, configuración y tipo. Cargar los slots de cada fila es trabajo y payload que nadie usa.
   - La ficha detalle (HU-10, otra historia) se va a pedir aparte con `GET /api/v1/unidades-transporte/{publicId}` al hacer clic en una fila, y ahí sí vienen los slots y los neumáticos montados.
   - Así cada contrato evoluciona por su lado. El listado no arrastra cambios del detalle (p. ej. los datos del neumático por slot) y el detalle no queda limitado por el costo de una página.
   - En esta HU el clic en la fila no hace nada.
2. **Reutilizar `ConfiguracionUnidadResumenDTO` dentro del DTO de listado.** Trae `id`, que la tabla no usa; el humano pidió solo `nombre` y `tipoUnidad`.
3. **Proyección de Spring Data (interfaz o constructor JPQL) en lugar de entidad + MapStruct.** Ahorraría hidratar entidades, pero no se combina bien con `Specification` + `Pageable` en `JpaSpecificationExecutor` (el `findBy(spec, q -> q.as(...).page(...))` complica el orden y el escape). Con 2 consultas y a lo sumo 20 filas, la diferencia no se nota, y MapStruct es la convención del repo.
4. **Responder 400 si `size > 20`.** Ver "Paginación unificada": rompe el contrato vigente de auditoría y no es el comportamiento nativo de Spring Data.
5. **Literal `10` en cada `@PageableDefault`, o depender del valor por defecto de la anotación (también 10).** Repite el valor en ocho lugares, o lo deja implícito en una librería. La constante en `PaginacionConfig` lo vuelve explícito y único.
6. **Índice trigram (`pg_trgm` + GIN).** Exige instalar una extensión, para una tabla de a lo sumo cientos de filas.
7. **`lower(patente) LIKE …` sin normalizar** (reutilizando `EspecificacionesAuditoria.contiene`). No encontraría búsquedas con espacios o guiones, que el alta sí acepta.
8. **Generalizar `OrdenAuditoria` en un validador de orden común.** Tocaría el named interface `common.auditoria`. Conviene evaluarlo con el segundo caso real (HU-28).
9. **Duplicar el pie de paginación y el encabezado ordenable en el feature.** Lo descartó el humano: van a `src/components/common/`.
10. **`GET /api/v1/unidades-transporte/buscar?patente=`.** Lleva un verbo en la URL, contra la convención REST del repo.
