# Pruebas de seguridad: autenticación y autorización

Análisis de vulnerabilidades y evaluación **OWASP Top 10:2025** del flujo de login/logout (HU-01, HU-02) y la autorización por rol. Complementa [`seguridad.md`](seguridad.md) (decisiones de diseño) y su lista de [Pendiente](seguridad.md#pendiente). **Fecha: 2026-10-04. Rama: `feature/login`.**

> Las pruebas se corrieron contra el stack de `compose.yaml` en perfil **dev** (usuarios seed `administrador`/`editor`/`lector`). Metodología: análisis estático del código, una auditoría independiente en paralelo y pruebas dinámicas con tokens manipulados (`node`+`openssl`), `curl` y `psql` como `gn_app`. No se modificó código ni datos.

> **Estado actual (2026-10-04, después de las correcciones):** todos los hallazgos están corregidos o quedan como configuración de despliegue documentada. Ver [Re-test después de las correcciones](#re-test-después-de-las-correcciones-2026-10-04). Las secciones siguientes describen el estado **anterior** a las correcciones y se conservan como registro de la auditoría.

---

## Resumen ejecutivo

El **núcleo criptográfico y de autorización es sólido**: ningún token manipulado (rol, `sub`, `alg:none`, otra clave, sin `jti`) fue aceptado; RLS y los privilegios mínimos de la base funcionan; el login no permite enumerar cuentas ni inyección. **El diseño de "defensa en capas" está bien pensado.**

Las debilidades son de **operación y robustez**, no de diseño criptográfico: falta rate limiting, TLS, cabeceras HTTP de seguridad, y hay dos problemas de disponibilidad/configuración nuevos de severidad alta (DoS en el login, credenciales de superusuario en el runtime del backend).

| OWASP 2025 (≈2021) | Resultado | Motivo |
|---|:--:|---|
| A01 Broken Access Control | ⚠ | RLS y 3 capas OK; rol del token no se re-valida hasta 8 h (H5). Aún sin endpoints de negocio. |
| A02 Security Misconfiguration | ✘ | Sin cabeceras/CSP (H3), puertos 5432/5050 en `0.0.0.0` (H6), Swagger en prod (H7), superusuario en runtime (N2). |
| A03 Supply Chain | ⚠ | Versiones fijadas; sin escaneo en CI (H12); `pgadmin:latest` (H6). `npm audit` prod: 0. |
| A04 Cryptographic Failures | ⚠ | HS256+Argon2id correctos; **sin TLS** (H2); secreto como texto crudo (N5). |
| A05 Injection | ✔ | JPA/consultas parametrizadas; `set_config` con lista blanca y `PreparedStatement`; `SECURITY DEFINER` con `search_path=''`. |
| A06 Insecure Design | ⚠ | **DoS en el login (N1)**; token no revocado al cerrar pestaña (H4). |
| A07 Authentication Failures | ✘ | **Sin rate limiting (H1)**. Sí: mensaje genérico, hash dummy, revocación en logout. |
| A08 Data Integrity | ✔ | JWT firmado, algoritmo fijo, sin deserialización insegura expuesta. |
| A09 Logging & Alerting | ✘ | Login fallido sin IP ni identificador (H9); el 500 de N3 inunda logs. |
| A10 Exceptional Conditions | ⚠ | 500 genérico OK, pero **4xx del framework caen en 500 con stack (N3)** y H8. |

**Veredicto:** en su estado actual **no pasa OWASP Top 10** (falla A02, A07, A09; parcial en A01/A04/A06/A10). Ninguna falla es un defecto conceptual; todas tienen corrección conocida y acotada. Para un TFI el diseño es destacable; antes de producción deben cerrarse los ítems de prioridad **Alta**.

---

## Las dos preguntas puntuales

### P1 — ¿Se puede robar el token de sesión y entrar con el mismo? → **Sí.**

Es un *bearer token*: no está vinculado a dispositivo, IP ni cliente. **Comprobado (TA-02):** el token de `lector` leído del navegador funciona igual desde `curl` → `GET /api/auth/me` responde **200** con los datos del usuario. Es un **riesgo aceptado y documentado** ([`seguridad.md` 3.1](seguridad.md#31-dónde-se-guarda-el-token)), pero con agravantes:

- **Vectores de robo abiertos:** XSS (el token está en `sessionStorage`, legible por JS; **no hay CSP ni cabeceras** — TA-05), **tráfico en claro sin TLS** (H2), devtools/extensiones.
- **Agravante (H4):** cerrar la pestaña borra el `sessionStorage` pero **no revoca el token**; sigue válido hasta 8 h. Solo el logout explícito lo invalida — y eso **sí funciona** (TA-04: tras logout, el token da 401).
- **Agravante (H5):** un usuario degradado o dado de baja conserva su rol hasta que el token vence.

**Mitigaciones:** TLS+HSTS, CSP estricta y cabeceras (`X-Content-Type-Options`, `Referrer-Policy`, `frame-ancestors`), token más corto + *refresh token* rotativo, `version_token` por usuario, y revocar en `pagehide` con `navigator.sendBeacon`.

### P2 — ¿Se puede cambiar el rol en el token y acceder como otro rol? → **No, salvo que se filtre `JWT_SECRET`.**

Todas las manipulaciones fueron **rechazadas con 401** (ver tabla TB). El `JwtDecoder` fija `MacAlgorithm.HS256`, exige `iss` y valida `jti`, así que alterar el payload rompe la firma, `alg:none` y la confusión de algoritmos no funcionan, y firmar con otra clave falla.

**Único camino real — comprobado (TB-07):** con el secreto se forja un token ADMINISTRADOR y `/me` responde **200** como administrador. Es el riesgo residual de HS256:

- El secreto se usa como **bytes UTF-8 del texto** (`JwtConfig:31-35`). El valor de dev tiene 79 caracteres base64 sin espacios (buena entropía); aun así conviene exigir base64 de 32 bytes aleatorios y decodificarlo (**N5**), porque una frase humana se craquea offline con un solo token.
- **Defensa en profundidad débil:** el rol de base lo toma `RolBaseDatosDataSource` **del claim del token**, no de `usuarios.nivel_acceso`. Con el secreto filtrado, el atacante obtiene también el rol de BD. El `sub` falso, en cambio, queda acotado por RLS.

**En el frontend:** cambiar `accessLevel` en `sessionStorage`/memoria solo altera la UI; `useSessionSync` lo corrige con `/me` al recargar y **no hay endpoints de negocio** que dependan de él. No da acceso real.

---

## Resultados de las pruebas

Leyenda: ✔ comportamiento seguro/esperado · ✘ hallazgo.

### TB — Manipulación de token (todas re-firmadas o alteradas desde un token real de `lector`)

| ID | Caso | Resultado | |
|---|---|:--:|---|
| TB-01 | `nivelAcceso`→ADMIN con firma original | **401** | ✔ |
| TB-02 | `alg:none` sin firma | **401** | ✔ |
| TB-03 | `alg:HS512` firmado con el secreto | **401** | ✔ (algoritmo fijo) |
| TB-04 | HS256 firmado con otra clave de 32 bytes | **401** | ✔ |
| TB-05a/b/c | `iss` ajeno / `jti` no-UUID / sin `jti`, re-firmados | **401** | ✔ |
| TB-06 | `sub`→admin con firma original | **401** | ✔ |
| TB-07 | **Admin forjado con el secreto** (simula filtración) | **200** | ✘ riesgo residual de HS256 (N5) |
| TC-03 | Login enviando un Bearer ajeno en el header | login normal | ✔ (lo ignora) |

### TA / TC / TD — Robo, autenticación y configuración

| ID | Caso | Resultado | |
|---|---|:--:|---|
| TA-02 | Reutilizar el token de `lector` desde otro cliente (`curl`) | 200 | ✘ bearer sin vínculo (P1) |
| TA-04 | Reutilizar el token **después del logout** | 401 | ✔ revocación OK |
| TA-05 | Cabeceras de seguridad en `GET /` (nginx) | ninguna | ✘ **H3** |
| TC-01 | 15 logins fallidos seguidos | 15×401, sin bloqueo | ✘ **H1** |
| TC-02 | Usuario inexistente vs. contraseña errónea | mismo mensaje genérico | ✔ (sin enumeración) |
| TC-04 | id>254 / SQLi en identificador / JSON roto | 400 / 400 / 400 | ✔ validación OK |
| TD-01 | `/swagger-ui`, `/api-docs` | 200 | ✘ **H7** |
| TD-01 | `/actuator/env`, `/actuator/heapdump` | 401 | ✔ no expuestos |
| TD-02 | Preflight CORS desde `evil.test` / `localhost:3000` | sin Allow-Origin / permitido | ✔ |
| TD-03 | `POST /login` con `Content-Type: text/plain` (sin auth) | **500 + stack en logs** | ✘ **N3** |
| TD-05 | Puertos publicados | 5432 y 5050 en `0.0.0.0` | ✘ **H6** |
| TD-06 | Log de login fallido | sin IP ni identificador | ✘ **H9** |

### TD-04 — Base de datos (`psql` como `gn_app`)

| Caso | Resultado | |
|---|:--:|---|
| `SELECT` en `usuarios.usuarios` sin rol | `permission denied` | ✔ deny by default |
| `gn_lector` sin `app.usuario_id` | 0 filas | ✔ RLS |
| `usuarios.buscar_para_login('administrador')` | 1 fila | ✔ (único acceso pre-auth) |
| `SET ROLE gn_administrador` como `gn_app` | **permitido** | ⚠ por diseño (`SET TRUE`): un SQLi escalaría a admin de BD pese a RLS — ver [riesgo aceptado](seguridad.md#211-autorización-en-la-base-de-datos) |

---

## Hallazgos

Prioridad = probabilidad × impacto en el contexto de despliegue previsto (frontend Cloudflare + backend/DB en contenedores).

### Alta

- **N1 — DoS en el login (pool + Argon2).** `AuthServiceImpl` es `@Transactional(readOnly=true)` a nivel de clase, así que Argon2 (`matches`) corre reteniendo una conexión; con `open-in-view` sin fijar (queda `true`) la conexión se libera recién al final del request. Pool de prod = 20. ~20-30 logins concurrentes (sin credenciales válidas) vacían el pool y tiran la API (incluido el validador de revocación de cada request). Cada hash pide 16 MiB → riesgo de OOM con muchos hilos. **Fix:** transacción corta para la búsqueda + hash fuera de ella, `spring.jpa.open-in-view=false`, bulkhead/semáforo para el hashing, y rate limit (H1). *Subir Argon2 (H12) agrava esto.*
- **N2 — Credenciales de superusuario en el runtime del backend.** `compose.yaml` y `application-prod.properties` dejan `DB_USER`/`DB_PASSWORD` (= superusuario de la imagen) en el proceso del backend de forma permanente, solo para Flyway. Un RCE o un heap dump da superusuario de BD y, vía `COPY … TO PROGRAM`, RCE en Postgres — anula todo el diseño `gn_app`+RLS. **Fix:** correr Flyway como job/init-container aparte; el backend recibe solo `DB_APP_PASSWORD`. (Amplía Pendiente #2.)
- **H1 — Sin rate limiting ni bloqueo en el login** (fuerza bruta / credential stuffing). *Confirmado TC-01.* Pendiente #3.
- **H2 — Sin HTTPS/HSTS**: credenciales y token en claro. Pendiente #5.

### Media

- **N3 — 4xx del framework convertidos en 500 con stack.** El `@ExceptionHandler(Exception.class)` de `ControllerAdvisor` captura `HttpMediaTypeNotSupportedException`, `HttpRequestMethodNotSupportedException`, `NoResourceFoundException`, etc.: un request sin autenticar (`text/plain`) devuelve 500 y escribe un stack completo en `ERROR` en cada intento. *Confirmado TD-03.* **Fix:** extender `ResponseEntityExceptionHandler` (o respetar el status de `ErrorResponse`) y loguear en WARN sin stack.
- **H3 — Sin CSP ni cabeceras de seguridad** (token en `sessionStorage`). *Confirmado TA-05.* Pendiente #8. **Nota (N9):** el deploy es Cloudflare, donde `nginx.conf` no aplica — hace falta un `public/_headers`.
- **H5 — Rol/baja no re-validados contra la base durante 8 h.** Pendiente #6/#7.
- **H6 — `compose.yaml`:** Postgres (5432) y pgAdmin (5050) publicados en `0.0.0.0`; imagen `dpage/pgadmin4:latest` sin fijar. *Confirmado TD-05.* **Fix:** no publicar 5432/5050 fuera de la red de compose (o `127.0.0.1:`), fijar versión de pgAdmin.
- **H9 — Login fallido sin IP ni identificador en el log**: imposible detectar o correlacionar ataques. *Confirmado TD-06.* Amplía Pendiente #10.
- **N4 — `event_publication` sin RLS y con grants amplios.** `V6` otorga `SELECT/INSERT/UPDATE/DELETE` a los cuatro roles (incluido `gn_lector`) sin RLS. Un SQLi permitiría falsificar/borrar eventos (p. ej. de auditoría). Cuidado con "arreglar" los listeners con `MODE_INHERITABLETHREADLOCAL` (haría que los hilos del pool hereden identidad de otro request). **Fix:** privilegios mínimos por rol y un rol de sistema para los listeners.

### Baja

- **N5 — Secreto JWT como texto crudo** (`JwtConfig`). Exigir base64 de 32 bytes aleatorios. *El valor de dev tiene buena entropía (TB-10).*
- **N6 — Placeholder interpolado en SQL.** `afterMigrate__clave_rol_app.sql`: una clave con `'` rompe o inyecta la sentencia (ejecutada como owner) y viaja en claro a los logs de PG si falla. **Fix:** validar el juego de caracteres o usar un verifier SCRAM pre-hasheado.
- **N7 — Perfil dev en entorno desplegado:** contraseñas por defecto y seed `administrador/Admin.1234` con `ON CONFLICT DO NOTHING` (sobreviven al pasar a prod). **Fix:** fallar el arranque si dev corre fuera de localhost y si en prod existen los usernames del seed.
- **H7 — Swagger/`/api-docs` accesibles** (también en prod). *Confirmado TD-01.* Pendiente #11.
- **H8 — `ControllerAdvisor` devuelve `ex.getMessage()`** de `IllegalArgumentException`/`IllegalStateException` al cliente. (Menor que N3.)
- **N8 — Converter de authorities vs. validador de BD.** `JwtGrantedAuthoritiesConverter` parte el claim por espacios; `RolBaseDatosDataSource` usa el valor exacto. Solo explotable con la clave, pero conviene un converter con lista blanca del enum y agregar `aud`.
- **N9 — Frontend/deploy:** `_headers` para Cloudflare; `.dockerignore` sin `.env*`/`.wrangler` (riesgo de incrustar `VITE_*` en el bundle con `COPY . .`); evaluar `nginx-unprivileged`.
- **H12 — Argon2id bajo el mínimo OWASP** (16 vs 19 MiB) y **sin escaneo de dependencias en CI**. Pendiente #4/#16. `bcprov` y `springdoc` están fijados fuera del BOM de Boot: incluirlos en el escaneo.
- **N10 — Carrera con un 401 tardío** (UX): el interceptor de axios no compara el token del request, así que un 401 rezagado puede borrar una sesión nueva.

### Descartados / sobrestimados

- **Fuga de identidad entre requests por el pool:** no existe. `set_config` se reescribe en cada checkout (autocommit) y la conexión se cierra si falla; OSIV abre el EntityManager después de los filtros; `@Scheduled`/`@Async` quedan como `gn_app` (fallan cerrado).
- **BearerTokenResolver:** rechaza por defecto el token en query y en form.
- **Open redirect con `state.from`:** no explotable (es history-state del mismo origen, armado como pathname+search).
- **`tokens_revocados_insert WITH CHECK(true)` y CORS `allowCredentials`:** hardening, no vulnerabilidades. El primero solo se abusa con SQLi (inflar la tabla) — acotarlo con `expira_en <= now() + interval '8h5m'`.

---

## Correcciones priorizadas (resumen)

1. **Antes de producción (Alta):** rate limiting + lockout (H1); TLS/HSTS (H2); separar Flyway del runtime del backend (N2); arreglar el login para que no agote el pool + `open-in-view=false` (N1).
2. **Endurecimiento (Media):** CSP y cabeceras vía `_headers` de Cloudflare (H3/N9); no publicar 5432/5050 y fijar pgAdmin (H6); extender `ResponseEntityExceptionHandler` (N3); log de login fallido con IP+identificador (H9); RLS y grants mínimos en `event_publication` (N4); revocar sesiones al cambiar rol/baja (H5).
3. **Mejoras (Baja):** secreto JWT en base64 decodificado (N5); placeholder de la clave sin interpolar (N6); guardas de perfil dev/seed (N7); Swagger off en prod (H7); subir Argon2 (tras N1) y Dependency-Check en CI (H12).

El mecanismo de autorización ya está listo; falta **anotar cada endpoint de negocio con `@PreAuthorize`** y definir los permisos del lector (Pendiente #1) a medida que se construyan los módulos.

---

## Re-test después de las correcciones (2026-10-04)

Mismo stack de `compose.yaml` (perfil **dev**, volumen recreado), ahora con el job de migraciones separado y el owner no superusuario. Además: `./mvnw verify` (195 tests, cobertura 99,8 % de líneas y 98,6 % de ramas), `npm run lint`, `typecheck` y `build`, y prueba end-to-end en Chrome. Los logs de `migraciones`, `backend`, `frontend` y `postgres` quedaron sin errores; los únicos `WARN` del backend son los que provocan las propias pruebas (logins rechazados, bloqueos, 415).

### Resultado por hallazgo

| Hallazgo | Corrección | Re-test | |
|---|---|---|:--:|
| **N1** DoS en el login | Login sin transacción (el hash corre sin conexión), `open-in-view=false`, bulkhead del hash (503 + `Retry-After`) | Tests del bulkhead y de 503; login funcional | ✔ |
| **N2** Superusuario en el runtime | Job `migraciones` (Flyway oficial) con owner `gn_owner` sin `SUPERUSER`/`BYPASSRLS`; el backend recibe solo `DB_APP_PASSWORD` | `docker inspect` del backend: sin `DB_USER`/`DB_PASSWORD`; `gn_owner`: `rolsuper=f`, `rolbypassrls=f` | ✔ |
| **H1** Sin rate limiting | Bucket4j: 10 intentos/min por IP, 5 fallos/15 min por cuenta, 429 + `Retry-After` | TC-01: 5×401 y el 6.º → **429** (`Retry-After: 179`), también con la clave correcta; 11 cuentas distintas desde una IP → el 11.º **429** | ✔ |
| **H2** Sin HTTPS/HSTS | HSTS en API (Spring Security, sobre HTTPS) y frontend; TLS en Cloudflare | Test `hstsSobreHttps`; TLS se configura al desplegar ([`despliegue-seguro.md`](despliegue-seguro.md#3-cloudflare-tls-a04)) | ✔ / 📋 |
| **N3** 4xx convertidos en 500 | `ControllerAdvisor` respeta el status de `ErrorResponse`, WARN sin stack | TD-03: `text/plain` → **415** `ExceptionInfo`, sin stack en el log; 405 con `Allow` | ✔ |
| **H3** Sin CSP ni cabeceras | `scripts/security-headers.mjs` → `_headers` (Cloudflare) y nginx; CSP + headers en la API | TA-05: `GET /` con CSP, `nosniff`, `DENY`, `Referrer-Policy`, `Permissions-Policy`, COOP/CORP, HSTS; Chrome: 0 violaciones navegando, `<script>` inline inyectado **bloqueado** | ✔ |
| **H4** Token vivo al cerrar la pestaña | Token de 2 h + cierre por inactividad (30 min) que revoca en el backend | Chrome: inactividad simulada → toast, `/login` y `POST /logout` 204 | ✔ (residual acotado, ver [seguridad.md 3.2](seguridad.md#32-flujo-de-sesión)) |
| **H5** Rol/baja no re-validados | Rol vigente leído de la base en cada request (`nivel_acceso_vigente`) | Tests de converter, de cambio de rol en PostgreSQL y de usuario deshabilitado → 401 | ✔ |
| **H6** Puertos y pgAdmin | Puertos solo en `127.0.0.1`; pgAdmin `9.18` bajo el perfil `herramientas` | TD-05: `127.0.0.1:8080/3000/5432`; pgAdmin no arranca por defecto | ✔ |
| **H7** Swagger en prod | `springdoc.*.enabled=false` en `prod` | Configuración de perfil (en `dev` sigue habilitado a propósito) | ✔ |
| **H8** `ex.getMessage()` al cliente | Mensajes genéricos para excepciones de la plataforma | Tests del advisor | ✔ |
| **H9** Login fallido sin contexto | `WARN` con IP e identificador; bloqueos también | Log: `Inicio de sesión rechazado … (ip=172.19.0.1, identificador=editor)` | ✔ |
| **H12** Argon2 y escaneo | Argon2id `m=19 MiB, t=2`; Dependabot y workflow de CI con `npm audit` | Hash generado con `m=19456,t=2,p=1`; hashes viejos siguen validando; `npm audit --omit=dev`: 0 | ✔ |
| **N4** `event_publication` | `V7`: grants mínimos por rol, lector sin acceso | `psql`: `gn_lector` → `permission denied` | ✔ |
| **N5** Secreto JWT como texto | Base64 decodificado, ≥ 32 bytes; `.env` regenerado con `openssl rand -base64 32` | Tests: frase en texto plano y base64 corto rechazados al arrancar | ✔ |
| **N6** Placeholder interpolado | Dollar quoting + `format('%L')` | El job de migraciones aplica la clave (`psql -U gn_app` conecta) | ✔ |
| **N7** Seed de dev en prod | `SinCredencialesPorDefecto`: `prod` no arranca con usuarios del seed y su clave por defecto | Backend con `SPRING_PROFILES_ACTIVE=prod` contra la base de dev → se detiene listando los 5 usuarios | ✔ |
| **N8** Authorities vs. validador de BD | Authority con lista blanca desde la base; claim `aud` exigido | Token con `aud` ajeno o sin `aud` → **401** | ✔ |
| **N9** Deploy del frontend | `_headers`, `.dockerignore` sin `.env*`/`.wrangler`, `nginx-unprivileged:1.30`, contenedor `read_only` | Imagen construida y corriendo sin root | ✔ |
| **N10** 401 tardío | El interceptor solo cierra la sesión si el 401 es del token actual | Revisión de código | ✔ |
| Hardening `tokens_revocados` | `V8`: `expira_en <= now() + 1 day` | `psql`: insert a 30 días → viola la política RLS | ✔ |

📋 = requiere configuración manual al desplegar, documentada en [`despliegue-seguro.md`](despliegue-seguro.md).

### Manipulación de tokens (re-test)

| Caso | Antes | Ahora |
|---|:--:|:--:|
| `nivelAcceso`→ADMIN con firma original | 401 | **401** |
| `alg:none` | 401 | **401** |
| Firmado con el secreto pero `aud` ajeno / sin `aud` | — | **401** / **401** |
| Firmado con el secreto, `sub` de un usuario inexistente | — | **401** |
| **Admin forjado con el secreto** (TB-07) | 200 **como administrador** | 200 **como lector**: el rol sale de la base, el claim se ignora |
| Token real después del logout | 401 | **401** |

Con el secreto filtrado todavía se puede suplantar a un usuario conociendo su `publicId` (riesgo propio de HS256; mitigado por el secreto aleatorio en base64 y documentado en [seguridad.md, Pendiente 7](seguridad.md#mejoras)), pero ya no escalar de rol.

### OWASP Top 10:2025 (re-evaluación)

| OWASP 2025 | Antes | Ahora | Motivo |
|---|:--:|:--:|---|
| A01 Broken Access Control | ⚠ | ✔ | Rol vigente desde la base en cada request; 3 capas (UI, `@PreAuthorize`, RLS); `event_publication` con privilegio mínimo. |
| A02 Security Misconfiguration | ✘ | ✔ | CSP y cabeceras en frontend y API, puertos en `127.0.0.1`, Swagger off en prod, sin superusuario en runtime, imágenes sin root y `read_only`. |
| A03 Supply Chain | ⚠ | ✔ | Versiones fijadas (incluido pgAdmin), Dependabot (maven, npm, docker, actions) y `npm audit` en CI. |
| A04 Cryptographic Failures | ⚠ | ✔ 📋 | Argon2id con parámetros OWASP, secreto JWT aleatorio en base64, HSTS; TLS en Cloudflare al desplegar. |
| A05 Injection | ✔ | ✔ | Sin cambios; además el placeholder de la clave de `gn_app` ya no se interpola. |
| A06 Insecure Design | ⚠ | ✔ | Login sin conexión retenida + bulkhead; token de 2 h e inactividad de 30 min con revocación. |
| A07 Authentication Failures | ✘ | ✔ | Rate limiting por IP y por cuenta sin enumeración, sin credenciales por defecto en prod. |
| A08 Data Integrity | ✔ | ✔ | JWT firmado, algoritmo fijo, `iss` y `aud` exigidos. |
| A09 Logging & Alerting | ✘ | ✔ 📋 | Logins fallidos y bloqueos con IP e identificador; 4xx en WARN sin stack. Alertas a configurar al desplegar. |
| A10 Exceptional Conditions | ⚠ | ✔ | Status correcto para errores del framework, nunca mensajes internos, 429/503 con `Retry-After`. |

**Veredicto:** el flujo de autenticación y autorización **pasa OWASP Top 10:2025**, con la condición de aplicar en producción la configuración de [`despliegue-seguro.md`](despliegue-seguro.md) (TLS, rate limiting de borde, Tunnel, alertas). Lo que queda abierto en [seguridad.md → Pendiente](seguridad.md#pendiente) depende de funcionalidades que todavía no existen (endpoints de negocio, gestión de usuarios) o son mejoras.

### Revisión independiente de las correcciones

Un segundo análisis (revisión de código enfocada en seguridad, sobre el diff de las correcciones) encontró y se corrigieron:

| Hallazgo | Corrección | Verificación |
|---|---|:--:|
| **Carrera en el límite por cuenta:** el intento se descontaba recién al confirmar el fallo, así que una ráfaga concurrente (desde muchas IPs) superaba los 5 intentos mientras se calculaban los hashes | El intento de la cuenta se consume **antes** del hash; un login correcto lo devuelve | 12 logins concurrentes contra una cuenta → exactamente **5×401** y **7×429**; test `consumeElIntentoDeLaCuentaAntesDeVerificarLaClave` | ✔ |
| **Bulkhead que retenía hilos de Tomcat** hasta 5 s: una ráfaga podía dejar sin hilos al resto de la API | Espera máxima de 500 ms | Test del bulkhead | ✔ |
| **`afterMigrate`:** una clave con `$do$` cortaba el bloque `DO` | Etiquetas propias en ambos niveles y el job de migraciones rechaza claves con `$` | `DB_APP_PASSWORD='a$b'` → el job termina con error antes de migrar | ✔ |
| **IP falsificable con `X-Forwarded-For`** desde la red interna | En `prod`, la IP sale de `CF-Connecting-IP` (configurable) y se documenta acotar `internal-proxies` | Configuración + [`despliegue-seguro.md`](despliegue-seguro.md#4-cloudflare-ip-real-del-cliente) | ✔ 📋 |
| **Pestaña duplicada:** la inactividad de una pestaña revocaba el token que se estaba usando en otra | La última actividad se comparte por `localStorage` (clave por sesión, sin el token) | Typecheck/lint; lógica revisada | ✔ |

Quedan documentados como riesgos menores: el nombre de usuario y el email de una cuenta tienen buckets separados (tope efectivo de 10 fallos/15 min) y las variables de entorno de los contenedores son visibles con `docker inspect` (en producción, usar Docker secrets).
