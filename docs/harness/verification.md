# Verificación — Cómo demostrar que el trabajo funciona

> Regla de oro: **el agente no dice "funciona", lo demuestra**.
> Toda feature termina con evidencia ejecutable, no con afirmaciones.
> `./init.sh` ejecuta exactamente lo mismo que el CI (`.github/workflows/ci.yml`).

## Niveles de verificación

### Nivel 1 — Backend: tests y cobertura (obligatorio si se toca el backend)

Desde `backend/gestion-neumaticos/` (requiere Docker en marcha, Testcontainers):

```bash
./mvnw -B verify                 # todos los tests + JaCoCo (LINE y BRANCH >= 85 %)
./mvnw test -Dtest=ClassName     # una sola clase mientras iterás
```

Todo servicio, controlador, mapper y migración nuevos tienen test:

1. Camino feliz.
2. Al menos un camino de error (404, 409, 400, 403…) si puede fallar.
3. Autorización: el rol mínimo exigido y el rechazo de uno inferior.
4. Si hay migración: `AutorizacionBaseDatosTests` (RLS en toda tabla) debe seguir verde.
5. `ModularityTests` verde (fronteras de módulo).

### Nivel 2 — Frontend: calidad estática (obligatorio si se toca el frontend)

Desde `frontend/gestion-neumaticos/`:

```bash
npm run lint
npm run typecheck
VITE_API_URL=https://api.example.com npm run build
npm audit --omit=dev --audit-level=high
```

El frontend **no tiene runner de tests**: estas cuatro verificaciones son el
mínimo, y se completan con una comprobación manual concreta del comportamiento
(que el implementer documenta). No agregues Vitest/Jest dentro de otra feature.

### Nivel 3 — Prueba en el stack real (obligatorio; el método depende de qué se tocó)

Los tests y el CI no alcanzan: antes de pedir el review hay que ver la funcionalidad
andando contra el stack real. El método depende del alcance de la feature:

| Alcance de la feature | Cómo se prueba |
|-----------------------|----------------|
| **Solo backend**      | API con `curl` (o similar) contra el backend levantado |
| **Solo frontend**     | Navegador con la extensión **Claude in Chrome** |
| **Frontend + backend**| Navegador con Claude in Chrome (recorre el flujo completo, que ejercita la API) y, además, `curl` para los casos que la UI no permite (403 por rol, 400, 409) |

**Levantar el stack** (desde la raíz; reconstruye las imágenes con tu código):

```bash
docker compose up -d --build     # postgres + migraciones (con seed dev) + backend + frontend
# API: http://localhost:8080   Frontend: http://localhost:3000   Swagger: /swagger-ui.html
docker compose logs -f backend   # si algo falla
```

Requiere el `.env` de la raíz (ver `.env.example`) con `SPRING_PROFILES_ACTIVE=dev`.
Si ya está levantado, `--build` solo recrea lo que cambió. Al terminar,
`docker compose down` (sin `-v`, para no perder el volumen del usuario).

**Backend con curl.** Usuarios de seed: `victoria.administrador|editor|lector`
con `Admin.1234` / `Editor.1234` / `Lector.1234` (el campo del login es `identifier`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login   -H "Content-Type: application/json"   -d '{"identifier":"victoria.editor","password":"Editor.1234"}' | node -p "JSON.parse(require('fs').readFileSync(0,'utf8')).token")

curl -i -X GET  http://localhost:8080/api/v1/<recurso> -H "Authorization: Bearer $TOKEN"
curl -i -X POST http://localhost:8080/api/v1/<recurso> -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json" -d '{...}'
```

Probá como mínimo, por cada endpoint nuevo o modificado: el camino feliz (status y
`Location` correctos), un error de validación o de negocio (400/404/409), **sin token (401)**
y **con un rol insuficiente (403)**. Nunca imprimas ni guardes tokens en `progress/`.

**Frontend con Claude in Chrome.** Cargá las herramientas `mcp__claude-in-chrome__*`
(skill `claude-in-chrome`), abrí una pestaña nueva en `http://localhost:3000`, iniciá sesión
con el usuario de seed del rol que corresponda y recorré el flujo de cada `CA<n>` de UI:
captura de pantalla del resultado, y revisá la consola (`read_console_messages`) y las
requests (`read_network_requests`) buscando errores. Probá también el rol que **no** debe
ver la acción. Esta prueba la ejecuta el `leader` (sesión principal, que tiene las herramientas
del navegador) cuando el implementer termina y **antes** de lanzar al reviewer; el
implementer deja en `progress/impl_<name>.md` la lista de pasos a recorrer. La prueba con
`curl` de una feature solo-backend la hace el propio implementer.

**Si no se puede probar** (Docker caído, extensión no conectada, falta el `.env`): no lo
saltees en silencio. Anotá en `progress/impl_<name>.md` qué no se pudo probar y por qué, y
avisale al humano; él decide si se cierra igual.

El resultado (qué se probó, qué respondió, qué se vio) se escribe en
`progress/smoke_<name>.md`: el reviewer lo exige antes de aprobar y el `CA<n>` cubierto por esta
prueba se cita en la trazabilidad.

### Nivel 4 — Trazabilidad de criterios de aceptación (obligatorio para features con `"sdd": true`)

Cada `CA<n>` de `specs/<name>/historia-usuario.md` debe poder mapearse a al
menos un test concreto (backend) o a la verificación documentada (UI sin
runner de tests, ver `docs/harness/specs.md`). El reviewer rechaza si falta cobertura.

```markdown
## Trazabilidad
- CA1 → `NeumaticoControllerTest.crear_devuelve201ConLocation`
- CA2 → `NeumaticoServiceImplTest.crear_conSerieDuplicada_lanzaEntidadDuplicada`
- CA3 → (frontend) lint + typecheck + verificación manual descrita
```

## Anti-patrones (no hacer)

- ❌ "He añadido el endpoint, debería funcionar." → falta test ejecutable.
- ❌ Test que solo verifica que no lanza excepción. → tiene que comprobar el
  resultado concreto (status, cuerpo, header, estado persistido).
- ❌ H2 o mocks de la base en tests de integración. → PostgreSQL real vía Testcontainers.
- ❌ Bajar el umbral de JaCoCo, excluir clases de la cobertura, `@SuppressWarnings`
  o `eslint-disable` para "pasar".
- ❌ Editar una migración Flyway ya existente. → se crea una nueva.
- ❌ Marcar la feature como `done` sin pasar `./init.sh`.

## Verificación final antes de cerrar

```bash
./init.sh           # debe terminar con [OK] Entorno listo
```

Variables opcionales de `init.sh` para iterar más rápido (**no** valen para cerrar
una feature; el cierre exige la corrida completa):

- `INIT_SKIP_BACKEND=1` — omite `./mvnw verify`.
- `INIT_SKIP_FRONTEND=1` — omite lint / typecheck / build / audit.

Si `./init.sh` está rojo, **no** marques nada como `done`. Anota el bloqueo
en `progress/current.md` con estado `blocked` en `feature_list.json`.
