# Seguridad: autenticación y autorización

Decisiones de diseño tomadas al implementar **HU-01 (Iniciar sesión)**, **HU-02 (Cerrar sesión)** y la autorización por rol (frontend, backend y base de datos), el endurecimiento posterior a la auditoría de seguridad, y lo que queda pendiente. Última actualización: 2026-10-04.

> **Idea rectora:** el frontend mejora la experiencia de uso; el backend es la única barrera real de seguridad. Todo lo que el frontend oculta o bloquea tiene que estar también protegido en el backend (ver [Pendiente](#pendiente)).

> **Pruebas:** el análisis de vulnerabilidades, la evaluación OWASP Top 10:2025 de este flujo y el re-test después de las correcciones están en [`pruebas-seguridad.md`](pruebas-seguridad.md).
>
> **Despliegue:** lo que se configura a mano en producción (TLS, rate limiting y DDoS en Cloudflare, Tunnel, secretos, backups) está en [`despliegue-seguro.md`](despliegue-seguro.md).

---

## 1. Visión general

```
 Navegador (React)                          Backend (Spring Boot)
 ─────────────────                          ─────────────────────
 LoginCard ── POST /api/auth/login ───────► AuthController ─► AuthServiceImpl
                                                                ├─ UsuarioRepository (Argon2id)
            ◄── { token JWT, expiraEn, usuario } ───────────────┘─ TokenService.generar()

 axios ── Authorization: Bearer <JWT> ────► Resource server (filtros de Spring Security)
                                              ├─ firma HS256, exp, iss, aud
                                              ├─ jti no revocado (seguridad.tokens_revocados)
                                              └─ rol vigente en la base (usuarios.nivel_acceso_vigente)

 Logout ── POST /api/auth/logout ─────────► TokenService.revocar(jti)
```

| Pieza | Dónde |
|---|---|
| Configuración de Spring Security, JWT, hash, revocación | `backend/.../security/` (módulo Modulith) |
| Usuarios, login, logout, `/me` | `backend/.../usuarios/` |
| Migraciones | `db/migration/V1__crear_tabla_usuarios.sql`, `V2__crear_tabla_tokens_revocados.sql`; roles, permisos y RLS en `V3`–`V9` |
| Rol de base por conexión | `backend/.../security/bd/` |
| Límite de intentos de login | `backend/.../security/limite/` (API: `LimitadorIntentosLogin`) |
| Owner de la base y job de migraciones | `infra/postgres/initdb/`, `backend/.../Dockerfile.migraciones`, servicio `migraciones` de `compose.yaml` |
| Cabeceras HTTP del frontend (CSP) | `frontend/.../scripts/security-headers.mjs` |
| Sesión, guards, formulario | `frontend/.../src/features/auth/` |
| Reglas de acceso de la UI | `frontend/.../src/lib/access.ts` |

---

## 2. Backend

### 2.1 Organización en módulos

- La seguridad vive en su propio módulo Modulith, `security/`, y no en `config/`. Su API pública (paquete base) es `TokenService`, `TokenEmitido`, `LimitadorIntentosLogin` y el puerto `NivelAccesoVigente`. Lo demás queda interno, en `jwt/`, `revocacion/`, `limite/`, `bd/` y `web/`.
- `usuarios` depende de `security` y no al revés: el módulo de seguridad no sabe qué es un usuario y solo emite o revoca tokens para un `publicId` y un nivel de acceso. Cuando necesita el rol vigente de un usuario lo pide por el puerto `NivelAccesoVigente`, que implementa `usuarios` (inversión de dependencias).
- `ModularityTests` verifica estos límites (`ApplicationModules.verify()`).

### 2.2 Autenticación con JWT (stateless)

- **Mecanismo:** el resource server OAuth2 de Spring Security, con Nimbus. Es el soporte estándar del framework, así que no hace falta una librería extra como jjwt ni un filtro escrito a mano.
- **Algoritmo:** HS256 (HMAC) con una clave compartida (`JWT_SECRET`). Alcanza porque un único servicio emite y valida los tokens. El secreto se configura en **base64** y se decodifica (`openssl rand -base64 32`): así es material aleatorio y no una frase que se pueda adivinar offline a partir de un token. La aplicación **no arranca** si falta, no es base64 o mide menos de 32 bytes (256 bits) decodificado.
- **Transporte:** header `Authorization: Bearer`, sin cookies. Por eso **CSRF está deshabilitado**: el navegador no adjunta el token automáticamente, así que no hay petición cruzada que falsificar.
- **Sesión:** `SessionCreationPolicy.STATELESS`. El servidor no guarda sesión HTTP.
- **Claims:**

  | Claim | Valor | Por qué |
  |---|---|---|
  | `sub` | `publicId` (UUID) del usuario | El `id` interno (Long) nunca sale del módulo `usuarios` (ver diagrama de dominio). |
  | `jti` | UUID aleatorio | Permite revocar un token puntual. |
  | `iss` | `gestion-neumaticos` | Se exige al validar. |
  | `aud` | `gestion-neumaticos-api` | Se exige al validar: un token emitido para otro destinatario con la misma clave no sirve acá. |
  | `iat` / `exp` | emisión y expiración | Vigencia `PT2H` (`app.security.jwt.expiracion`). Además el frontend cierra la sesión tras 30 minutos de inactividad (3.2). |
  | `nivelAcceso` | `ROLE_ADMINISTRADOR` / `ROLE_EDITOR` / `ROLE_LECTOR` | **Solo informativo.** La autorización usa el nivel vigente en la base (ver abajo). |

- **Validación en cada request:** firma (algoritmo fijo HS256), `exp`, `iss`, `aud` y que el `jti` no esté revocado.
- **Rol vigente desde la base:** `AutoridadesVigentesConverter` arma la autenticación con el nivel de acceso que el usuario tiene **hoy** en `usuarios.usuarios` (función `usuarios.nivel_acceso_vigente`, ver 2.11), no con el claim. Un cambio de rol o una baja rigen en el request siguiente (antes, hasta 8 h después), y un claim `nivelAcceso` alterado no otorga permisos aunque se haya filtrado la clave de firma. Si el usuario ya no existe, el token se rechaza con 401. El costo es una consulta indexada por request, igual que la de revocación.

### 2.3 Cierre de sesión e invalidación del token (HU-02)

Un JWT no tiene estado: una vez firmado, es válido hasta que vence. Para que "cerrar sesión" invalide de verdad el token, se mantiene una **lista de revocación**:

- `POST /api/auth/logout` guarda el `jti` y su expiración en `seguridad.tokens_revocados`.
- `TokenNoRevocadoValidator` rechaza cualquier token cuyo `jti` esté en esa tabla, y también los tokens sin `jti` o con un `jti` que no sea un UUID.
- Una tarea diaria (`LimpiezaTokensRevocadosTask`, 03:00) borra las filas ya vencidas, que de todos modos rechaza la validación de `exp`. La tabla solo crece con logouts de tokens que todavía no vencieron.

Opciones descartadas:

- **Tokens de vida muy corta sin revocación:** el logout no sería inmediato.
- **Sesión de servidor:** se pierde la ventaja de una API sin estado y complica escalar.

### 2.4 Almacenamiento de contraseñas

- **Argon2id** con los parámetros mínimos de OWASP: `m=19 MiB, t=2, p=1` (`new Argon2PasswordEncoder(16, 32, 1, 19456, 2)`). Es el algoritmo que recomienda hoy OWASP y, a diferencia de bcrypt, no trunca la contraseña a 72 bytes.
- Va envuelto en un **`DelegatingPasswordEncoder`**: cada hash se guarda con prefijo y parámetros (`{argon2}$argon2id$v=19$m=19456,t=2,p=1$…`), así que los hashes anteriores (`m=16384`, como los del seed de dev) siguen validando y más adelante se puede cambiar de algoritmo o de parámetros sin invalidar contraseñas.
- **Bulkhead:** el encoder queda detrás de un límite de concurrencia (`PasswordEncoderConcurrenciaLimitada`, un semáforo con tantos cupos como núcleos). Si no hay cupo en 500 ms responde **503** con `Retry-After` en lugar de encolar hilos: una ráfaga de logins no puede agotar memoria (19 MiB por hash) ni dejar al resto de la API sin hilos de Tomcat (la espera es corta a propósito).
- Nunca se persiste ni se loguea la contraseña en texto plano. `LoginRequestDTO` y `Usuario` sobrescriben `toString` para no exponer la contraseña ni el hash. `Usuario` no usa `@Data` por ese motivo.

### 2.5 Login resistente a la enumeración de cuentas

- **Mensaje único:** usuario inexistente y contraseña incorrecta devuelven el mismo **401 "Credenciales inválidas"** (`CredencialesInvalidasException`).
- **Tiempo constante:** si el usuario no existe, igual se ejecuta un `passwordEncoder.matches` contra un *hash dummy* precalculado. Así la respuesta tarda lo mismo y no se puede deducir por tiempos qué cuentas existen.
- **Logs:** un login fallido o bloqueado se registra en `WARN` con la **IP y el identificador normalizado** (para detectar y correlacionar ataques); uno exitoso, en `INFO` con el `publicId` y la IP. El identificador ya pasó `@IdentificadorValido` (sin espacios ni saltos de línea), así que no permite inyectar líneas en el log. Nunca se loguea la contraseña.

### 2.5.1 Límite de intentos y protección contra DoS en el login

- **Rate limiting (Bucket4j, token bucket)** en `LimitadorIntentosLoginImpl`, configurable con `app.security.login.*`:
  - **Por IP:** 10 intentos por minuto. Cuenta todo intento, exitoso o no (frena el *credential stuffing* contra muchas cuentas).
  - **Por identificador:** 5 intentos fallidos cada 15 minutos (se repone uno cada 3 min). El intento se consume **antes** de verificar la contraseña y un login correcto lo devuelve (olvida los intentos previos), así que en la práctica cuentan los fallos; consumirlo por adelantado impide que una ráfaga concurrente supere el límite mientras los hashes anteriores todavía se calculan. Cuenta exista o no la cuenta, así el bloqueo no revela cuáles existen. Es *throttling* temporal y no un bloqueo permanente, para que un atacante no pueda dejar a un usuario afuera indefinidamente. El nombre de usuario y el email de una misma cuenta tienen buckets separados, así que el tope efectivo es de 10 fallos cada 15 minutos.
  - Superado el límite responde **429** con `Retry-After` (en segundos) y el mismo mensaje en ambos casos. El chequeo es previo a buscar al usuario y a calcular el hash.
  - Los buckets viven en un caché Caffeine acotado (100 000 entradas, expiran tras su ventana), así identificadores aleatorios no agotan la memoria. Es por instancia: con varias réplicas, ver [`despliegue-seguro.md`](despliegue-seguro.md#7-backend-más-de-una-instancia).
- **Sin conexión retenida durante el hash:** `AuthServiceImpl.login` no es transaccional; la búsqueda toma y devuelve su conexión, y Argon2 corre fuera. `spring.jpa.open-in-view=false`, así ningún request retiene una conexión hasta el final. Antes, ~20 logins concurrentes agotaban el pool y tiraban la API.
- **IP real:** en `prod`, `server.forward-headers-strategy=native` hace que Tomcat tome la IP del cliente de `X-Forwarded-For` solo si el request viene de un proxy interno. Detrás de Cloudflare, ver [`despliegue-seguro.md`](despliegue-seguro.md#4-cloudflare-ip-real-del-cliente).
- El borde (Cloudflare) suma un rate limiting propio y la mitigación DDoS, que se configuran a mano.

### 2.6 Validación de entrada

Reglas mínimas, coincidentes entre frontend y backend:

| Campo | Reglas |
|---|---|
| `identifier` | Obligatorio, máx. 254. Si contiene `@`, formato de email; si no, `^[a-zA-Z0-9._-]{3,30}$` (`@IdentificadorValido`). Se recorta y se pasa a minúsculas antes de buscar. |
| `password` | Obligatoria (`@NotEmpty`, no `@NotBlank`), máx. 128. **No se recorta** ni se le exigen reglas de complejidad en el login: eso corresponde al alta o cambio de contraseña. El máximo evita usar contraseñas enormes para un DoS contra un hash costoso. |

`email` y `nombre_usuario` se persisten en minúsculas (`@PrePersist`), y la base lo garantiza con `CHECK`. Un JSON mal formado responde 400, no 500.

### 2.7 Identificadores

- `id`: `BIGINT` con secuencia `INCREMENT BY 50`, igual a `allocationSize = 50`. Es de uso interno (PK, joins).
- `public_id`: **UUIDv7** (`@UuidGenerator(style = VERSION_7)`). Es el que se expone en la API y en el JWT. No permite adivinar otros usuarios recorriendo ids secuenciales, y al estar ordenado por tiempo mantiene un índice B-tree compacto.

### 2.8 Errores

- Los 401 y 403 de los filtros de seguridad se responden con el mismo `ExceptionInfo` JSON que el `ControllerAdvisor` (`JsonSecurityErrorHandler`), con `WWW-Authenticate: Bearer`.
- Los errores propios de Spring MVC implementan `ErrorResponse` (415 media type no soportado, 405 método no permitido, 404 ruta inexistente, 400 parámetro faltante…): el `ControllerAdvisor` **conserva su status y sus headers** (por ejemplo `Allow` en el 405) con un mensaje propio, y los loguea en `WARN` sin stack, porque los provoca el cliente. Antes caían en el 500 genérico y escribían un stack en `ERROR` por request.
- **Nunca se devuelve el mensaje de una excepción de la plataforma** (JDK, Spring, JPA): `IllegalArgumentException` → 400 "La solicitud es inválida", `EntityNotFoundException` → 404 genérico, `IllegalStateException` y cualquier otra → 500 "Error interno del servidor". El detalle queda solo en el log. Las excepciones propias del dominio (`exception/`) sí llevan su mensaje, porque se escriben para el cliente.
- 429 (`DemasiadosIntentosException`) y 503 (`ServicioSaturadoException`) incluyen `Retry-After`.

### 2.9 CORS y secretos

- CORS admite un único origen (`FRONTEND_URL`), expone `Retry-After` y **no permite credenciales** (`allowCredentials=false`): la autenticación viaja en el header `Authorization`, no en cookies.
- `JWT_SECRET` y las credenciales de base viven en el `.env` raíz (fuera de git); `.env.example` documenta cada variable sin valores.
- **Usuarios de prueba:** se cargan solo en `dev`, con el seed repetible `db/seed/dev/R__usuarios_dev.sql` (en `spring.flyway.locations` de `application-dev.properties`, y en `FLYWAY_LOCATIONS` del job de migraciones de compose). Hay dos editores y dos lectores para poder comprobar que cada uno ve solo su fila. **Salvaguarda:** en `prod` la app no arranca si algún usuario del seed sigue con su contraseña por defecto (`SinCredencialesPorDefecto`), por si una base que corrió en `dev` pasa a producción.
- **Credenciales de base:** el superusuario de la imagen (`DB_USER`/`DB_PASSWORD`) solo crea el owner al inicializar el volumen. El owner (`DB_OWNER_USER`, sin `SUPERUSER`) solo lo usa el job de migraciones. **El backend recibe únicamente `DB_APP_PASSWORD`** (rol `gn_app`): un RCE o un heap dump del backend ya no expone credenciales con DDL ni de superusuario (ver 2.11).

### 2.10 Autorización por rol en los endpoints

Se usa el mecanismo estándar de Spring Security, sin código propio:

- `@EnableMethodSecurity` en `SecurityConfig`. Cada endpoint declara su regla con `@PreAuthorize("hasRole('EDITOR')")`.
- Un bean `RoleHierarchy`: **ADMINISTRADOR > EDITOR > LECTOR**. `hasRole('EDITOR')` habilita también al administrador, y `hasRole('LECTOR')` habilita a los tres. Lo toman tanto `@PreAuthorize` como `authorizeHttpRequests`.
- Un rol insuficiente responde **403** con `ExceptionInfo`: `ControllerAdvisor` atrapa `AccessDeniedException`. Si no, el handler genérico lo convertiría en 500.
- El login ignora un `Authorization: Bearer` que venga en la request (`bearerTokenResolver`): siempre corre sin usuario.

Convención para los endpoints que vengan:

| Operación | Anotación |
|---|---|
| Lectura que también puede hacer el lector | `@PreAuthorize("hasRole('LECTOR')")` |
| Operación de negocio (alta, edición, etc.) | `@PreAuthorize("hasRole('EDITOR')")` |
| Configuración, catálogos, auditoría, usuarios | `@PreAuthorize("hasRole('ADMINISTRADOR')")` |

Sin anotación, el endpoint queda abierto a cualquier usuario autenticado (`anyRequest().authenticated()`). `AutorizacionPorRolTest` verifica la jerarquía con un controller de prueba.

El rol que evalúan `@PreAuthorize` y la base es el **vigente en la base** en ese request (2.2), no el que traía el token.

### 2.10.1 Cabeceras HTTP de la API

Spring Security agrega por defecto `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` y, en requests HTTPS, `Strict-Transport-Security` (1 año, `includeSubDomains`). Además:

- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`: la API solo devuelve JSON, no carga recursos ni se puede embeber. Swagger UI (solo en `dev`) queda afuera porque necesita sus scripts.
- `Referrer-Policy: no-referrer`.
- En `prod`, Swagger UI y `/api-docs` están **deshabilitados** (`springdoc.*.enabled=false`).

### 2.11 Autorización en la base de datos

Principio de **privilegio mínimo y denegación por defecto**. Sigue el patrón *authenticator* de PostgREST y Supabase: aunque una consulta del backend tuviera un bug, la base solo devuelve o modifica lo que el rol del usuario puede tocar.

**Roles** (`V3__roles_y_privilegios_minimos.sql`):

| Rol | Tipo | Qué puede hacer |
|---|---|---|
| Superusuario de la imagen (`DB_USER`) | `SUPERUSER` | Solo crea el owner al inicializar el volumen (`infra/postgres/initdb/01-crear-owner.sh`). Ningún servicio se conecta con él. |
| Owner (`DB_OWNER_USER`, `gn_owner`) | `LOGIN CREATEROLE`, sin `SUPERUSER` ni `BYPASSRLS`; dueño de la base, schemas y tablas | Solo DDL, desde el job de migraciones (servicio `migraciones`, imagen oficial de Flyway). `CREATEROLE` le alcanza para crear y administrar los roles `gn_*`. Sin superusuario no hay `COPY … TO PROGRAM` ni escalada al sistema operativo. |
| `gn_app` | `LOGIN NOINHERIT` | Rol de conexión del pool. Por sí mismo solo puede: ejecutar `usuarios.buscar_para_login` y `usuarios.nivel_acceso_vigente`, leer `seguridad.tokens_revocados`, borrar los tokens ya vencidos y gestionar `event_publication` (listeners asíncronos). |
| `gn_administrador`, `gn_editor`, `gn_lector` | `NOLOGIN` | Los permisos reales. `gn_app` puede asumirlos (`SET ROLE`), pero no los hereda (`WITH INHERIT FALSE`). |

La clave de `gn_app` la fija el callback `afterMigrate__clave_rol_app.sql` desde `DB_APP_PASSWORD` en cada migrate. El placeholder va dentro de un string con *dollar quoting* de etiqueta propia y pasa por `format('%L')`: una comilla en la clave ya no rompe ni inyecta la sentencia que corre como owner.

**Cómo llega el usuario a la base.** `RolBaseDatosDataSource` (módulo `security`) envuelve el datasource. En cada conexión que se toma del pool ejecuta un único statement parametrizado:

```sql
SELECT set_config('role', 'gn_editor', false), set_config('app.usuario_id', '<publicId del JWT>', false)
```

- El rol sale de una lista blanca según la *authority* de la autenticación, que el resource server ya cargó desde la base (2.2). El claim del token no interviene: con la clave de firma filtrada, un `nivelAcceso` forjado tampoco da el rol de base.
- Sin usuario autenticado (login, validación del token, tareas programadas) se usa `'none'`, que vuelve a `gn_app`, y `''`.
- Como cada checkout sobrescribe los dos valores, una conexión reciclada no arrastra la identidad del usuario anterior.

Las políticas leen al usuario con `seguridad.usuario_actual()`.

**Denegar por defecto:**

- Se revocan los privilegios de `PUBLIC` sobre la base, el schema `public` y la ejecución de funciones nuevas. Las tablas nacen sin grants.
- Toda tabla tiene `ENABLE ROW LEVEL SECURITY`. Sin una política que lo permita, no se ve ni se modifica nada.
- Cada tabla lleva una política **`RESTRICTIVE`** "requiere autenticación" (`seguridad.usuario_actual() IS NOT NULL`) para los tres roles. Se combina con AND con las permisivas, así que ningún rol opera sin un usuario identificado.

**`usuarios.usuarios`** (`V4`):

| | Administrador | Editor | Lector |
|---|:-:|:-:|:-:|
| SELECT | todas las filas | solo la propia | solo la propia |
| INSERT | ✔ | ✘ | ✘ |
| UPDATE | `nombre_usuario`, `nombre_apellido`, `email`, `clave`, `nivel_acceso` (no `id` ni `public_id`) | ✘ | ✘ |
| DELETE | ✘ | ✘ | ✘ |

El login ocurre antes de que haya un usuario. En lugar de abrir la tabla a `gn_app`, este solo puede ejecutar `usuarios.buscar_para_login(identificador)`: una función `SECURITY DEFINER` con `search_path` vacío que devuelve únicamente la fila buscada. Un usuario ya autenticado no puede ejecutarla.

Con el mismo patrón, `usuarios.nivel_acceso_vigente(public_id)` (`V9`) devuelve solo el nivel de acceso actual (o `NULL`) mientras se autentica el token. La ejecuta únicamente `gn_app`.

**`seguridad.tokens_revocados`** (`V5`, `V8`):

- `gn_app` la lee, porque comprobar la revocación es parte de autenticar el token.
- `gn_app` borra solo filas con `expira_en < now()`: la purga diaria no puede borrar un token vigente.
- Los tres roles insertan (logout) y leen. El `INSERT` exige `expira_en <= now() + 1 day` (`V8`): con una inyección SQL no se puede inflar la tabla con filas que la purga nunca borraría.

**Excepciones documentadas:**

- `public.flyway_schema_history`: solo la toca el owner.
- `public.event_publication` (Spring Modulith): sin RLS, porque es infraestructura del framework sin datos por usuario y los listeners asíncronos corren sin usuario. Con privilegios mínimos (`V7`): administrador y editor publican y marcan eventos (`SELECT`, `INSERT`, `UPDATE`); el lector no tiene acceso; `gn_app` marca, reintenta y purga (`SELECT`, `UPDATE`, `DELETE`) pero no publica. Nadie más que `gn_app` borra. No se usa `MODE_INHERITABLETHREADLOCAL` para los listeners: haría que hilos del pool hereden la identidad de otro request.

**Por qué no un enum de PostgreSQL para el nivel de acceso:**

- Solo lo usa una tabla, y el `CHECK` existente ya restringe los valores.
- Un enum no permite quitar valores y complica el mapeo de Hibernate.
- Los niveles ya están modelados como roles de base.

**Riesgo aceptado:** la base confía en que `gn_app` fija el rol correcto. Una inyección SQL en la app podría ejecutar `SET ROLE gn_administrador`, como en cualquier esquema de este tipo. Por eso todo el acceso pasa por JPA o consultas parametrizadas.

**Salvaguardas:**

- La app no arranca si se conecta con un rol superusuario o con `BYPASSRLS`. Esto pasaría, por ejemplo, si se conectara con las credenciales del superusuario, y todas las políticas quedarían salteadas en silencio.
- `AutorizacionBaseDatosTests` falla si aparece una tabla nueva sin RLS, y verifica que las migraciones corran con un owner no superusuario (los tests migran como `gn_owner`, igual que compose).

**Al crear una tabla nueva** (en su schema de módulo), en la misma migración:

1. `ENABLE ROW LEVEL SECURITY`.
2. La política `RESTRICTIVE` de autenticación para los tres roles.
3. Los `GRANT` mínimos por rol, más `USAGE` sobre el schema y la secuencia si corresponde.
4. Las políticas permisivas que correspondan.

---

## 3. Frontend

### 3.1 Dónde se guarda el token

- Se guarda en un store zustand persistido en **`sessionStorage`**.
  - Ventaja: sobrevive a una recarga, pero se borra al cerrar la pestaña o el navegador.
  - Riesgo aceptado: un XSS podría leer el token. La alternativa, una cookie `HttpOnly` (o un *refresh token* en cookie), se descartó porque el frontend (Cloudflare) y el backend estarían en sitios distintos. Eso obligaría a usar `SameSite=None` y volver a activar la protección CSRF.
  - **Mitigaciones implementadas:** CSP estricta sin scripts inline (3.5), token de 2 h, cierre por inactividad a los 30 min con revocación en el backend (3.2), rol leído de la base en cada request (2.2) y TLS + HSTS en producción.
- El token nunca viaja por la URL ni queda en `localStorage`. Es un *bearer token*: quien lo tenga puede usarlo desde otro cliente hasta que expire o se revoque (no está vinculado al dispositivo).

### 3.2 Flujo de sesión

- **Envío del token:** `src/lib/api/client.ts` agrega el header `Authorization: Bearer` a cada request. Como `src/lib` no puede importar features, el feature `auth` registra cómo obtener el token y qué hacer ante un 401 (`configureApiClient`).
- **Token rechazado:** ante un 401 (salvo el del propio login), se limpia la sesión local, se vacía la caché de react-query y se avisa "Tu sesión expiró". Solo si el request se hizo **con el token actual**: un 401 tardío de un request hecho con el token de una sesión anterior no cierra la sesión nueva.
- **Cierre automático** (`useSessionTimeout`, montado en `RequireAuth`):
  - Tras **30 minutos sin actividad** (teclado, mouse, rueda o toque; configurable con `VITE_SESSION_IDLE_MINUTES`) se limpia la sesión, **se revoca el token en el backend** y se avisa "Cerramos tu sesión por inactividad". Una pestaña olvidada abierta ya no deja un token válido hasta su expiración.
  - Al llegar a `expiraEn` se limpia la sesión sin esperar a que un request devuelva 401.
  - Al volver a una pestaña en segundo plano se revisa de inmediato, porque los navegadores frenan los timers de las pestañas ocultas.
  - Cerrar la pestaña borra el `sessionStorage` pero no revoca el token (no hay forma confiable de hacerlo al cerrar: `sendBeacon` no admite el header `Authorization` y `pagehide` también se dispara al recargar). Ese token queda acotado por los 2 h de vigencia.
- **Login rechazado por límite:** un 429 muestra "Demasiados intentos. Esperá N minutos" con los minutos del header `Retry-After`; un 503, "El servicio está ocupado".
- **Sesión restaurada:** al recargar, `useSessionSync` la valida contra `GET /api/auth/me` y refresca los datos del usuario, cuyo nivel pudo cambiar. La clave de esa consulta incluye el `publicId` del usuario, para que nunca se reutilice la respuesta cacheada de otro usuario.
- **Logout:** pide confirmación y revoca el token en el backend. La sesión local se limpia aunque esa llamada falle: si el usuario pidió salir, no debe quedar adentro.
- **Botón "atrás" (back/forward cache):** el navegador puede restaurar una página anterior con la memoria de JavaScript intacta, con la sesión todavía cargada aunque se haya cerrado después. `syncSessionOnPageRestore` escucha `pageshow` con `persisted`, vacía la caché y vuelve a leer `sessionStorage`. Sin esto, "atrás" después de un logout mostraba la app como si el usuario siguiera logueado. El problema apareció en la prueba end-to-end.

### 3.3 Guards de rutas

| Guard | Efecto |
|---|---|
| `RequireAuth` | Sin sesión válida (o con el token vencido según `expiraEn`): redirige a `/login` y recuerda la ruta pedida. |
| `GuestOnly` | Con sesión, `/login` redirige a la ruta pedida o a `/home`. |
| `RequireRouteAccess` | Si el nivel de acceso no tiene permiso para la ruta, redirige a `/home`, aunque se tipee la URL a mano. |

### 3.4 Autorización de interfaz

`ROUTE_ACCESS`, en `src/lib/access.ts`, es la **única fuente de verdad**. La leen tanto la navegación (sidebar y menú mobile, que ocultan el ítem y el grupo si queda vacío) como `RequireRouteAccess` (que bloquea la URL).

| Sección | Administrador | Editor | Lector |
|---|:-:|:-:|:-:|
| Inicio | ✔ | ✔ | ✔ |
| Unidades de transporte, Neumáticos, Reparaciones, Almacenamiento | ✔ | ✔ | ✘ |
| Configuración (`/settings`) | ✔ | ✘ | ✘ |
| Auditoría (`/audit`) | ✔ | ✘ | ✘ |

El administrador es el "superusuario" del negocio. El editor puede hacer todo menos Configuración y Auditoría. Las restricciones del lector todavía no están definidas, así que por ahora solo entra a Inicio.

> Esto es **autorización de interfaz**: decide qué se muestra, no qué se permite. Las barreras reales son `@PreAuthorize` en el backend (2.10) y los permisos de la base (2.11).

### 3.5 Cabeceras HTTP y CSP

Una única definición, `scripts/security-headers.mjs`, genera las cabeceras para los dos destinos: un plugin de Vite escribe `dist/_headers` (Cloudflare) y el `Dockerfile` genera el snippet de nginx. La CSP permite conectarse solo al origen de `VITE_API_URL`, así que el build falla si no está definida.

| Cabecera | Valor |
|---|---|
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self' <API>; manifest-src 'self'; worker-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=(), usb=()` |
| `Cross-Origin-Opener-Policy` / `Cross-Origin-Resource-Policy` | `same-origin` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` (rige solo sobre HTTPS) |

- `script-src 'self'` sin `'unsafe-inline'` ni `'unsafe-eval'`: un script inyectado no se ejecuta, y `connect-src` impide mandar el token a otro origen. Verificado en Chrome: un `<script>` inline inyectado queda bloqueado.
- `style-src` admite `'unsafe-inline'` porque Base UI y el `ThemeProvider` inyectan estilos en tiempo de ejecución; los estilos no ejecutan código.
- La imagen Docker usa `nginx-unprivileged` (sin root, puerto 8080), `server_tokens off`, contenedor `read_only`, y `.dockerignore` excluye `.env*` y `.wrangler` (Vite incrustaría cualquier `VITE_*` en el bundle).

---

## 4. Verificación

- **Backend:** `./mvnw verify` corre 195 tests y exige 85 % de cobertura de líneas y ramas (hoy 99,8 % y 98,6 %). Necesita Docker: los tests de integración levantan `postgres:18-alpine` con Testcontainers, porque roles, GRANT y RLS no existen en una base embebida. Las migraciones corren como el owner no superusuario `gn_owner`, igual que en compose.
  - Unit tests del service con Mockito: hash dummy, normalización, límite de intentos (bloqueo antes de buscar al usuario, fallo y éxito registrados).
  - `@WebMvcTest` del controller con la cadena de seguridad real: endpoints públicos y protegidos, validaciones 400, 401 por credenciales, por token revocado o ausente y por usuario deshabilitado en la base, login que ignora un Bearer, IP del cliente, 429 y 503 con `Retry-After`, 415 y 405 (no 500), cabeceras de seguridad y HSTS sobre HTTPS.
  - `AutorizacionPorRolTest`: jerarquía de roles con `@PreAuthorize` y 403 en JSON.
  - `AutorizacionBaseDatosTests`, contra PostgreSQL real:
    - Rol de cada conexión.
    - Cada rol ve y modifica solo lo permitido.
    - El administrador no borra ni cambia el `public_id`.
    - Sin sesión no se lee la tabla, pero el login y `nivel_acceso_vigente` funcionan (y un usuario autenticado no puede ejecutarlos).
    - Un cambio de rol se ve en la consulta siguiente.
    - Privilegios de `event_publication` por rol.
    - Logout, purga de tokens y expiración acotada al revocar.
    - Owner sin superusuario y ninguna tabla sin RLS.
  - El emisor y el validador de JWT con el encoder y decoder reales: token expirado, firmado con otra clave, revocado o para otro `aud`; secreto no base64 o corto.
  - `AutoridadesVigentesConverter` (rol de la base, usuario inexistente, nivel desconocido), el limitador con reloj manual, el bulkhead del hash y los parámetros de Argon2id.
  - El `ControllerAdvisor` (mensajes genéricos para excepciones de la plataforma, status de `ErrorResponse`, `Retry-After`), el check de credenciales por defecto en `prod`, el validador del identificador, la estructura modular y el arranque completo del contexto.
- **End-to-end en Chrome, contra el stack de `compose.yaml`:**
  - Redirección sin sesión.
  - Campos obligatorios.
  - Mismo mensaje para usuario inexistente y contraseña incorrecta.
  - Login por usuario y por email.
  - Persistencia al recargar.
  - Logout con confirmación.
  - Token viejo rechazado con 401.
  - "Atrás" después del logout.
  - Editor sin Configuración ni Auditoría, también por URL.
  - Lector solo con Inicio: `/tires` y `/settings` tipeadas a mano redirigen a `/home`.
  - Administrador con todas las secciones.
  - `/me` y logout responden 200/204 con los tres roles (corren como `gn_lector`, `gn_editor` y `gn_administrador`).
  - *(2026-10-04)* Ninguna violación de CSP ni error de consola navegando todas las secciones; un `<script>` inline inyectado queda bloqueado por la CSP.
  - *(2026-10-04)* Seis fallos seguidos muestran "Demasiados intentos. Esperá 3 minutos".
  - *(2026-10-04)* Cierre por inactividad: toast, vuelta al login y token revocado en el backend (204).
- **Base, con `psql` como `gn_app`:**
  - Sin rol, `SELECT` sobre `usuarios` → `permission denied`.
  - `gn_lector` ve 1 fila y no puede actualizar ni borrar.
  - `gn_administrador` ve todas y actualiza, pero `DELETE` y el cambio de `public_id` → `permission denied`.
  - `gn_administrador` sin `app.usuario_id` ve 0 filas.
  - `SET ROLE` al owner → denegado.
  - *(2026-10-04)* `gn_lector`: `event_publication` y `nivel_acceso_vigente` → `permission denied`; revocar un token con expiración a 30 días → viola la política RLS.
- **Re-test dinámico de la auditoría** (`curl`, tokens manipulados, `docker inspect`): ver [`pruebas-seguridad.md`](pruebas-seguridad.md#re-test-después-de-las-correcciones-2026-10-04).

---

## Pendiente

Lo que sigue abierto después de corregir los hallazgos de [`pruebas-seguridad.md`](pruebas-seguridad.md) (2026-10-04). Lo que se configura a mano al desplegar (TLS, WAF, rate limiting de borde, Tunnel, backups) no está acá sino en [`despliegue-seguro.md`](despliegue-seguro.md).

### Al construir los módulos de negocio

1. **Anotar cada endpoint nuevo con `@PreAuthorize`** según la convención de 2.10, y crear cada tabla con RLS, la política restrictiva y los grants mínimos (2.11). El mecanismo está listo; falta aplicarlo endpoint por endpoint.
2. **Definir los permisos de lectura del lector** en las tres capas (`ROUTE_ACCESS`, `@PreAuthorize("hasRole('LECTOR')")` y políticas de base). Hoy solo entra a Inicio.

### Con la gestión de usuarios (alta, edición, baja)

3. **Baja lógica** (`activo` / `deletedAt`, según `AuditableEntity` del diagrama). Basta con que `usuarios.buscar_para_login` y `usuarios.nivel_acceso_vigente` ignoren a los inactivos para que el login y los tokens vigentes dejen de servir de inmediato.
4. **Revocar todas las sesiones de un usuario al cambiar su contraseña.** El cambio de rol y la baja ya rigen al instante (2.2), pero un token robado sigue sirviendo tras un cambio de contraseña. Opción: columna `sesiones_validas_desde` comparada con el `iat` del token en la misma consulta del rol vigente.
5. **Política de contraseñas** para el alta y el cambio (largo mínimo 12, chequeo contra contraseñas filtradas). No aplica al login.
6. **Auditoría de eventos de seguridad** (logins exitosos y fallidos, bloqueos, logouts, cambios de rol) en el módulo `trazabilidad`, que alimentaría la pantalla de Auditoría. Hoy quedan en los logs con IP e identificador.

### Mejoras

7. **Rotación de la clave JWT con `kid`** para rotar `JWT_SECRET` sin cerrar todas las sesiones. Si otros servicios tuvieran que validar tokens, pasar a firma asimétrica (RS256 o EdDSA).
8. **Rendimiento por request:** cada request autenticado hace dos consultas indexadas (revocación y rol vigente). Si el volumen crece, unificarlas en una función o cachear **solo los revocados** (Caffeine con TTL de segundos). *Cachear "no revocado" o el rol con un TTL largo anularía el logout y el cambio de rol inmediatos.*
9. **Limitador distribuido** (Bucket4j sobre Redis o PostgreSQL) si el backend pasa a tener más de una instancia.
10. **Tests automatizados del frontend** (schema de login, guards, `syncSessionOnPageRestore`, `useSessionTimeout`, interceptor de 401). Hoy la cobertura del frontend es la prueba end-to-end.
11. **Refresh token en cookie `HttpOnly`** si frontend y API pasan a compartir dominio registrable (`app.<dominio>` / `api.<dominio>`): sacaría el token de sesión del alcance de JavaScript (3.1).
