# HU-27 - Buscar unidades de transporte por patente

**Historia**: Como usuario del sistema (cualquier nivel de acceso), quiero buscar unidades tractoras y semirremolques ingresando total o parcialmente su patente, para localizar rápidamente una unidad de transporte específica sin recorrer el listado completo.

**Criterios de aceptación originales** (`feature_list.json`, literales):

1. Dado que un usuario ingresa una cadena parcial de caracteres en el campo de búsqueda "patente", entonces el sistema debe retornar todas las unidades de transporte activas cuya patente contenga dicha cadena, sin distinguir mayúsculas de minúsculas.
2. Dado que el campo de búsqueda se deja vacío, cuando se ejecuta la búsqueda, entonces el sistema debe retornar el listado completo de unidades de transporte activas.
3. Dado que ninguna unidad coincide con el criterio ingresado, entonces el sistema debe informar que no se encontraron resultados.
4. La lista de resultados debe soportar paginación.

**Criterios de Aceptación** (numerados; entre corchetes, el criterio original y la frase de la historia que cubre cada uno):

### Backend (contrato HTTP de `GET /api/v1/unidades-transporte`)

- CA1: Dado que existen unidades activas con patentes `AB123CD`, `AB456EF` y `XYZ789`, cuando un usuario autenticado solicita `GET /api/v1/unidades-transporte?patente=b12`, entonces el sistema responde 200 con una página cuyo `content` contiene solo la unidad `AB123CD` (la coincidencia es parcial, en cualquier posición de la patente, y sin distinguir mayúsculas de minúsculas); cada elemento trae solo `publicId`, `patente` y `configuracion` con `nombre` y `tipoUnidad` (sin slots). [orig. 1]
- CA2: Dado que existe una unidad dada de baja (`deleted_at` no nulo) cuya patente contiene el término buscado, cuando un usuario solicita la búsqueda con ese término, entonces esa unidad no aparece en el `content` ni se cuenta en `page.totalElements`. [orig. 1, "activas"]
- CA3: Dado que existen unidades activas y una dada de baja, cuando un usuario solicita `GET /api/v1/unidades-transporte` sin el parámetro `patente`, o con `patente` vacío o solo espacios, entonces el sistema responde 200 con todas las unidades activas (y ninguna dada de baja). [orig. 2]
- CA4: Dado que ninguna unidad activa contiene el término buscado, cuando un usuario solicita la búsqueda, entonces el sistema responde 200 con `content` vacío y `page.totalElements = 0`. [orig. 3]
- CA5: Dado que existen más unidades activas que el tamaño de página, cuando un usuario solicita `GET /api/v1/unidades-transporte?page=<n>&size=<s>` (con `<s>` ≤ 20), entonces el sistema responde 200 con a lo sumo `<s>` unidades, ordenadas por patente ascendente por defecto, y con los metadatos `page.number`, `page.size`, `page.totalElements` y `page.totalPages`. Sin `size`, la página es de 10 unidades. [orig. 4]
- CA6: Dado que un usuario pide un tamaño de página mayor a 20 (por ejemplo `size=500`), cuando solicita el listado, entonces el sistema responde 200 con la página acotada a 20 elementos (`page.size = 20`). [orig. 4]
- CA7: Dado que un usuario pide ordenar por un campo que no es `patente` (por ejemplo `sort=id,asc`), cuando solicita el listado, entonces el sistema responde 400 con un cuerpo `ExceptionInfo`. [orig. 4]
- CA8: Dado un usuario con nivel Lector, Editor o Administrador, cuando solicita `GET /api/v1/unidades-transporte`, entonces el sistema responde 200 (también contra PostgreSQL real, con el rol de base `gn_lector`). ["cualquier nivel de acceso"]
- CA9: Dado un usuario sin sesión (sin token o con token inválido), cuando solicita `GET /api/v1/unidades-transporte`, entonces el sistema responde 401 con un cuerpo `ExceptionInfo`. ["usuario del sistema"]

### Frontend (sección Unidades de transporte, `/transport-units`)

