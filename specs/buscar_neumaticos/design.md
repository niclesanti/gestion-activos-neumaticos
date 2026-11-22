# Design — HU-28 buscar_neumaticos

## 1. Contrato

**`GET /api/v1/neumaticos?numeroSerie=&marcaId=&estado=&situacion=&page=&size=&sort=`** — `hasRole('LECTOR')` (los tres roles). Mismo patrón que HU-27.
- Filtros opcionales, combinados con AND, siempre sobre activos (ver HU):
  - `numeroSerie`: recortado y en mayúsculas (como en el alta); en blanco = sin filtro; `LIKE` contiene, con los comodines escapados.
  - `marcaId` (Long, id de `/marcas`): igualdad. Un id inexistente da 200 vacío, no 404.
  - `estado` (`EstadoNeumatico`): igualdad, un solo valor. `DE_BAJA` da 200 vacío (nunca está activo).
  - `situacion` (`SituacionOperativa`): igualdad sobre `situacionOperativaVigente`, un solo valor. `DE_BAJA` da 200 vacío (nunca está activo).
- 200 `Page<NeumaticoListadoDTO>` `VIA_DTO`: `publicId`, `numeroSerie` (nullable), `marca` (nombre), `estadoActual`, `situacionOperativaVigente`, `kilometrajeTotal` (nullable). No reutiliza `NeumaticoResponseDTO` (trae ubicación y datos que la tabla no muestra).
- Paginación: `PaginacionConfig` (10 / 20). Orden por defecto `numeroSerie,asc`; lista blanca: solo `numeroSerie`; desempate interno fijo por `publicId` (UUIDv7, orden de alta) para que las páginas sean estables con muchos sin serie. En `asc` los sin serie quedan al final (orden por defecto de PostgreSQL).
- Errores: 400 `ExceptionInfo` por `sort` fuera de la lista (`ParametroInvalidoException`) o por `estado` / `marcaId` con tipo inválido (ya lo resuelve `ControllerAdvisor`); 401 sin token.
- Sin N+1: count + página con la marca en el mismo fetch.

**Frontend** (feature `tires`, `/tires`):
- Debajo del `PageHeader`, una barra de filtros en fila (apilada en mobile): input de búsqueda "Número de serie" (ícono, debounce con `useDebouncedValue`), `Select` "Marca" (de `useTireBrands`, con opción "Todas"), `Select` "Estado" (estados salvo `DE_BAJA`, con opción "Todos"), `Select` "Situación operativa" (situaciones salvo `DE_BAJA`, con opción "Todas") y botón "Limpiar filtros" (visible solo con algún filtro activo). Cada cambio vuelve a `page=0` y busca solo: no hay botón "Buscar" (como HU-27).
- Tabla (`TiresTable`, mismo esqueleto que `TransportUnitsTable`): Número de serie (ordenable con `SortableTableHead`; vacío → "Sin número de serie" atenuado), Marca, Estado (`Badge`), Situación operativa, Kilometraje (formateado; null → "No registrado"). Skeleton al cargar, `Empty` con reintento ante error, `Empty` "No se encontraron neumáticos" con filtros activos (con "Limpiar filtros") y "Todavía no hay neumáticos registrados" sin filtros. `TablePagination` debajo. La fila no es clicable (HU-14).
- El alta ya invalida `["tires"]`: la query del listado vive bajo esa clave y se refresca sola.
- UI instalada que se reutiliza: `input`, `select`, `table`, `badge`, `skeleton`, `empty`, `button`, `label`, y `components/common/{page-header,sortable-table-head,table-pagination}`. Nada para instalar.

## 2. Archivos

Backend (`.../gestion_neumaticos/neumaticos/`):
- `domain/EspecificacionesNeumatico.java` — `activos`, `numeroSerieContiene`, `deMarca`, `conEstado`, `conSituacion`.
- `domain/OrdenNeumaticos.java` — lista blanca de orden.
- `domain/dto/NeumaticoListadoDTO.java` — record del listado.
- `repository/NeumaticoRepository.java` — `findAll(Specification, Pageable)` con la marca en el fetch.
- `mapper/NeumaticoMapper.java` — `toListadoDTO`.
- `service/NeumaticoService(Impl).java` — `buscar(filtros, pageable)`, `readOnly`, `warn` si el orden es inválido.
- `controller/NeumaticoController.java` — `GET` del listado.
- Tests: fixtures en `TestDataFactory`; snippet REST Docs `neumaticos-busqueda`.

Frontend (`frontend/gestion-neumaticos/src/`):
- `features/tires/api/tires-api.ts` — tipos `TireListItem`, `TiresQuery` (+ valores por defecto) y `searchTires`.
- `features/tires/hooks/use-tires.ts` — query bajo `["tires", "list", query]` con `keepPreviousData`.
- `features/tires/components/tire-filters.tsx` — barra de filtros.
- `features/tires/components/tires-table.tsx` — tabla, estados y paginación.
- `features/tires/pages/tires-page.tsx` — reemplaza el placeholder.
- `features/tires/hooks/use-register-tire.ts` — quitar el comentario "futuro listado".
- `lib/i18n/locales/{es,en}/tires.json` — `list.*`, `filters.*`, `situation.*`; quitar `empty`.

Docs: `docs/Seguridad/seguridad.md` 2.10 — fila del `GET`; `CLAUDE.md`/`docs/estado-actual.md` si listan endpoints de `neumaticos`.

## 3. Migración

Ninguna: `V10` ya da `SELECT` con `neumaticos_select USING (true)` a los tres roles sobre `neumaticos` y `marcas_neumaticos`; `V19` agregó `deleted_at`.

## 4. Desvíos de convención

- El filtro de marca va por `marcaId` interno (Long): es el id que ya expone `GET /marcas` (catálogo sin `publicId`). No es un id de entidad de negocio.
- `ROUTE_ACCESS` no cambia (decisión del humano): el Lector no ve la página aunque el endpoint lo admita.

## 5. Alternativas descartadas

- **Índice trigram (`pg_trgm`) para el `LIKE`**: requiere crear una extensión y el volumen esperado no lo justifica.
- **Botón "Buscar" explícito**: HU-27 busca al escribir; se mantiene la misma experiencia.
- **Filtrar por nombre de marca**: el id evita ambigüedad y ya llega en el `Select`.
- **Casilla "Solo en espera"** (literal de la historia): descartada por el humano; el `Select` de situación la cubre y permite filtrar también por las otras situaciones.
