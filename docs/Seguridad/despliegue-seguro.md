# Despliegue seguro: configuración manual

Lo que **no** puede resolver el código y hay que configurar a mano al desplegar. Complementa [`seguridad.md`](seguridad.md) (diseño) y [`pruebas-seguridad.md`](pruebas-seguridad.md) (pruebas). Última actualización: 2026-10-04.

> El stack de `compose.yaml` es para desarrollo local: publica los puertos solo en `127.0.0.1` y usa HTTP. En producción el TLS, el rate limiting de borde y la protección DDoS los pone Cloudflare.

## Topología recomendada

```
 Navegador ──HTTPS──► Cloudflare ──► Workers static assets   (frontend, app.<dominio>)
                         │
                         └──HTTPS──► Cloudflare Tunnel ──► backend (contenedor, api.<dominio>)
                                                             │  red privada de Docker
                                                             └──► postgres (sin puertos publicados)
```

- **Frontend:** `wrangler deploy` sube `dist/` como *static assets* (`wrangler.jsonc`). El build genera `dist/_headers` con la CSP y las demás cabeceras.
- **Backend:** sin puertos abiertos a internet. `cloudflared` (Cloudflare Tunnel) corre junto al backend y abre la conexión hacia Cloudflare. Así nadie puede saltear Cloudflare (y su WAF/rate limiting) yendo directo a la IP del servidor.
- **Base de datos:** solo en la red privada. Nunca publicar el 5432 ni exponer pgAdmin.

## 1. Secretos y variables

Generar cada secreto al azar y guardarlo en el gestor de secretos del entorno (no en git):

| Variable | Cómo generarla | Notas |
|---|---|---|
| `DB_PASSWORD` | `openssl rand -hex 24` | Superusuario de la imagen. Solo crea el owner al inicializar el volumen. |
| `DB_OWNER_PASSWORD` | `openssl rand -hex 24` | Owner sin `SUPERUSER`: solo lo usa el job de migraciones. |
| `DB_APP_PASSWORD` | `openssl rand -hex 24` | Rol `gn_app`. **Sin `$`**: viaja dentro de un string con dollar quoting; el job de migraciones la rechaza. |
| `JWT_SECRET` | `openssl rand -base64 32` | Base64 de ≥ 32 bytes; la app no arranca si no lo es. |

Las variables de entorno quedan visibles con `docker inspect` para quien tenga acceso al daemon de Docker (por ejemplo, `DB_OWNER_PASSWORD` en el contenedor de Postgres). En producción, restringir ese acceso o pasar los secretos como *Docker secrets* / archivos montados (las imágenes de Postgres admiten `POSTGRES_PASSWORD_FILE`).

Valores de producción:

- `SPRING_PROFILES_ACTIVE=prod`: apaga Swagger y `/api-docs`, deshabilita Flyway en el backend, activa `server.forward-headers-strategy=native` y **no arranca si existe algún usuario del seed de dev con su contraseña por defecto**.
- `FLYWAY_LOCATIONS=filesystem:/flyway/sql/migraciones` (sin `seed-dev`).
- `FRONTEND_URL=https://app.<dominio>` (CORS, sin barra final) y `VITE_API_URL=https://api.<dominio>` (build del frontend: queda en la CSP `connect-src`).
- `VITE_SESSION_IDLE_MINUTES` (opcional, por defecto 30): minutos de inactividad tras los que el frontend cierra la sesión.

## 2. Base de datos

1. Crear el volumen con el script `infra/postgres/initdb/01-crear-owner.sh` montado (crea `DB_OWNER_USER` con `CREATEROLE`, sin `SUPERUSER`/`BYPASSRLS`, dueño de la base). Solo corre con un volumen vacío.
2. Correr el job de migraciones **antes** de cada despliegue del backend: `docker compose run --rm migraciones` (o el equivalente en el orquestador). Aplica las migraciones como owner y fija la clave de `gn_app`.
3. Quitar la sección `ports` de `postgres` y no desplegar `pgadmin`. Para administrar, usar un túnel SSH o Cloudflare Access.
4. Backups cifrados (`pg_dump` programado) fuera del servidor, con prueba de restauración periódica.
5. Si la base no está en la misma red privada que el backend, exigir TLS (`sslmode=verify-full` en `spring.datasource.url`).

## 3. Cloudflare: TLS (A04)

En el dashboard de la zona (**SSL/TLS**):

- **Encryption mode: Full (strict)**. Con Tunnel la conexión al origen ya va cifrada.
- **Edge Certificates → Always Use HTTPS: On**, **Automatic HTTPS Rewrites: On**.
- **Minimum TLS Version: 1.2**, **TLS 1.3: On**.
- **HSTS: On**, `max-age` 12 meses, `includeSubDomains` (la app ya envía la cabecera; esto la aplica también a respuestas del borde). Activar `preload` solo cuando todos los subdominios sirvan HTTPS.

## 4. Cloudflare: IP real del cliente

