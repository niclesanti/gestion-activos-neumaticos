# Despliegue: decisiones y servicios

Decisiones de infraestructura para desplegar la plataforma con el menor costo posible. Complementa [`despliegue-seguro.md`](despliegue-seguro.md), donde está la configuración manual de seguridad (TLS, Tunnel, rate limiting de borde, secretos y backups). **Fecha: 2026-11-14.**

> **Criterio rector:** usar servicios de bajo costo y reproducir en la nube el mismo stack de `compose.yaml` que ya se usa y se prueba en local. Así se minimizan las diferencias entre entornos y la configuración que el equipo tiene que aprender.

---

## 1. Ambientes

| Ambiente | Rama de GitHub | Perfil del backend | Estado |
|---|---|---|---|
| `staging` | `develop` | `prod` | Se levanta ahora |
| `production` | `main` | `prod` | Se levanta más adelante |

- **`staging`** es una réplica de `production`: misma infraestructura, mismos servicios y la misma configuración del backend (Java + Spring Boot con el perfil `prod`). Solo cambian los secretos, los datos y los subdominios. Sirve para validar cada versión antes de pasarla a producción y para que el cliente pruebe la plataforma.
- **`production`** se levanta repitiendo los pasos de la sección 6. Se posterga para no pagar dos ambientes mientras el proyecto está en desarrollo.
- **Flujo de ramas:** lo que se integra en `develop` se despliega en `staging`. Cuando una versión está validada, se pasa a `main` y se despliega en `production`.
- **El perfil `dev` queda solo para local.** Carga usuarios seed con contraseñas conocidas y habilita Swagger, así que nunca se usa en un ambiente desplegado.

---

## 2. Servicios

Valen para los dos ambientes: cada ambiente tiene su propia instancia, su propio Tunnel y su propio Worker.

| Componente | Servicio | Motivo |
|---|---|---|
| Frontend (React, build estático) | **Cloudflare Workers (static assets)** | Las solicitudes a archivos estáticos no se cobran. Usa los `_headers` (CSP) que ya genera el build. Cloudflare recomienda Workers en lugar de Pages para proyectos nuevos. |
| Backend (Spring Boot) + PostgreSQL + job de migraciones | **Una instancia de Amazon Lightsail por ambiente** (Linux, Docker Compose) | Precio mensual fijo que incluye cómputo, disco y transferencia. Corre el mismo `compose.yaml` que en local. |
| Exposición del backend y HTTPS | **Cloudflare Tunnel** | La instancia no publica puertos a internet. Cloudflare termina TLS y agrega la protección contra DDoS. |
| DNS / dominio | **Dominio gestionado en Cloudflare** | Lo necesita el Tunnel. Los subdominios se separan por ambiente (ver 6). |
| Secretos | **GitHub Environments** (`staging`, `production`) | Fuente de verdad de los secretos de cada ambiente (ver 5). |
| Observabilidad (métricas, logs, dashboard, alertas) | **Grafana Cloud (plan Free)** + un contenedor **Grafana Alloy** en la instancia (`docker compose --profile observabilidad up -d`) | No se hospeda nada pesado en la instancia: Alloy empuja métricas y logs de forma saliente. Diseño en [`decisiones-observabilidad.md`](../Obs&Tel/decisiones-observabilidad.md). Costo: USD 0. |
| Backups de la base | **Snapshots de Lightsail** o `pg_dump` diario a **Cloudflare R2** | R2 tiene un plan gratuito de almacenamiento y no cobra egreso. |

```
Navegador ──► Cloudflare
               ├─ Workers (frontend estático)
               └─ Tunnel ──► Lightsail (Docker Compose)
                              ├─ backend (Spring Boot, perfil prod)
                              ├─ postgres
                              └─ migraciones (job Flyway)
```

---

## 3. Alternativas descartadas (por ahora)

- **EC2 + RDS:** cada recurso se factura por separado (instancia, disco, IP, transferencia, base), así que el costo es menos predecible y la configuración más compleja.
- **Base gestionada (RDS o base de Lightsail):** cuesta más que la instancia entera. Además, el esquema de roles (owner sin superusuario, `gn_app`, RLS) ya está probado sobre el PostgreSQL de compose y habría que adaptarlo. Se puede reevaluar para `production` si crece la carga, pero en ese caso `staging` tendría que cambiar igual, para seguir siendo idéntico.
- **Levantar `production` desde el inicio:** duplica el costo sin un beneficio inmediato.
- **AWS Systems Manager Parameter Store / Secrets Manager para los secretos:** son la práctica estándar en AWS, pero una instancia de Lightsail no admite roles IAM. Para leerlos habría que guardar en la instancia la access key de un usuario IAM, que es otra credencial de largo plazo, así que no mejora la situación (ver 5).

---

## 4. Configuración común a `staging` y `production`

- **Perfil del backend:** `prod` en los dos ambientes. Swagger queda deshabilitado, y la app no arranca si quedan usuarios del seed con su contraseña por defecto.
- **Secretos propios de cada ambiente:** `JWT_SECRET` (`openssl rand -base64 32`), `DB_APP_PASSWORD` y la contraseña del owner. Nunca se comparten entre ambientes: un token de `staging` no sirve en `production`. Cómo se guardan y cómo llegan al backend se explica en la sección 5.
- **Usuarios:** se crean a mano en cada ambiente, para el equipo y para el cliente.
- **Tamaño de la instancia:** como mínimo **2 GB de RAM**, el mismo plan en los dos ambientes. Los planes más chicos no alcanzan para Spring Boot más PostgreSQL.
- **Región:** la más cercana a los usuarios con un precio razonable (por ejemplo, São Paulo). Confirmar el precio en la consola antes de crear la instancia.

