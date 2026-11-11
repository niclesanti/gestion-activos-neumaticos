# Smoke test (curl) — HU-27

Stack: `docker compose up -d --build` (backend y frontend reconstruidos con los cambios). Usuarios del seed dev; no se registran tokens. Se crearon 12 unidades de prueba como `victoria.editor` (todas 201), en total 18 unidades en la base de dev.

| Caso | Resultado |
|---|---|
| `victoria.lector` `GET /api/v1/unidades-transporte` | 200, `page.size = 10`, `totalElements = 18`, `totalPages = 2`; cada elemento trae solo `publicId`, `patente` y `configuracion{nombre, tipoUnidad}` (sin slots) |
| `?patente=ab%201` | 200, 1 coincidencia (`AB123CD`) |
| `?patente=QQQ` | 200, `content` vacío, `totalElements = 0` |
| `?size=500` | 200, `page.size = 20` (acotado) |
| `?sort=id,asc` | 400 `ExceptionInfo` ("Solo se puede ordenar por patente") |
| sin token | 401 `ExceptionInfo` |
| `victoria.administrador` `GET /api/v1/proveedores/auditoria` | 200, `page.size = 10` |
| ídem con `?size=500` | 200, `page.size = 20` |

El smoke del navegador (CA10 a CA15 y CA17) queda para el leader: pasos en `progress/impl_buscar_unidades_transporte_por_patente.md`.

# Smoke test (navegador, Claude in Chrome) — HU-27

- **Fecha:** 2026-11-09 · **Ejecutó:** leader · **Stack:** `docker compose` (build nueva), `http://localhost:3000`, usuarios del seed dev.
- **Resultado global:** ✅ PASA. Sin errores en consola. Capturas de pantalla no disponibles (el renderer no respondió a `captureScreenshot`); verificado con texto de página, árbol de accesibilidad y requests de red.

| Paso | CA | Rol | Verificación | Resultado |
|---|---|---|---|---|
| Carga inicial con búsqueda vacía | CA11 | editor | `GET /api/v1/unidades-transporte?page=0&size=10&sort=patente,asc` → 200, sin `patente`; 10 filas ordenadas, "18 unidades · página 1 de 2" | ✅ |
| Paginación | CA13 | editor | Anterior deshabilitado en página 1; Siguiente → `page=1` → 8 filas, "página 2 de 2" | ✅ |
| Orden por patente | CA13 | editor | Clic en encabezado → `sort=patente,desc`, vuelve a `page=0`, orden invertido | ✅ |
| Búsqueda parcial `b12` | CA10 | editor | Una sola request tras el debounce (`patente=b12&page=0`); muestra `AB123CD · Tractor común · Tractor` | ✅ |
| Sin resultados `qqq` | CA12 | editor | "No se encontraron unidades de transporte" + "Revisá la patente o probá con menos caracteres." | ✅ |
| Normalización `xq z-9` | CA10 | editor | Encuentra `XQZ992` (ignora espacio y guion, case-insensitive) | ✅ |
| Alta con búsqueda activa (`sm`) | CA15 | editor | Diálogo → `SMK001` Semirremolque → `POST` 201 → refetch automático; la tabla muestra `SMK001 · Semirremolque · Semirremolque` sin recargar | ✅ |
| Lector | CA14 | lector | "Unidades de transporte" en sidebar y navegación móvil; lista y busca (`xqz` → 6); sin botón "Registrar unidad" | ✅ |
| Auditoría: tamaño de página | CA16/CA17 | admin | Usuarios y Unidades de transporte piden `size=10`; UT: "19 registros · página 1 de 2" | ✅ |
| Auditoría: paginación | CA17 | admin | Siguiente → `page=1`, "página 2 de 2", Siguiente deshabilitado | ✅ |
| Auditoría: orden | CA17 | admin | Creado → `sort=createdAt,desc` → `createdAt,asc` (vuelve a `page=0`) | ✅ |
| Auditoría: búsqueda | CA17 | admin | `SMK` → `q=SMK&page=0` → 1 registro (creado por @victoria.editor) | ✅ |
| Auditoría: ficha | CA17 | admin | Clic en `SMK001` abre la ficha de solo lectura con la línea de tiempo (Creación) | ✅ |

Observación menor (no es defecto): el diálogo de alta solo ofrece configuraciones del seed; en los datos de prueba del implementer casi todas las unidades son "Tractor común".