El límite de intentos del login y los logs usan la IP del cliente. Con Tunnel, el backend recibe los requests desde `cloudflared` (red privada) y Tomcat, con `server.forward-headers-strategy=native`, toma la IP de `X-Forwarded-For` solo si el request viene de un proxy interno.

En `prod` la IP se toma por defecto de **`CF-Connecting-IP`**, que Cloudflare siempre sobrescribe (el cliente no la puede falsificar, a diferencia de `X-Forwarded-For`). Se cambia con `CLIENT_IP_HEADER`.

- Acotar los proxies de confianza a la subred de la red de Docker donde corre `cloudflared` (por defecto Tomcat confía en todas las redes privadas, así que cualquier contenedor de la red podría inyectar una IP): `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES=172\.30\.0\.\d{1,3}` (ajustar a la subred real, definida en compose).
- Si en lugar de Tunnel el backend recibe tráfico directo de Cloudflare, la IP de origen es una IP pública de Cloudflare: hay que poner los [rangos de Cloudflare](https://www.cloudflare.com/ips/) en `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` y un firewall que solo acepte esos rangos.
- Si no hay Cloudflare delante, `CLIENT_IP_HEADER=X-Forwarded-For` y `internal-proxies` con la IP del reverse proxy propio. Sin esto todos los clientes compartirían la IP del proxy y el límite por IP se volvería global.

## 5. Cloudflare: rate limiting y DDoS (A06/A07)

El backend ya limita el login por IP (10/min) y por cuenta (5 fallos/15 min), pero en memoria y por instancia. El borde frena el volumen antes de que llegue al servidor. En **Security → WAF → Rate limiting rules**:

| Regla | Expresión | Límite sugerido | Acción |
|---|---|---|---|
| Login | `http.host eq "api.<dominio>" and http.request.uri.path eq "/api/auth/login" and http.request.method eq "POST"` | 5 requests / 10 s por IP (plan Free: período y bloqueo de 10 s) | Block |
| API general *(planes pagos)* | `http.host eq "api.<dominio>" and starts_with(http.request.uri.path, "/api/")` | 300 requests / 1 min por IP | Managed Challenge o Block |

Además:

- **Security → Bots → Bot Fight Mode: On.**
- **Security → Settings → Security Level: Medium** (subir a *I'm Under Attack* durante un ataque).
- **WAF → Managed rules** (Cloudflare Managed Ruleset y OWASP Core Ruleset) si el plan lo incluye.
- La mitigación DDoS L3/L4/L7 de Cloudflare está activa por defecto; no requiere configuración.

## 6. Frontend en Cloudflare

```bash
cd frontend/gestion-neumaticos
VITE_API_URL=https://api.<dominio> npm run build   # falla si falta VITE_API_URL
npx wrangler deploy
```

Verificar las cabeceras después de cada deploy:

```bash
curl -sI https://app.<dominio>/ | grep -iE "content-security-policy|x-frame-options|strict-transport"
```

Si la API cambia de dominio hay que volver a compilar: la CSP solo permite conectarse al origen de `VITE_API_URL`.

## 7. Backend: más de una instancia

El limitador de intentos (Bucket4j) vive en memoria de cada instancia. Con varias réplicas:

- Pasar a un backend distribuido de Bucket4j (`bucket4j-redis` o `bucket4j-postgresql`), o
- Confiar el límite al borde (regla de login de la sección 5), con sesiones *sticky* por IP como mínimo.

El bulkhead del hash (`PasswordConfig`) también es por instancia, y es lo correcto: protege la CPU y la memoria de cada proceso.

## 8. Rotación de secretos

| Secreto | Procedimiento | Efecto |
|---|---|---|
| `JWT_SECRET` | Cambiar la variable y reiniciar el backend. | Cierra todas las sesiones (los tokens dejan de validar). |
| `DB_APP_PASSWORD` | Cambiar la variable, correr el job de migraciones (aplica la clave nueva) y reiniciar el backend. | Sin corte de sesiones. |
| `DB_OWNER_PASSWORD` | `ALTER ROLE gn_owner PASSWORD '…'` como superusuario y actualizar la variable. | Solo afecta al job de migraciones. |

## 9. Monitoreo y alertas (A09)

- Alertar sobre ráfagas de `Inicio de sesión rechazado` y `Login bloqueado por límite` en los logs del backend (incluyen IP e identificador).
- Revisar **Security → Events** de Cloudflare (bloqueos del WAF y del rate limiting).
- Los 5xx del backend loguean el stack en `ERROR`; los 4xx que provoca el cliente quedan en `WARN` sin stack.

## 10. Repositorio y cadena de suministro (A03)

En la configuración de GitHub:

- **Dependabot alerts** y **Dependabot security updates**: activados (el repo ya trae `.github/dependabot.yml`).
- **Secret scanning** y **push protection**: activados.
- **Branch protection** en `main`: exigir el workflow `CI` (tests + cobertura del backend, lint/typecheck/build y `npm audit` del frontend).
