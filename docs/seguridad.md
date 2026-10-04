# Seguridad: autenticación y autorización

Decisiones de diseño tomadas al implementar **HU-01 (Iniciar sesión)**, **HU-02 (Cerrar sesión)** y la autorización por rol (frontend, backend y base de datos), y lo que queda pendiente. Última actualización: 2026-10-02.

> **Idea rectora:** el frontend mejora la experiencia de uso; el backend es la única barrera real de seguridad. Todo lo que el frontend oculta o bloquea tiene que estar también protegido en el backend (ver [Pendiente](#pendiente)).

---

## 1. Visión general

```
 Navegador (React)                          Backend (Spring Boot)
 ─────────────────                          ─────────────────────
 LoginCard ── POST /api/auth/login ───────► AuthController ─► AuthServiceImpl
                                                                ├─ UsuarioRepository (Argon2id)
            ◄── { token JWT, expiraEn, usuario } ───────────────┘─ TokenService.generar()

 axios ── Authorization: Bearer <JWT> ────► Resource server (filtros de Spring Security)
                                              ├─ firma HS256, exp, iss
                                              └─ jti no revocado (seguridad.tokens_revocados)

 Logout ── POST /api/auth/logout ─────────► TokenService.revocar(jti)
```

| Pieza | Dónde |
|---|---|
| Configuración de Spring Security, JWT, hash, revocación | `backend/.../security/` (módulo Modulith) |
| Usuarios, login, logout, `/me` | `backend/.../usuarios/` |
| Migraciones | `db/migration/V1__crear_tabla_usuarios.sql`, `V2__crear_tabla_tokens_revocados.sql`; roles, permisos y RLS en `V3`–`V6` |
| Rol de base por conexión | `backend/.../security/bd/` |
| Sesión, guards, formulario | `frontend/.../src/features/auth/` |
| Reglas de acceso de la UI | `frontend/.../src/lib/access.ts` |

---

## 2. Backend

### 2.1 Organización en módulos

- La seguridad vive en su propio módulo Modulith, `security/`, y no en `config/`. Su API pública es solo `TokenService` y `TokenEmitido` (paquete base). Lo demás queda interno, en `jwt/`, `revocacion/` y `web/`.
- `usuarios` depende de `security` y no al revés: el módulo de seguridad no sabe qué es un usuario y solo emite o revoca tokens para un `publicId` y un nivel de acceso.
- `ModularityTests` verifica estos límites (`ApplicationModules.verify()`).

### 2.2 Autenticación con JWT (stateless)

- **Mecanismo:** el resource server OAuth2 de Spring Security, con Nimbus. Es el soporte estándar del framework, así que no hace falta una librería extra como jjwt ni un filtro escrito a mano.
- **Algoritmo:** HS256 (HMAC) con una clave compartida (`JWT_SECRET`). Alcanza porque un único servicio emite y valida los tokens. La aplicación **no arranca** si el secreto falta o mide menos de 32 bytes (256 bits).
- **Transporte:** header `Authorization: Bearer`, sin cookies. Por eso **CSRF está deshabilitado**: el navegador no adjunta el token automáticamente, así que no hay petición cruzada que falsificar.
- **Sesión:** `SessionCreationPolicy.STATELESS`. El servidor no guarda sesión HTTP.
- **Claims:**

  | Claim | Valor | Por qué |
  |---|---|---|
  | `sub` | `publicId` (UUID) del usuario | El `id` interno (Long) nunca sale del módulo `usuarios` (ver diagrama de dominio). |
  | `jti` | UUID aleatorio | Permite revocar un token puntual. |
  | `iss` | `gestion-neumaticos` | Se exige al validar. |
  | `iat` / `exp` | emisión y expiración | Vigencia por defecto `PT8H` (un turno), configurable en `app.security.jwt.expiracion`. |
  | `nivelAcceso` | `ROLE_ADMINISTRADOR` / `ROLE_EDITOR` / `ROLE_LECTOR` | Se usa tal cual como *authority* de Spring Security. |

- **Validación en cada request:** firma, `exp`, `iss` y que el `jti` no esté revocado.

### 2.3 Cierre de sesión e invalidación del token (HU-02)

Un JWT no tiene estado: una vez firmado, es válido hasta que vence. Para que "cerrar sesión" invalide de verdad el token, se mantiene una **lista de revocación**:

- `POST /api/auth/logout` guarda el `jti` y su expiración en `seguridad.tokens_revocados`.
- `TokenNoRevocadoValidator` rechaza cualquier token cuyo `jti` esté en esa tabla, y también los tokens sin `jti` o con un `jti` que no sea un UUID.
- Una tarea diaria (`LimpiezaTokensRevocadosTask`, 03:00) borra las filas ya vencidas, que de todos modos rechaza la validación de `exp`. La tabla solo crece con logouts de tokens que todavía no vencieron.

Opciones descartadas:

- **Tokens de vida muy corta sin revocación:** el logout no sería inmediato.
- **Sesión de servidor:** se pierde la ventaja de una API sin estado y complica escalar.

### 2.4 Almacenamiento de contraseñas

- **Argon2id** mediante `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`. Es el algoritmo que recomienda hoy OWASP y, a diferencia de bcrypt, no trunca la contraseña a 72 bytes.
- Va envuelto en un **`DelegatingPasswordEncoder`**: cada hash se guarda con prefijo (`{argon2}$argon2id$v=19$m=…`), así que más adelante se puede cambiar de algoritmo o de parámetros sin invalidar las contraseñas existentes.
- Nunca se persiste ni se loguea la contraseña en texto plano. `LoginRequestDTO` y `Usuario` sobrescriben `toString` para no exponer la contraseña ni el hash. `Usuario` no usa `@Data` por ese motivo.

### 2.5 Login resistente a la enumeración de cuentas

- **Mensaje único:** usuario inexistente y contraseña incorrecta devuelven el mismo **401 "Credenciales inválidas"** (`CredencialesInvalidasException`).
- **Tiempo constante:** si el usuario no existe, igual se ejecuta un `passwordEncoder.matches` contra un *hash dummy* precalculado. Así la respuesta tarda lo mismo y no se puede deducir por tiempos qué cuentas existen.
- Los logs registran el resultado del intento, pero no el identificador cuando el login falla, ni datos sensibles.

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
- El handler genérico de 500 **no devuelve `ex.getMessage()`** al cliente. El detalle queda solo en el log.

### 2.9 CORS y secretos

- CORS admite un único origen (`FRONTEND_URL`).
- `JWT_SECRET` y las credenciales de base viven en el `.env` raíz (fuera de git).
- **Usuarios de prueba:** se cargan solo en el perfil `dev`, con el seed repetible `db/seed/dev/R__usuarios_dev.sql` que se suma a `spring.flyway.locations` en `application-dev.properties`. Hay dos editores y dos lectores para poder comprobar que cada uno ve solo su fila. En `prod` no existen.
- **Credenciales de base:** `DB_USER`/`DB_PASSWORD` son las del owner y solo las usa Flyway. La app se conecta como `gn_app`, con `DB_APP_PASSWORD` (ver 2.11).

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

### 2.11 Autorización en la base de datos

Principio de **privilegio mínimo y denegación por defecto**. Sigue el patrón *authenticator* de PostgREST y Supabase: aunque una consulta del backend tuviera un bug, la base solo devuelve o modifica lo que el rol del usuario puede tocar.

**Roles** (`V3__roles_y_privilegios_minimos.sql`):

| Rol | Tipo | Qué puede hacer |
|---|---|---|
| Owner (`DB_USER`) | dueño de schemas y tablas | Solo DDL, desde Flyway. La app nunca se conecta con él. |
| `gn_app` | `LOGIN NOINHERIT` | Rol de conexión del pool. Por sí mismo solo puede: ejecutar `usuarios.buscar_para_login`, leer `seguridad.tokens_revocados` y borrar los tokens ya vencidos. |
| `gn_administrador`, `gn_editor`, `gn_lector` | `NOLOGIN` | Los permisos reales. `gn_app` puede asumirlos (`SET ROLE`), pero no los hereda (`WITH INHERIT FALSE`). |

La clave de `gn_app` la fija el callback `afterMigrate__clave_rol_app.sql` desde `DB_APP_PASSWORD` en cada arranque.

**Cómo llega el usuario a la base.** `RolBaseDatosDataSource` (módulo `security`) envuelve el datasource. En cada conexión que se toma del pool ejecuta un único statement parametrizado:

```sql
SELECT set_config('role', 'gn_editor', false), set_config('app.usuario_id', '<publicId del JWT>', false)
```

- El rol sale de una lista blanca según el claim `nivelAcceso`.
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

**`seguridad.tokens_revocados`** (`V5`):

- `gn_app` la lee, porque comprobar la revocación es parte de autenticar el token.
- `gn_app` borra solo filas con `expira_en < now()`: la purga diaria no puede borrar un token vigente.
- Los tres roles insertan (logout) y leen.

**Excepciones documentadas:**

- `public.flyway_schema_history`: solo la toca el owner.
- `public.event_publication` (Spring Modulith, `V6`): sin RLS, porque es infraestructura y los listeners asíncronos corren sin usuario.

**Por qué no un enum de PostgreSQL para el nivel de acceso:**

- Solo lo usa una tabla, y el `CHECK` existente ya restringe los valores.
- Un enum no permite quitar valores y complica el mapeo de Hibernate.
- Los niveles ya están modelados como roles de base.

**Riesgo aceptado:** la base confía en que `gn_app` fija el rol correcto. Una inyección SQL en la app podría ejecutar `SET ROLE gn_administrador`, como en cualquier esquema de este tipo. Por eso todo el acceso pasa por JPA o consultas parametrizadas.

**Salvaguardas:**

- La app no arranca si se conecta con un rol superusuario o con `BYPASSRLS`. Esto pasaría, por ejemplo, si se conectara con las credenciales del owner, y todas las políticas quedarían salteadas en silencio.
- `AutorizacionBaseDatosTests` falla si aparece una tabla nueva sin RLS.

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
  - Riesgo aceptado: un XSS podría leer el token. La alternativa, una cookie `HttpOnly`, se descartó porque el frontend (Cloudflare) y el backend estarían en sitios distintos. Eso obligaría a usar `SameSite=None` y volver a activar la protección CSRF. Ver [Pendiente](#pendiente) para las mitigaciones.
- El token nunca viaja por la URL ni queda en `localStorage`.

### 3.2 Flujo de sesión

- **Envío del token:** `src/lib/api/client.ts` agrega el header `Authorization: Bearer` a cada request. Como `src/lib` no puede importar features, el feature `auth` registra cómo obtener el token y qué hacer ante un 401 (`configureApiClient`).
- **Token rechazado:** ante cualquier 401 (salvo el del propio login), se limpia la sesión local, se vacía la caché de react-query y se avisa "Tu sesión expiró".
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

---

## 4. Verificación

- **Backend:** `./mvnw test` corre 95 tests. Necesita Docker: los tests de integración levantan `postgres:18-alpine` con Testcontainers, porque roles, GRANT y RLS no existen en una base embebida.
  - Unit tests del service con Mockito, incluidos el hash dummy y la normalización.
  - `@WebMvcTest` del controller con la cadena de seguridad real: endpoints públicos y protegidos, validaciones 400, 401 por credenciales y por token revocado o ausente, y login que ignora un Bearer.
  - `AutorizacionPorRolTest`: jerarquía de roles con `@PreAuthorize` y 403 en JSON.
  - `AutorizacionBaseDatosTests`, contra PostgreSQL real:
    - Rol de cada conexión.
    - Cada rol ve y modifica solo lo permitido.
    - El administrador no borra ni cambia el `public_id`.
    - Sin sesión no se lee la tabla, pero el login funciona.
    - Logout y purga de tokens.
    - Ninguna tabla sin RLS.
  - El emisor y el validador de JWT con el encoder y decoder reales: token expirado, firmado con otra clave o revocado.
  - El validador del identificador.
  - La estructura modular.
  - El arranque completo del contexto.
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
- **Base, con `psql` como `gn_app`:**
  - Sin rol, `SELECT` sobre `usuarios` → `permission denied`.
  - `gn_lector` ve 1 fila y no puede actualizar ni borrar.
  - `gn_administrador` ve todas y actualiza, pero `DELETE` y el cambio de `public_id` → `permission denied`.
  - `gn_administrador` sin `app.usuario_id` ve 0 filas.
  - `SET ROLE` al owner → denegado.

---

## Pendiente

Ordenado por prioridad.

### Alta

1. **Aplicar la autorización por rol en cada endpoint nuevo.** El mecanismo está listo (`@PreAuthorize` + `RoleHierarchy`, ver 2.10). Falta anotar cada endpoint de negocio a medida que se cree, y definir los permisos de lectura del lector (frontend, backend y políticas de base) con la convención de 2.10 y 2.11.
2. **Owner no superusuario en producción.** Hoy Flyway migra con el superusuario de la imagen de PostgreSQL. En producción conviene un owner dedicado con `CREATEROLE` (para los roles `gn_*`) y sin `SUPERUSER`.
3. **Límite de intentos y bloqueo temporal en el login.** Limitar por IP y por cuenta, por ejemplo con 5 fallos y bloqueo progresivo (Bucket4j o un filtro con contador), y evaluar un CAPTCHA tras varios fallos. Hoy el login no tiene protección contra fuerza bruta.
4. **Subir los parámetros de Argon2id.** Los valores por defecto de Spring (`m=16 MiB, t=2, p=1`) quedan un poco por debajo del mínimo de OWASP (`m=19 MiB, t=2, p=1`). Se corrige con `new Argon2PasswordEncoder(16, 32, 1, 19456, 2)` en `PasswordConfig`, midiendo antes el tiempo por hash en el servidor real. Los hashes existentes siguen validando porque cada uno guarda sus parámetros.
5. **HTTPS obligatorio en producción**, con HSTS. Las credenciales y el token viajan en claro sobre HTTP.

### Media

6. **Revocar todas las sesiones de un usuario** al cambiar la contraseña, darlo de baja o cambiarle el nivel de acceso. Hoy solo se revoca el token puntual del logout, y el nivel de acceso viaja en el token, así que un cambio de rol no rige hasta que vence. Opción: un campo `version_token` en `usuarios`, incluido como claim y comparado al validar.
7. **Baja lógica de usuarios** (`activo` / `deletedAt`, según `AuditableEntity` del diagrama) y rechazo del login para usuarios inactivos.
8. **Mitigar XSS**, ya que el token está en `sessionStorage`: una Content-Security-Policy estricta en `nginx.conf`, más los headers `X-Content-Type-Options`, `Referrer-Policy` y `frame-ancestors`.
9. **Expiración en el frontend:** hoy la sesión vencida se detecta al navegar o al recibir un 401. Falta avisar antes de que expire o cerrar la sesión automáticamente al llegar a `expiraEn`, y evaluar un *refresh token* si 8 horas resulta corto o largo para la operación.
10. **Auditoría de eventos de seguridad** (logins exitosos y fallidos, logouts, cambios de rol) en el módulo `trazabilidad`, que alimentaría la pantalla de Auditoría.
11. **Exposición en producción:** deshabilitar Swagger y `/api-docs` en `prod` (`springdoc.api-docs.enabled=false`, el propio log lo advierte), y fijar `spring.jpa.open-in-view=false`.

### Baja

12. **Rotación de la clave JWT:** soportar varias claves con `kid` para rotar `JWT_SECRET` sin cerrar todas las sesiones. Si otros servicios tuvieran que validar tokens, pasar a firma asimétrica (RS256 o EdDSA).
13. **Rendimiento de la revocación:** cada request consulta `tokens_revocados`. Si el volumen crece, cachear la consulta (Caffeine con TTL igual a la vida del token) o mover la lista a Redis.
14. **Política de contraseñas para el alta y el cambio de contraseña** (largo mínimo, chequeo contra contraseñas filtradas). No aplica al login.
15. **Tests del frontend** (schema de login, guards, `syncSessionOnPageRestore`). Hoy la cobertura del frontend es solo la prueba end-to-end manual.
16. **Análisis de dependencias** (OWASP Dependency-Check o Dependabot) en CI.
