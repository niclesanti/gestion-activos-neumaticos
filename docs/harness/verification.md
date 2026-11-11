# Verificación

> El agente no dice "funciona", lo demuestra. La verificación completa es **una sola corrida**
> de `./verify.sh` al terminar la implementación; durante el trabajo solo se corren chequeos puntuales.

## Nivel 1 — Ciclo de desarrollo (implementer, mientras trabaja)

Solo lo mínimo para la task en curso, sin Maven completo:

```bash
cd backend/gestion-neumaticos && ./mvnw -B test -Dtest=ClaseTest   # Docker en marcha (Testcontainers)
cd frontend/gestion-neumaticos && npm run typecheck && npm run lint
```

Todo servicio, controlador, mapper y migración nuevos llevan test: camino feliz, un camino de error
(404/409/400/403…), autorización (rol mínimo y rechazo de uno inferior) y, si hay migración,
`AutorizacionBaseDatosTests` (RLS) y `ModularityTests` verdes. Controladores: `@WebMvcTest` con
`SecurityConfig` real; repositorios/RLS: Testcontainers (PostgreSQL real, nunca H2). El frontend
no tiene runner de tests: se cubre con lint + typecheck + la comprobación en navegador.

## Nivel 2 — `./verify.sh` (leader, una vez, al terminar el implementer)

Replica `.github/workflows/ci.yml` solo para lo que cambió en la rama desde que salió de `develop`:
backend `./mvnw -B verify` (tests + JaCoCo ≥ 85 %), frontend `npm ci` (si hace falta), lint, typecheck,
build y `npm audit`; más un chequeo de coherencia de `feature_list.json` y specs. Opciones:
`--backend`, `--frontend`, `--all`; base configurable con `VERIFY_BASE`. Muestra `[OK]`/`[FAIL]` y un
extracto del error; el log completo queda en `.verify-logs/` (ignorado por git). Si algo falla, se
arregla y se vuelve a correr; no se cierra con rojo. El implementer puede correrlo una vez antes de
entregar para no devolver algo roto.

## Nivel 3 — Navegador (leader, solo si cambió `frontend/`)

Si la HU es solo backend, los tests de Nivel 1/2 (`@WebMvcTest` + integración con PostgreSQL real)
son la prueba suficiente: no se hace `curl` ni se levanta el stack.

Si toca el frontend, el leader levanta el stack (`docker compose up -d --build`; requiere `.env` con
`SPRING_PROFILES_ACTIVE=dev`; al terminar, `docker compose down` sin `-v`) y recorre con la extensión
**Claude in Chrome** los pasos que dejó el implementer en `impl_<name>.md`: `http://localhost:3000`,
usuarios de seed `victoria.administrador|editor|lector` con `Admin.1234` / `Editor.1234` /
`Lector.1234`. Revisa consola y requests (`read_console_messages`, `read_network_requests`) y prueba
también el rol que **no** debe ver la acción. Nunca se escriben tokens en `progress/`. Si no se puede
probar, se anota por qué y el humano decide si se cierra igual.

## Trazabilidad CA ↔ verificación

Una sola matriz, en `progress/impl_<name>.md`:

```markdown
## Trazabilidad
- CA1 → `NeumaticoControllerTest.crear_devuelve201ConLocation`
- CA3 → (frontend) lint + typecheck + navegador: la tabla se refresca tras el alta
```

## Anti-patrones

- ❌ "Agregué el endpoint, debería funcionar." → falta test ejecutable.
- ❌ Test que solo verifica que no lanza excepción → debe comprobar status, cuerpo, header o estado persistido.
- ❌ H2 o mocks de la base en tests de integración.
- ❌ Bajar el umbral de JaCoCo, excluir clases, `@SuppressWarnings` o `eslint-disable` para "pasar".
- ❌ Editar una migración Flyway existente → se crea una nueva.
- ❌ Cerrar la feature sin `./verify.sh` verde.
- ❌ Correr `verify.sh` completo en cada edición o al arrancar la sesión.
