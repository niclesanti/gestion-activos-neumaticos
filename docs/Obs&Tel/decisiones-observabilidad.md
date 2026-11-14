# Observabilidad: métricas y logs

Diseño del stack de observabilidad del backend: un panel de Grafana con la salud del sistema y los logs centralizados en un formato estándar. Complementa [`decisiones-despliegue.md`](decisiones-despliegue.md) (infraestructura) y [`despliegue-seguro.md`](despliegue-seguro.md) (configuración manual, sección 9: monitoreo y alertas). **Fecha: 2026-11-14.** **Estado:** el stack local está implementado y verificado; la parte de la nube (token y URLs de Grafana Cloud, workflow de despliegue) se completa cuando existan `staging` y `production`.

> **Criterio rector:** el mínimo de piezas y de RAM en la instancia. En la nube **no se hospeda nada** de observabilidad: la instancia solo corre un agente liviano que *empuja* métricas y logs a Grafana Cloud. En local se levantan los mismos componentes que Grafana Cloud usa por dentro (Prometheus, Loki, Grafana) con **el mismo archivo de configuración del agente**: solo cambian los destinos.

---

## 1. Resumen de la decisión

| Pieza | Decisión | Por qué |
|---|---|---|
| Métricas del backend | **Spring Boot Actuator + Micrometer (`micrometer-registry-prometheus`)**, endpoint `/actuator/prometheus` | Es el estándar de Spring. Los dashboards públicos de Spring Boot están hechos para estos nombres de métricas. |
| Formato de logs | **JSON con Elastic Common Schema (ECS)**, nativo de Spring Boot (`logging.structured.format.file=ecs`) | ECS es un esquema estándar y abierto. Spring Boot lo genera sin dependencias extra (desde 3.4). |
| Agente / colector | **Grafana Alloy**, un solo contenedor | Lee las métricas y los logs y los envía a Grafana Cloud. Es el colector oficial de Grafana (Promtail y Grafana Agent quedaron deprecados en su favor). |
| Destino en la nube | **Grafana Cloud, plan Free**: Prometheus (Mimir) para métricas, Loki para logs, Grafana para el panel | 10 000 series activas, 50 GB de logs por mes, 14 días de retención, 3 usuarios. Alcanza de sobra para `staging` y `production` juntos. Costo: USD 0. |
| Destino en local | **Prometheus + Loki + Grafana** en `compose.yaml`, bajo el *profile* `observabilidad-local`. Grafana queda en `127.0.0.1:3001` (el 3000 es del frontend) | Mismas APIs que Grafana Cloud: el agente se configura igual. |
| Dashboard | **[Spring Boot 3.x Statistics (ID 19004)](https://grafana.com/grafana/dashboards/19004)**. Opcional: **[JVM (Micrometer) (ID 4701)](https://grafana.com/grafana/dashboards/4701)** | Ya existen, se importan por ID, usan métricas de Micrometer + Prometheus. |
| Trazas (Tempo) | **Fuera de alcance por ahora** | Con un solo backend monolítico aportan poco. Se pueden sumar después sin cambiar nada de esto (ver 9). |

---

## 2. Arquitectura

### 2.1 En la nube (`staging` y `production`)

```
                    Instancia Lightsail (Docker Compose)
 ┌────────────────────────────────────────────────────────────────────┐
 │                                                                    │
 │  backend ── :8080 (API) ◄──────────── cloudflared ◄── Cloudflare   │
 │     │                                                              │
 │     ├─ :8081 /actuator/prometheus ◄── scrape cada 60 s ──┐         │
 │     │   (puerto de gestión, solo red interna de Docker)  │         │
 │     │                                                    │         │
 │     └─ escribe /logs/backend.json (ECS) ── volumen ──► alloy ──────┼──► HTTPS (saliente)
 │                                            (solo lectura)          │     Grafana Cloud
 │  postgres                                                          │      ├─ Prometheus (métricas)
 │  migraciones                                                       │      ├─ Loki (logs)
 └────────────────────────────────────────────────────────────────────┘      └─ Grafana (dashboard, alertas)
```

- **Todo es saliente.** Alloy *empuja* a Grafana Cloud; no se abre ningún puerto ni se agrega ninguna ruta al Tunnel. Es coherente con "la instancia no publica puertos a internet".
- **El puerto de gestión (`8081`) nunca pasa por el Tunnel.** `cloudflared` sigue apuntando solo a `backend:8080`. Así `/actuator/*` no es alcanzable desde internet aunque alguien adivine la ruta.
- **El backend no conoce a Grafana Cloud.** No recibe ningún token nuevo: sigue recibiendo únicamente `DB_APP_PASSWORD` y `JWT_SECRET` (ver `seguridad.md` 2.9). El token de Grafana Cloud lo tiene solo Alloy.

### 2.2 En local

```
 backend ── :8081 ◄── alloy ──► prometheus (remote write) ──┐
    └─ /logs/backend.json ──► alloy ──► loki ───────────────┤
                                                            └─► grafana  http://127.0.0.1:3001
```

Alloy usa **el mismo `config.alloy`**; solo cambian las URLs y credenciales que recibe por variables de entorno. Si algo funciona en local, funciona en la nube.

---

## 3. Recursos y costos

| Componente | Dónde corre | RAM aproximada | Costo |
|---|---|---|---|
| Alloy | Instancia (nube y local) | ~100–150 MB. Se le pone un tope de **256 MB** en compose | USD 0 |
| Prometheus, Loki, Grafana | **Solo en local** | ~300–500 MB entre los tres | USD 0 |
| Grafana Cloud Free | SaaS | — | USD 0 |

**Por qué entra en la instancia de 2 GB:** se agrega un único proceso liviano. Prometheus, Loki y Grafana (lo pesado) nunca corren en Lightsail.

**Presupuesto de series (límite del plan Free: 10 000):** un backend Spring Boot típico expone entre 500 y 1 500 series. Con `staging` + `production` quedan holgadamente por debajo, siempre que:

- No se habiliten histogramas de percentiles en `http.server.requests` (multiplican las series por la cantidad de buckets).
- No se agreguen etiquetas (*tags*) de alta cardinalidad a las métricas: nunca usuario, IP, id de entidad ni URL cruda. Spring ya usa la URI con plantilla (`/api/v1/unidades-transporte/{publicId}`), que es acotada.
- El *scrape* en la nube sea cada **60 s** (Grafana Cloud factura también por puntos por minuto: con 60 s, 1 punto por serie por minuto).

**Presupuesto de logs (límite: 50 GB/mes):** con `INFO` en producción y sin `DEBUG`, una API de este tamaño genera del orden de decenas o cientos de MB por mes. No es una preocupación.

---

## 4. Métricas (backend)

### 4.1 Dependencias

- `spring-boot-starter-actuator`
- `io.micrometer:micrometer-registry-prometheus` (versión gestionada por el BOM de Spring Boot)

`spring-modulith-starter-insight` (que ya está) sigue funcionando: sus métricas de módulos y eventos también salen por el mismo endpoint.

### 4.2 Configuración (común a todos los perfiles)

```properties
# Puerto de gestión separado: no pasa por el Tunnel ni se publica
management.server.port=${MANAGEMENT_PORT:8081}
# Solo health y prometheus; "info" ya no se expone
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.show-details=never
management.endpoint.health.probes.enabled=true

# Etiqueta que usa el dashboard para elegir la aplicación.
# Se le agrega el ambiente para poder elegir staging/production en el selector del dashboard sin editarlo.
management.metrics.tags.application=${spring.application.name}-${APP_ENTORNO:local}

# Métricas de hilos y sesiones de Tomcat (paneles del dashboard)
server.tomcat.mbeanregistry.enabled=true
```

- `APP_ENTORNO` es una **variable** (no secreto) del GitHub Environment: `staging` o `production`. En local queda `local`.
- **Seguridad del endpoint:** la cadena actual termina en `anyRequest().authenticated()`, así que `/actuator/prometheus` respondería 401 al agente. Hay que agregar un `SecurityFilterChain` propio para actuator, con `securityMatcher(EndpointRequest.toAnyEndpoint())`, `permitAll()` y un `@Order` anterior a la cadena principal. Es seguro porque **la protección es de red**: el puerto 8081 solo existe dentro de la red de Docker. Se exponen solo `health` y `prometheus`; nada de `env`, `heapdump`, `loggers` ni `configprops`.
- `show-details=never`: el health no revela qué base usa ni su estado interno.

### 4.3 Qué muestra el dashboard 19004

Uptime, uso de CPU (proceso y sistema), memoria heap/no-heap, GC, hilos, conexiones del pool HikariCP, requests HTTP (rate, errores 4xx/5xx, latencia media y máxima por endpoint), sesiones y hilos de Tomcat, y nivel de logs (contador de eventos de Logback por nivel: `logback_events_total`).

---

## 5. Logs

### 5.1 Formato: ECS en un archivo

```properties
# Archivo estructurado (lo lee Alloy). La ruta NO va en application.properties
# (rompería la ejecución desde el IDE y los tests): compose la fija con la variable
# de entorno LOGGING_FILE_NAME=/logs/backend.json.
logging.structured.format.file=ecs
logging.structured.ecs.service.name=${spring.application.name}
logging.structured.ecs.service.environment=${APP_ENTORNO:local}

# Rotación: el archivo nunca crece sin límite
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=3
logging.logback.rollingpolicy.total-size-cap=50MB
```

Ejemplo de una línea (un login rechazado):

```json
{"@timestamp":"2026-11-14T20:15:03.120Z","log.level":"WARN","process.thread.name":"http-nio-8080-exec-3","log.logger":"…usuarios.AuthServiceImpl","message":"Inicio de sesión rechazado ip=… identificador=…","service.name":"gestion-neumaticos","service.environment":"staging","ecs.version":"8.11"}
```

**Por qué un archivo en un volumen compartido y no leer la salida de Docker:** para leer los logs de los contenedores, Alloy necesitaría montar el socket de Docker, que equivale a acceso de root sobre la máquina, en un contenedor que además tiene salida a internet y un token. Con el archivo, Alloy solo monta un volumen **de solo lectura** con los logs del backend. Es el mismo criterio de privilegio mínimo de `seguridad.md`.

### 5.2 Consola

La consola sigue en texto legible, para `docker compose logs` y el desarrollo. En `prod` se limita a `WARN` (`logging.threshold.console=WARN`) para no escribir dos veces lo mismo en disco. Además, en compose se acota el log de Docker del backend:

```yaml
logging:
  driver: json-file
  options: { max-size: "10m", max-file: "3" }
```

### 5.3 Niveles por perfil

| Perfil | Root | Paquete de la app | Efecto |
|---|---|---|---|
| `dev` (local) | `INFO` | `DEBUG` | Los `debug` de la capa de servicio llegan al Loki local para depurar. |
| `prod` (`staging` y `production`) | `INFO` | `INFO` | No se generan ni se envían `DEBUG`/`TRACE`. Si hace falta depurar en `staging`, se sube el nivel por variable de entorno (`LOGGING_LEVEL_<PAQUETE>=DEBUG`) y se redespliega. |

No filtrar por nivel en el agente: lo que no sirve no se genera. Es lo más barato en CPU, disco y red.

### 5.4 Cómo se guardan en Loki (eficiencia)

Loki indexa solo las **etiquetas** (*labels*); el cuerpo del log se guarda comprimido y se consulta con filtros. Por eso:

| Etiqueta | Valores | Origen |
|---|---|---|
| `service_name` | `gestion-neumaticos-backend` | Fija en Alloy |
| `environment` | `local`, `staging`, `production` | Variable `APP_ENTORNO` |
| `level` | `ERROR`, `WARN`, `INFO`, `DEBUG` | Campo `log.level` del JSON |

**Nada más va como etiqueta**: ni logger, ni IP, ni identificador, ni usuario. Esos campos quedan dentro del JSON y se consultan así:

```logql
{service_name="gestion-neumaticos-backend", environment="production", level="WARN"}
  | json
  | message =~ "Inicio de sesión rechazado.*"
```

La marca de tiempo del evento se toma de `@timestamp` (no la hora en que Alloy lo leyó).

### 5.5 Datos personales

Los logs de seguridad ya incluyen IP e identificador (`seguridad.md` 2.5). Al enviarlos a Grafana Cloud quedan en un tercero:

- Elegir la región del *stack* de Grafana Cloud al crearlo (la más cercana disponible).
- La retención del plan Free (14 días) es una ventaja: minimiza lo que se guarda.
- No agregar más datos personales a los logs. Nunca contraseñas, tokens ni el header `Authorization` (ya es así).

---

## 6. Alloy

### 6.1 Configuración (`infra/observabilidad/alloy/config.alloy`)

Borrador de referencia. Verificar la sintaxis contra la versión de Alloy que se fije (en versiones viejas `sys.env` se llamaba `env`).

```alloy
// ── Credencial (archivo montado como secreto) ─────────────────────
local.file "grafana_token" {
  filename  = "/run/secrets/GRAFANA_CLOUD_TOKEN"
  is_secret = true
}

// ── Métricas ──────────────────────────────────────────────────────
prometheus.scrape "backend" {
  targets         = [{"__address__" = "backend:8081"}]
  job_name        = "gestion-neumaticos-backend"
  metrics_path    = "/actuator/prometheus"
  scrape_interval = sys.env("SCRAPE_INTERVAL")   // 60s en la nube, 15s en local
  // (el target también lleva "namespace" = APP_ENTORNO: lo exige el selector del dashboard 19004)
  forward_to      = [prometheus.remote_write.destino.receiver]
}

prometheus.remote_write "destino" {
  external_labels = { environment = sys.env("APP_ENTORNO") }
  endpoint {
    url = sys.env("METRICS_URL")
    basic_auth {
      username = sys.env("METRICS_USER")
      password = local.file.grafana_token.content
    }
  }
}

// ── Logs ──────────────────────────────────────────────────────────
local.file_match "backend" {
  path_targets = [{"__path__" = "/logs/backend.json", "service_name" = "gestion-neumaticos-backend"}]
}

loki.source.file "backend" {
  targets    = local.file_match.backend.targets
  forward_to = [loki.process.backend.receiver]
}

loki.process "backend" {
  stage.json {
    expressions = { level = "\"log.level\"", ts = "\"@timestamp\"" }
  }
  stage.labels    { values = { level = "" } }
  stage.timestamp { source = "ts"  format = "RFC3339" }
  forward_to = [loki.write.destino.receiver]
}

loki.write "destino" {
  external_labels = { environment = sys.env("APP_ENTORNO") }
  endpoint {
    url = sys.env("LOGS_URL")
    basic_auth {
      username = sys.env("LOGS_USER")
      password = local.file.grafana_token.content
    }
  }
}
```

> **Verificar el formato ECS real.** Según la versión de Spring Boot, ECS sale con claves planas (`"log.level"`) o anidadas (`"log": {"level": …}`). Mirar una línea de `/logs/backend.json` y ajustar las expresiones de `stage.json` (`"\"log.level\""` para claves planas, `"log.level"` para anidadas).

### 6.2 Servicio en `compose.yaml`

```yaml
alloy:
  image: grafana/alloy:v1.22.0
  profiles: ["observabilidad", "observabilidad-local"]   # nube: --profile observabilidad; local: observabilidad-local
  command: ["run", "--storage.path=/var/lib/alloy/data", "/etc/alloy/config.alloy"]
  environment:
    APP_ENTORNO, SCRAPE_INTERVAL, METRICS_URL, METRICS_USER, LOGS_URL, LOGS_USER   # desde .env / GitHub Environment
  secrets: [GRAFANA_CLOUD_TOKEN]   # local: fuente "environment" (valor de relleno en .env); nube: archivo escrito por el workflow
  volumes:
    - ./infra/observabilidad/alloy/config.alloy:/etc/alloy/config.alloy:ro
    - logs-backend:/logs:ro          # mismo volumen que escribe el backend
    - alloy-data:/var/lib/alloy/data # posiciones de lectura: no reenvía todo tras un reinicio
  mem_limit: 256m
  restart: unless-stopped
  # sin "ports": la UI de Alloy (12345) no se publica
```

- El backend monta `logs-backend:/logs` con escritura; Alloy, de solo lectura.
- Alloy no se conecta a Postgres ni al socket de Docker.

### 6.3 Variables y secretos por ambiente

En cada **GitHub Environment** (`staging`, `production`), siguiendo `decisiones-despliegue.md` sección 5:

| Nombre | Tipo | Valor |
|---|---|---|
| `GRAFANA_CLOUD_TOKEN` | **Secreto** | Token de una *access policy* de Grafana Cloud con **solo** los scopes `metrics:write` y `logs:write`. Uno por ambiente, para poder revocarlos por separado. |
| `METRICS_URL`, `METRICS_USER` | Variable | URL de *remote write* de Prometheus y su usuario numérico (Grafana Cloud → stack → Prometheus → Details) |
| `LOGS_URL`, `LOGS_USER` | Variable | URL de *push* de Loki y su usuario numérico (stack → Loki → Details) |
| `APP_ENTORNO` | Variable | `staging` / `production` |
| `SCRAPE_INTERVAL` | Variable | `60s` |

El workflow escribe `GRAFANA_CLOUD_TOKEN` como archivo en el directorio de secretos de la instancia, igual que los demás. El token **solo puede escribir**: si se filtra, no permite leer los datos ni los dashboards.

Un solo *stack* de Grafana Cloud alcanza para los dos ambientes: se distinguen por la etiqueta `environment` y por el selector `application` del dashboard.

---

## 7. Stack local

Bajo el *profile* de compose `observabilidad-local`, para no levantarlo siempre (un `docker compose up -d` normal no arranca nada de esto; en la nube se usa `--profile observabilidad`, que solo incluye a Alloy). Imágenes en versiones fijas: `grafana/alloy:v1.22.0`, `prom/prometheus:v3.16.0`, `grafana/loki:3.7.10`, `grafana/grafana:13.3.1`.

| Servicio | Imagen | Notas |
|---|---|---|
| `prometheus` | `prom/prometheus` | Con `--web.enable-remote-write-receiver` (recibe lo que manda Alloy, no hace scrape propio) y `--storage.tsdb.retention.time=3d`. Sin puertos publicados. |
| `loki` | `grafana/loki` | Modo *single binary* con la configuración por defecto (almacenamiento en filesystem). Sin puertos publicados. |
| `grafana` | `grafana/grafana` | `127.0.0.1:3001:3000` (el 3000 ya es del frontend), igual que el resto de los puertos locales. *Provisioning* de los datasources (Prometheus y Loki) y del dashboard desde archivos versionados. |
| `alloy` | El mismo servicio de 6.2 | En `.env` local: `METRICS_URL=http://prometheus:9090/api/v1/write`, `LOGS_URL=http://loki:3100/loki/api/v1/push`, `SCRAPE_INTERVAL=15s`, usuario y token de relleno (Prometheus y Loki locales no piden autenticación). |

Archivos a versionar:

```
infra/observabilidad/
├── alloy/config.alloy
└── grafana/
    ├── provisioning/datasources/datasources.yaml   # Prometheus y Loki con uid fijos
    ├── provisioning/dashboards/dashboards.yaml     # carga los JSON de ../../dashboards
    └── dashboards/spring-boot-3x-statistics.json   # JSON del 19004 descargado de grafana.com
```

> Los JSON de grafana.com traen la variable `${DS_PROMETHEUS}` para el datasource. Para cargarlos por *provisioning* hay que reemplazarla por el `uid` fijo del datasource de Prometheus local.

Uso:

```bash
docker compose --profile observabilidad-local up -d
# Grafana en http://127.0.0.1:3001 → dashboard "Spring Boot 3.x Statistics"
# Logs: Explore → Loki → {service_name="gestion-neumaticos-backend"}
```

---

## 8. Grafana Cloud: configuración manual

Una sola vez:

1. Crear la cuenta en el plan **Free** y un *stack* (elegir la región).
2. **Access policies:** crear una política con scopes `metrics:write` y `logs:write`, y un token por ambiente. Cargarlos en los GitHub Environments (6.3).
3. **Importar el dashboard:** Dashboards → New → Import → ID `19004` → datasource `grafanacloud-<stack>-prom`. Opcional: `4701` (JVM en detalle).
4. **Alertas mínimas** (Alerting → Alert rules), con notificación por email:

   | Alerta | Condición | Fuente |
   |---|---|---|
   | Backend caído | `up{job="gestion-neumaticos-backend"} == 0` durante 2 min | Métricas |
   | Errores 5xx | tasa de `http_server_requests_seconds_count{status=~"5.."}` > 0 sostenida 5 min | Métricas |
   | Heap alta | heap usada / máxima > 90 % durante 10 min | Métricas |
   | Pool de conexiones agotado | `hikaricp_connections_pending` > 0 durante 2 min | Métricas |
   | Ráfaga de logins rechazados | `count_over_time({service_name="gestion-neumaticos-backend", level="WARN"} \|= "Inicio de sesión rechazado" [5m])` > 50 | Logs |

   La última cubre lo que pide `despliegue-seguro.md` sección 9 ("alertar sobre ráfagas de inicio de sesión rechazado y login bloqueado por límite").

---

## 9. Alternativas descartadas (por ahora)

- **Hospedar Prometheus, Loki y Grafana en la instancia:** necesitan entre 300 y 500 MB más y disco propio. En una instancia de 2 GB con Spring Boot y PostgreSQL, no entra con margen, y habría que mantenerlos, protegerlos y hacerles backup.
- **Enviar por OTLP directo desde el backend (sin agente):** ahorra un contenedor, pero el backend tendría que guardar el token de Grafana Cloud (rompe "el backend solo recibe `DB_APP_PASSWORD`"), los logs necesitan un *appender* de OpenTelemetry extra, y los nombres de métricas convertidos desde OTLP no siempre coinciden con los que esperan los dashboards públicos de Prometheus.
- **Promtail o Grafana Agent:** deprecados en favor de Alloy.
- **Leer los logs de Docker con el socket montado:** equivale a dar root al contenedor del agente (ver 5.1).
- **Imagen todo-en-uno `grafana/otel-lgtm` para local:** es cómoda, pero la ruta de datos no sería la misma que en la nube (usa un colector OpenTelemetry, no Alloy con esta configuración).
- **Trazas distribuidas (Tempo):** con un único backend aportan poco. Si se agregan más adelante (`micrometer-tracing-bridge-otel` + exportador OTLP hacia Alloy), ECS incluye el `trace.id` en cada log y Grafana permite saltar del log a la traza.
- **Métricas de PostgreSQL y del host:** Alloy las trae integradas (`prometheus.exporter.postgres`, `prometheus.exporter.unix`) sin contenedores extra. Quedan como siguiente paso: la de PostgreSQL requiere un rol de monitoreo (`gn_monitor` con `pg_monitor`, sin acceso a datos) que se agrega con una migración, y la del host suma series. Mientras tanto, CPU, red y disco de la instancia se ven en la consola de Lightsail.

---

## 10. Instrucciones para el agente

Orden sugerido. Cada paso con su criterio de aceptación.

1. **Dependencias y propiedades de métricas (4.1, 4.2).**
   - ✔ `curl http://localhost:8081/actuator/prometheus` (desde la red de Docker) devuelve métricas con `application="gestion-neumaticos-local"`.
   - ✔ `GET /actuator/prometheus` y `/actuator/health` en el puerto **8080** responden 404.
2. **`SecurityFilterChain` de actuator (4.2).**
   - ✔ Test `@SpringBootTest` (con puerto de gestión) que verifica: `prometheus` y `health` responden 200 sin token en 8081; `env`, `heapdump` y `loggers` no están expuestos; la cadena principal sigue exigiendo JWT. Mantener la cobertura exigida por `./mvnw verify`.
3. **Logs ECS en archivo y niveles por perfil (5.1–5.3).** Confirmar que la versión de Spring Boot es 3.4 o superior.
   - ✔ `/logs/backend.json` tiene una línea JSON por evento; rota a los 10 MB; `dev` incluye `DEBUG` del paquete de la app y `prod` no.
   - ✔ Ningún log contiene contraseñas, tokens ni el header `Authorization`.
4. **Alloy y stack local (6.1, 6.2, 7)**, con imágenes en versiones fijas.
   - ✔ `docker compose --profile observabilidad-local up -d` levanta todo; el dashboard 19004 aparece cargado y con datos en `http://127.0.0.1:3001`.
   - ✔ En Explore → Loki se ven los logs con las etiquetas `service_name`, `environment` y `level`, y `| json` extrae los campos ECS.
   - ✔ Ningún servicio nuevo publica puertos salvo Grafana en `127.0.0.1:3001`.
5. **Integración con el despliegue (6.3).** El workflow escribe `GRAFANA_CLOUD_TOKEN` como archivo de secreto y levanta `alloy` en la instancia (sin los servicios del profile local). Verificar que `cloudflared` apunte solo a `backend:8080`.
6. **Documentación.** Actualizar `decisiones-despliegue.md` (fila de Grafana Cloud en servicios y costos, nuevo secreto y variables en la sección 5) y `despliegue-seguro.md` sección 9 (enlazar las alertas de este documento).

Pasos manuales (no los hace el agente): crear la cuenta y el stack de Grafana Cloud, los tokens, importar el dashboard y crear las alertas (sección 8).

> Los límites y precios de Grafana Cloud son de noviembre de 2026; confirmarlos en [grafana.com/pricing](https://grafana.com/pricing/) antes de crear la cuenta.
