# Tarea: entregar los secretos al stack como archivos (Docker secrets)

> Descripción lista para cargar en Jira. Pendiente de implementar. Contexto: [`decisiones-despliegue.md`](decisiones-despliegue.md), sección 5.

**Tipo:** Tarea técnica · **Área:** Infraestructura / Despliegue · **Prioridad:** Alta (bloquea el primer despliegue de `staging`)

## Contexto

Hoy `compose.yaml` pasa `JWT_SECRET`, `DB_APP_PASSWORD`, `DB_OWNER_PASSWORD` y `DB_PASSWORD` como variables de entorno. Eso las deja visibles en `docker inspect` y en `/proc/<pid>/environ`. Es un riesgo menor ya anotado en `docs/pruebas-seguridad.md`. Para los ambientes desplegados (`staging` y `production`) los secretos tienen que llegar como archivos montados en `/run/secrets/`.

El backend ya está preparado: `application.properties` tiene `spring.config.import=optional:configtree:/run/secrets/`. Un archivo llamado `JWT_SECRET` en ese directorio se resuelve como `${JWT_SECRET}` sin cambiar ninguna propiedad. Falta cablear el resto del stack.

## Objetivo

Que en un ambiente desplegado ningún secreto viaje como variable de entorno, sin cambiar el flujo de desarrollo local (`.env` + `docker compose up`).

## Alcance

1. **Archivo `compose.deploy.yaml`** que se suma al `compose.yaml` base (`docker compose -f compose.yaml -f compose.deploy.yaml up -d`). Declara `secrets:` con `file: ${SECRETS_DIR}/<NOMBRE>` y los asigna a cada servicio. Quita las variables de entorno equivalentes con `!reset` (requiere Compose v2.24 o superior). `compose.yaml` y el `.env` de desarrollo no cambian.
2. **backend:** monta `JWT_SECRET` y `DB_APP_PASSWORD`. No recibe ninguna otra credencial.
3. **postgres:** usa `POSTGRES_PASSWORD_FILE`. Adaptar `infra/postgres/initdb/01-crear-owner.sh` para que lea la clave del owner de un archivo (con la variable de entorno como alternativa, para no romper el desarrollo).
4. **migraciones:** Flyway solo acepta `FLYWAY_PASSWORD` como variable. En el `command` del servicio (que ya usa `sh -c`), exportar `FLYWAY_PASSWORD` y `FLYWAY_PLACEHOLDERS_APP_PASSWORD` leyendo los archivos antes de `exec flyway migrate`. Mantener el chequeo que rechaza `$` en `DB_APP_PASSWORD`.
5. **Documentación:** actualizar `decisiones-despliegue.md` (sección 5), `despliegue-seguro.md` y la sección de compose de `CLAUDE.md`.

## Puntos a tener en cuenta

- **Permisos:** `decisiones-despliegue.md` indica permisos `600` para los archivos, pero así los contenedores no pueden leerlos. Compose monta el archivo con el dueño y los permisos del host, y los procesos corren como otro usuario (`spring` en el backend, `postgres` en la base). Dejar el directorio de secretos en `700` para el usuario de despliegue y los archivos en `644`, o `640` con un grupo compartido, y corregir esa línea del documento.
- **Sin salto de línea final:** `JWT_SECRET` se decodifica como base64 y Spring no recorta el contenido del archivo. Escribir cada secreto con `printf '%s'`, nunca con `echo`.
- **Modo `read_only` del backend:** `/run/secrets` es un montaje aparte, así que no entra en conflicto. Verificarlo igual.
- **Fuera de alcance:** el workflow de GitHub Actions que escribe los archivos en la instancia (tarea de CI/CD) y la migración a Parameter Store si más adelante se pasa a EC2.

## Criterios de aceptación

- [ ] Con `compose.yaml` + `compose.deploy.yaml` y perfil `prod`, el stack levanta: `migraciones` termina OK, el backend arranca y el login funciona.
- [ ] `docker inspect` de `backend`, `postgres` y `migraciones` no muestra ningún valor de secreto en `Env`.
- [ ] Los secretos solo aparecen como archivos en `/run/secrets/` de los servicios que los necesitan (el backend no ve la clave del owner ni la del superusuario).
- [ ] `docker compose up` con el `.env` de desarrollo sigue funcionando sin cambios.
- [ ] Con un volumen nuevo, `infra/postgres/initdb/01-crear-owner.sh` crea el owner leyendo la clave de un archivo.
- [ ] Con un `JWT_SECRET` con salto de línea final o menor a 32 bytes, el backend se niega a arrancar con un error claro.
- [ ] La documentación afectada está actualizada.

## Verificación sugerida

1. Crear un directorio de secretos temporal con los archivos (`printf '%s'`, permisos como se definan arriba).
2. `SECRETS_DIR=<ruta> SPRING_PROFILES_ACTIVE=prod docker compose -f compose.yaml -f compose.deploy.yaml up -d --build` (con un volumen nuevo).
3. `docker inspect` de cada servicio, `docker compose exec backend ls -l /run/secrets` y un login real.