- CA10: Dado un usuario en `/transport-units`, cuando escribe en el campo de búsqueda "Patente", entonces, tras una pausa de tipeo (debounce), la interfaz dispara `GET /api/v1/unidades-transporte` con el parámetro `patente` igual al texto ingresado (recortado) y `page=0`, y la tabla muestra por cada resultado su patente, su configuración, y su tipo (Tractor / Semirremolque). En esta historia hacer clic en una fila no hace nada (la ficha detalle es HU-10). [orig. 1]
- CA11: Dado un usuario en `/transport-units` con el campo de búsqueda vacío (al entrar o tras borrarlo), entonces la interfaz pide el listado sin el parámetro `patente` y la tabla muestra todas las unidades activas de la primera página. [orig. 2]
- CA12: Dado que la búsqueda con un término devuelve `content` vacío, entonces la interfaz muestra el estado vacío "No se encontraron unidades de transporte" con la sugerencia de revisar la patente, en lugar de la tabla. Si no hay término y no existe ninguna unidad, muestra en cambio "Todavía no hay unidades de transporte registradas". [orig. 3]
- CA13: Dado que el resultado tiene más de una página (más de 10 unidades), entonces la interfaz muestra debajo de la tabla el resumen "N unidades · página X de Y" y los botones Anterior / Siguiente, deshabilitados en la primera y la última página respectivamente; la interfaz pide `size=10`, cambiar de página pide la `page` correspondiente, y cambiar el término de búsqueda vuelve a `page=0`. [orig. 4]
- CA14: Dado un usuario con nivel Lector, cuando inicia sesión, entonces ve el ítem "Unidades de transporte" en la navegación, puede entrar a `/transport-units` y ver el listado, y no ve el botón "Registrar unidad". ["cualquier nivel de acceso"]
- CA15: Dado un usuario Editor o Administrador en `/transport-units`, cuando registra una unidad nueva con el diálogo de alta, entonces el listado se vuelve a pedir y la unidad nueva aparece en la tabla sin recargar la página. [consistencia del listado con HU-07]

### Criterio de paginación unificado (efecto sobre la consulta de auditoría, HU-43)

- CA16: Dado un Administrador, cuando solicita cualquier `GET /api/v1/<recurso>/auditoria` sin `size`, entonces el sistema responde 200 con páginas de 10 registros; y si pide `size` mayor a 20, la página se acota a 20 (`page.size = 20`). [decisión del humano: 10/20 para todos los listados]
- CA17: Dado un Administrador en `/audit`, cuando usa la búsqueda, ordena por Creado / Última modificación y pasa de página, entonces la pantalla se comporta igual que antes de esta historia (mismo resumen, mismos botones Anterior / Siguiente, mismo indicador de orden), ahora con los componentes compartidos de paginación y encabezado ordenable, y pidiendo `size=10`. [regresión de la extracción de componentes]

**Notas / Supuestos**:

- **Unidad activa** = no dada de baja lógicamente (`deleted_at IS NULL`, columnas de auditoría de `V19`, HU-43). Hoy ninguna historia da de baja unidades (no hay `UPDATE` sobre la tabla), así que en los tests la baja se simula insertando la fila con `deleted_at`/`deleted_by` como owner de la base.
- **Normalización del término**: el término se normaliza igual que la patente en el alta (`domain.Patentes.normalizar`: mayúsculas, sin espacios ni guiones), así `ab 1`, `AB-1` y `ab1` encuentran `AB123CD`. Es la forma de cumplir "sin distinguir mayúsculas de minúsculas" sobre patentes guardadas normalizadas; se verifica dentro de CA1. Los comodines de `LIKE` (`%`, `_`, `\`) se buscan literales.
- **Paginación**: query params estándar de Spring Data (`page`, desde 0; `size`, por defecto 10, máximo 20; `sort`, por defecto `patente,asc`, único campo permitido `patente`). La respuesta es `Page<UnidadTransporteListadoDTO>` (DTO propio del listado, solo con lo que muestra la tabla) serializada `VIA_DTO` (`content` + `page{size, number, totalElements, totalPages}`), igual que la consulta de auditoría. CA6 y CA7 no figuran literalmente en la HU: son la parte de contrato de "soportar paginación" que el backend tiene que acotar (tope de tamaño y lista blanca de orden, como en HU-43).
- **Criterio de paginación unificado (decisión del humano)**: 10 por defecto y 20 como máximo pasan a valer para **todos** los listados paginados, incluida la consulta de auditoría de HU-43 (hoy 20 por defecto y tope 100). El cambio de auditoría es un efecto colateral deliberado de esta historia y se verifica con sus tests existentes ajustados y en el smoke test (ver `design.md`).
- **"Cualquier nivel de acceso"**: el endpoint va con `@PreAuthorize("hasRole('LECTOR')")` (por la jerarquía de roles, los tres). En la base no hace falta cambiar nada: `V16` ya da `SELECT` y la política `unidades_transporte_select USING (true)` a los tres roles. En la interfaz, el Lector hoy no entra a `/transport-units` (`ROUTE_ACCESS`); esta historia le abre la sección en solo lectura (ver `design.md`).
- **CA15** no es un criterio literal de HU-27: asegura que el listado nuevo no quede desactualizado frente al alta existente (HU-07). La invalidación ya existe en `useRegisterTransportUnit`; el criterio verifica que el listado la reciba.
- **Fuera de alcance**: ficha detalle de la unidad (HU-10; la fila no navega a ningún lado todavía), filtros por tipo o configuración, modificación y baja de unidades, búsqueda de neumáticos (HU-28).
- El frontend no tiene runner de tests: CA10 a CA15 se verifican con lint + typecheck + build y la prueba en el navegador con Claude in Chrome que hace el leader (pasos en `tasks.md`).
