# Implementación: observabilidad (backend)

## Resumen

Archivos (todo en `backend/gestion-neumaticos/`):
- `src/main/resources/application.properties`: bloque de gestión (4.2: puerto `${MANAGEMENT_PORT:8081}`, exposure `health,prometheus`, `show-details=never`, probes, tag `application`, `mbeanregistry`) y bloque ECS (5.1) sin `logging.file.name`.
- `src/main/resources/application-prod.properties`: `logging.threshold.console=WARN`.
- `security/SecurityActuatorConfig.java` (nuevo): `SecurityFilterChain` con `@Order(HIGHEST_PRECEDENCE)`, `securityMatcher(EndpointRequest.toAnyEndpoint())`, stateless, CSRF off, `permitAll` solo `health`/`prometheus`, el resto `denyAll`. `EndpointRequest` en Boot 4.1.1: `org.springframework.boot.security.autoconfigure.actuate.web.servlet`.
- `security/SecurityConfig.java`: se quitó `/actuator/health` e `/actuator/info` del `permitAll`; se agregó `/actuator/**` permitAll (ver desvíos).
- `src/test/.../security/ActuatorGestionTest.java` (nuevo).

`./mvnw clean verify`: BUILD SUCCESS, 709 tests, 0 fallos, JaCoCo "All coverage checks have been met". `ModularityTests` sigue verde. Ningún test existente dependía de `/actuator/*`.

## Trazabilidad

Los tests de `ActuatorGestionTest` cubren:
- `prometheus` y `health` responden 200 sin token en el puerto de gestión. El primero trae `application="gestion-neumaticos-local"` y el segundo no trae `components`.
- `env`, `heapdump`, `loggers` e `info` responden 404 en el puerto de gestión.
- `/actuator/prometheus` y `/actuator/health` responden 404 (no 401) en el puerto de la API.
- `/api/v1/neumaticos` sin token responde 401.

## Desvíos respecto del spec / decisiones

1. **`/actuator/**` permitAll en la cadena principal.** Con puertos separados el matcher `EndpointRequest` no matchea en el puerto 8080, así que la cadena principal respondía 401 en vez de 404. Con el permitAll la petición llega a MVC y responde 404. No expone nada: no hay handlers de actuator en ese puerto, y si algún día se unificaran los puertos, la cadena de actuator (anterior) sigue aplicando `denyAll`. También hace que los endpoints no expuestos den 404 y no 401 en el puerto de gestión.
2. **Test con puerto de gestión fijo libre, no `management.server.port=0`.** Con `RANDOM_PORT` ambos puertos valen 0 y Boot los trata como el mismo puerto, y el matcher no distingue el contexto de gestión (health/prometheus daban 401). El test reserva un puerto libre con `ServerSocket` vía `@DynamicPropertySource`; `@LocalManagementPort` lo lee igual.
3. **`@SpringBootTest` desactiva el export de métricas.** El test fija `management.prometheus.metrics.export.enabled=true`, si no el endpoint prometheus no existe.
4. **HTTP en el test con `java.net.http.HttpClient`**, sin `TestRestTemplate`.

## Observaciones para el leader

- **`pom.xml` tiene `micrometer-registry-prometheus` declarado dos veces**: una al agregarse (compile, línea ~44) y otra preexistente con `scope runtime` (línea ~119). Maven avisa de "duplicate declaration"; funciona, pero conviene borrar una de las dos.
- Algo externo (probablemente un IDE con JDK 25) recompila `target/` con class version 69 al cambiar archivos y rompe surefire con JDK 21 ("compiled by a more recent version"). Se resolvió con `clean verify`. No es un problema del código.
- Quedó un archivo suelto `C:\development\scratch.log` fuera del repo (un log mío con ruta relativa errónea). No pude borrarlo por el bloqueo de seguridad del `rm`; se puede eliminar a mano.
- No toqué Dockerfile, compose, infra ni docs.

## Bloqueos

Ninguno.
