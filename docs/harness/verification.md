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

### Nivel 3 — Smoke test manual (opcional pero recomendado)

Antes de cerrar la sesión, un flujo end-to-end con el stack local:

```bash
docker compose up -d postgres migraciones      # base + migraciones (seed dev)
# backend: perfil dev con las variables de .env exportadas
# frontend: npm run dev  → http://localhost:5173 (o el puerto de Vite)
```

Ingresar con un usuario de seed (`victoria.editor` / `Editor.1234`) y recorrer la
funcionalidad nueva. Dejá el entorno como estaba (`docker compose down` si lo levantaste).

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