---

## 5. Gestión de secretos

En una instancia propia, los secretos terminan en la memoria del backend sí o sí. Quien tenga acceso de root a la máquina puede leerlos igual. Por eso el objetivo es:

1. Que haya una única fuente de verdad por ambiente, con acceso controlado.
2. Que nadie necesite ver ni copiar los valores a mano.
3. Que en la instancia queden lo menos expuestos posible.

**Fuente de verdad: GitHub Environments.**

- Se crean dos environments en el repositorio, `staging` y `production`, cada uno con sus propios secretos (`JWT_SECRET`, `DB_APP_PASSWORD`, contraseña del owner, token del Tunnel, `GRAFANA_CLOUD_TOKEN` con solo los scopes `metrics:write` y `logs:write`, uno por ambiente).
- La configuración que no es secreta (`FRONTEND_URL`, `SPRING_PROFILES_ACTIVE`, `VITE_API_URL`, y para observabilidad `APP_ENTORNO`, `SCRAPE_INTERVAL=60s`, `METRICS_URL`, `METRICS_USER`, `LOGS_URL`, `LOGS_USER`) va como *variables* del mismo environment.
- El environment `production` exige una aprobación manual antes de desplegar.
- El workflow de despliegue es el único que lee los valores y los escribe en la instancia en cada deploy. La instancia no tiene credenciales de AWS ni de GitHub.

**En la instancia: archivos y no variables de entorno.**

- Los secretos se escriben como archivos en un directorio propio, por ejemplo `/opt/gestion-neumaticos/secrets/`. Pertenecen a un usuario dedicado al despliegue y tienen permisos `600`.
- `compose.yaml` los declara en `secrets:` y los monta dentro de cada contenedor en `/run/secrets/`. Así no aparecen en `docker inspect`, que es el riesgo menor que quedó anotado en [`pruebas-seguridad.md`](pruebas-seguridad.md).
- **Backend:** Spring Boot los lee de forma nativa con `spring.config.import=optional:configtree:/run/secrets/`. Si cada archivo se llama igual que la variable actual (por ejemplo `JWT_SECRET`), las propiedades existentes (`${JWT_SECRET}`) siguen funcionando sin cambios.
- **Otros contenedores:** la imagen de PostgreSQL acepta variables `*_FILE` (por ejemplo `POSTGRES_PASSWORD_FILE`). En el job de migraciones hay que verificar cómo recibe la clave y, si solo acepta variables de entorno, leerla del archivo al arrancar el contenedor.

**Acceso a la instancia.**

- SSH solo con clave, sin contraseña y sin login de root.
- El acceso queda restringido al equipo y al workflow de despliegue (clave guardada como secreto del environment).

**Si más adelante se migra a EC2,** conviene pasar a Parameter Store (`SecureString`, gratuito en el tier estándar) con un rol IAM asignado a la instancia. Esa es la práctica estándar en AWS y elimina las credenciales de largo plazo.

---

## 6. Pasos para levantar un ambiente

Los mismos pasos sirven para `staging` (rama `develop`) y para `production` (rama `main`):

1. Crear la instancia de Lightsail (Linux), instalar Docker y Docker Compose, y crear el usuario de despliegue con acceso SSH solo por clave.
2. Cargar los secretos y las variables en el GitHub Environment del ambiente (ver 5).
3. Desplegar desde la rama del ambiente: el workflow escribe los archivos de secretos en la instancia y levanta el stack con `docker compose up -d`. El job `migraciones` crea el esquema antes de que arranque el backend.
4. Instalar `cloudflared` y crear el Tunnel hacia el backend (`api-staging.<dominio>` / `api.<dominio>`).
5. Publicar el frontend en Workers con `VITE_API_URL` apuntando a la API del ambiente (`staging.<dominio>` / `<dominio>`).
6. Ajustar `FRONTEND_URL` (CORS) y la IP real del cliente (`CF-Connecting-IP`) según [`despliegue-seguro.md`](despliegue-seguro.md).
7. Programar los backups y probar una restauración.
8. Crear los usuarios iniciales.

**Actualización de versión:** un workflow de GitHub Actions por ambiente. Cada push a `develop` despliega en `staging` y cada push a `main`, en `production` (con aprobación manual). El workflow actualiza el código en la instancia, reescribe los secretos, ejecuta `docker compose up -d --build` y publica el frontend en Workers.

---

## 7. Costos estimados

| Concepto | `staging` | `staging` + `production` |
|---|---|---|
| Cloudflare Workers, Tunnel, R2 (dentro del plan gratuito) | USD 0 | USD 0 |
| GitHub Environments y Actions (dentro del plan gratuito) | USD 0 | USD 0 |
| Lightsail, plan de 2 GB | ~USD 12/mes | ~USD 24/mes |
| Dominio (uno solo para los dos ambientes) | ~USD 10–15/año | ~USD 10–15/año |
| **Total** | **~USD 12–15/mes** | **~USD 25–28/mes** |

**Cuenta de AWS:** elegir el **plan Paid** al crearla. Los créditos del Free Tier se aplican igual, pero el plan Free cierra la cuenta (con sus recursos) a los 6 meses o al agotarse los créditos. Configurar una alerta de presupuesto en AWS Budgets.

> Los precios son orientativos a noviembre de 2026; confirmarlos en las páginas oficiales antes de contratar. Verificar también que los environments con secretos y las reglas de aprobación estén disponibles en el plan de GitHub del repositorio (en repositorios privados del plan gratuito tienen limitaciones).
