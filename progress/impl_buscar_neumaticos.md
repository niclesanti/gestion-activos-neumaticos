# Implementación HU-28 buscar_neumaticos

## Resumen

Backend: `GET /api/v1/neumaticos?numeroSerie&marcaId&estado&situacion&page&size&sort` (`hasRole('LECTOR')`).
Filtra siempre sobre activos (sin baja lógica, `estadoActual` y `situacionOperativaVigente` distintos de `DE_BAJA`), serie por
contenido, sin distinguir mayúsculas y con comodines escapados. Orden: solo `numeroSerie` (por defecto asc) con desempate fijo por
`publicId`; otro campo -> 400 `ParametroInvalidoException`. Marca en el mismo fetch (`@EntityGraph`), sin N+1. Sin migración.

Frontend: `/tires` reemplaza el placeholder por barra de filtros (serie con debounce, Marca, Estado, Situación operativa, "Limpiar
filtros"), tabla (`TiresTable`), paginación `size=10`. Sin componentes shadcn nuevos; `ROUTE_ACCESS` sin cambios.

Archivos backend (`neumaticos/`): `domain/EspecificacionesNeumatico`, `domain/OrdenNeumaticos`, `domain/dto/NeumaticoListadoDTO`,
`mapper/NeumaticoMapper.toListadoDTO`, `repository/NeumaticoRepository.findAll(spec, pageable)`, `service/NeumaticoService(Impl).buscar`,
`controller/NeumaticoController.buscar`.
Tests: `OrdenNeumaticosTest`, `NeumaticoMapperTest`, `NeumaticoServiceImplTest` (nested `buscar`), `NeumaticoControllerTest`
(nested `Buscar`, snippet REST Docs `neumaticos-busqueda`), `BusquedaNeumaticoIntegrationTests`; fixture `TestDataFactory.neumaticoListado(Neumatico)`.
Archivos frontend: `features/tires/{api/tires-api.ts, hooks/use-tires.ts, hooks/use-register-tire.ts (comentario), components/tire-filters.tsx,
components/tires-table.tsx, pages/tires-page.tsx}`, `lib/i18n/locales/{es,en}/tires.json` (`situation`, `filters`, `list`; se quitó `empty`).
Docs: fila del `GET` en `docs/Seguridad/seguridad.md` 2.10.

Verificación puntual (Maven con JDK 21): `OrdenNeumaticosTest, NeumaticoMapperTest, NeumaticoServiceImplTest, NeumaticoControllerTest,
BusquedaNeumaticoIntegrationTests, ModularityTests` en verde; `npm run lint` y `npm run typecheck` en verde. NO se corrió `./verify.sh`
ni `mvn verify` (lo hace el leader).

## Trazabilidad

| CA | Verificación |
|----|--------------|
| CA1 | `BusquedaNeumaticoIntegrationTests.parcialSinDistinguirMayusculas`, `comodinesSeBuscanLiterales`, `conSerieNoDevuelveSinSerie` |
| CA2 | `BusquedaNeumaticoIntegrationTests.combinaFiltros` |
| CA3 | `BusquedaNeumaticoIntegrationTests.filtraPorSituacion`, `deBajaYMarcaInexistente_vacio` |
| CA4 | `BusquedaNeumaticoIntegrationTests.sinFiltros_devuelveTodosLosActivos`; `NeumaticoControllerTest.Buscar.sinParametros_usaOrdenYTamanioPorDefecto` |
| CA5 | `BusquedaNeumaticoIntegrationTests.pagina`; `NeumaticoControllerTest.Buscar` (paginación, `buscaConLector`) |
| CA6 | `BusquedaNeumaticoIntegrationTests.excluyeLosInactivos` (baja lógica, estado y situación `DE_BAJA`) |
| CA7 | `BusquedaNeumaticoIntegrationTests.enEsperaSeFiltraPorSituacionVigente` |
| CA8 | `BusquedaNeumaticoIntegrationTests.pagina` y `cargaLaPaginaSinNMasUno`; `NeumaticoControllerTest.Buscar.tamanioMayorAlMaximo_seAcota`, `ordenNoPermitido`, `filtroConTipoInvalido`; `NeumaticoServiceImplTest.Buscar`; `OrdenNeumaticosTest` |
| CA9 | `NeumaticoControllerTest.Buscar.buscaConLector`, `buscaConEditorYAdministrador`, `sinToken`; `BusquedaNeumaticoIntegrationTests.lectorLeeConSuRolDeBase` |
| CA10 | Verificación de UI (ver abajo): petición con todos los filtros y `page=0`; lint + typecheck |
| CA11 | Verificación de UI (ver abajo): "N neumáticos · página X de Y", Anterior/Siguiente, `size=10` |

## Pasos para el navegador

Usuario `victoria.editor` / `Editor.1234` (también `victoria.administrador`), ruta `/tires`.
1. Con todos los filtros vacíos: Network muestra `GET /api/v1/neumaticos?page=0&size=10&sort=numeroSerie,asc` sin parámetros de filtro (CA10, CA4). No aparece "Limpiar filtros".
2. Escribir parte de un número de serie en cualquier mayúscula/minúscula: tras el debounce pide `numeroSerie=...&page=0`; la tabla muestra solo coincidencias; aparece "Limpiar filtros".
3. Elegir Marca, Estado y Situación operativa: cada cambio pide con todos los filtros completos y `page=0`; las listas de Estado y Situación no ofrecen "De baja"; la opción "Todos/Todas" quita el filtro.
4. Registrar un neumático con "Registrar neumático": el listado se refresca solo; el nuevo queda "En espera" (filtrando Situación = En espera aparece).
5. Sin coincidencias: "No se encontraron neumáticos" con botón "Limpiar filtros". Sin neumáticos ni filtros: "Todavía no hay neumáticos registrados".
6. Con más de 10 resultados: pie "N neumáticos · página X de Y", Anterior deshabilitado en la primera, Siguiente en la última (CA11); clic en el encabezado "Número de serie" invierte el orden y vuelve a la página 1.
7. Fila sin serie: "Sin número de serie" atenuado; kilometraje null: "No registrado".
8. Rol que NO debe ver la acción/página: `victoria.lector` sigue redirigido (la ruta `/tires` no cambió; endpoint sí lo admite, CA9).

## Bloqueos

Ninguno. Notas:
- `tasks.md`: marqué las checkboxes `[x]` (no toqué su contenido).
- T9: solo agregué la fila a `docs/Seguridad/seguridad.md`; `CLAUDE.md` no lista endpoints de `neumaticos` y `docs/estado-actual.md` lo actualiza el leader al cerrar.
- La firma del servicio es `buscar(numeroSerie, marcaId, estado, situacion, pageable)` (sin objeto de filtros), igual que HU-27.

## Verificación del leader (2026-11-18)

- **Backend:** `mvn -B clean verify` con JDK 21 (equivale a `./mvnw -B verify`; el wrapper no descarga Maven en esta máquina por `CRYPT_E_NO_REVOCATION_CHECK`): `[OK]` — 742 tests, 0 fallas, cobertura JaCoCo cumplida.
- **Frontend (`./verify.sh --frontend`):** `npm run lint` `[OK]`, `npm run typecheck` `[OK]`, `npm run build` `[OK]`, `npm audit --omit=dev --audit-level=high` **`[FAIL]`**: 4 vulnerabilidades altas en la cadena `picomatch` → `fdir` → `tinyglobby` → `vite`. No las causa esta HU (no se tocó `package.json` ni `package-lock.json`): es un aviso de seguridad nuevo sobre dependencias existentes. La corrección automática (`npm audit fix --force`) instalaría `vite@6.2.7` (retroceso, cambio incompatible). **Pendiente de decisión de la humana.**

## Prueba en el stack real

Stack reconstruido con `docker compose up -d --build`; Chrome (puppeteer-core) contra `http://localhost:3000`, usuario `victoria.editor`. Datos de prueba: 14 neumáticos creados por API (13 con serie `E2E28-001..013` y 1 sin serie, estados Nuevo usado / Hombros, marcas Fate / Bridgestone), **borrados al terminar** (la base quedó con 0 neumáticos, como antes).

| Paso | Resultado |
|---|---|
| Sin filtros | `GET ?page=0&size=10&sort=numeroSerie,asc`, sin parámetros de filtro; no aparece "Limpiar filtros"; "14 neumáticos · página 1 de 2" (CA4, CA10, CA11) |
| Página siguiente | `page=1`; 4 filas; "página 2 de 2"; Siguiente deshabilitado, Anterior habilitado (CA11) |
| Serie parcial en minúsculas `e2e28-00` | `numeroSerie=e2e28-00&page=0`; 9 resultados; aparece "Limpiar filtros" (CA1) |
| + Estado = Hombros | `numeroSerie=…&estado=HOMBROS`; 4 resultados, todos Hombros (CA2) |
| Marca = Fate | `marcaId=51`; 7 filas, todas Fate (CA2) |
| Situación = En stock / En espera | `situacion=EN_STOCK` → "No se encontraron neumáticos"; `situacion=EN_ESPERA` → 4 resultados (CA3) |
| Opciones de los desplegables | Estado y Situación **no** ofrecen "De baja"; Marca lista Bridgestone, Fate, Pirelli |
| Sin coincidencias + "Limpiar filtros" | "No se encontraron neumáticos" con botón; al limpiar vuelve el listado completo (desde caché) |
| Orden por "Número de serie" | clic → `sort=numeroSerie,desc`, vuelve a página 1; sin serie primero en desc y último en asc (CA8) |
| Fila sin serie / kilometraje null | "Sin número de serie" atenuado / "No registrado" |
| Lector en `/tires` | redirigido a `/home`, 0 peticiones al listado (la ruta sigue cerrada, decisión de la humana) |
| Consola del navegador | sin errores |

Nota menor: el kilometraje de 4 cifras se muestra sin separador de miles ("1000 km") y desde 5 cifras con punto ("10.000 km"): es el comportamiento estándar de `Intl` en español, no un defecto.
