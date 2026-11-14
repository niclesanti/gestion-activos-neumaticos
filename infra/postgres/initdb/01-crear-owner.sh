#!/bin/sh
# Corre una sola vez, al crear el volumen de datos, como superusuario de la
# imagen (POSTGRES_USER). Crea el owner dedicado de la base, que ejecuta las
# migraciones de Flyway: CREATEROLE para crear y administrar los roles gn_*,
# pero sin SUPERUSER ni BYPASSRLS. Desde acá en adelante el superusuario no lo
# usa ningún servicio de la aplicación.
#
# Los valores viajan como variables de psql (:'clave', :"owner"): psql los cita,
# así que una comilla en la clave no rompe ni inyecta la sentencia.
set -eu

: "${DB_OWNER_USER:?Falta DB_OWNER_USER}"
# La clave llega como archivo (DB_OWNER_PASSWORD_FILE, Docker secret) o, como
# alternativa, en la variable DB_OWNER_PASSWORD.
if [ -n "${DB_OWNER_PASSWORD_FILE:-}" ]; then
    [ -r "$DB_OWNER_PASSWORD_FILE" ] || { echo "No se puede leer $DB_OWNER_PASSWORD_FILE" >&2; exit 1; }
    DB_OWNER_PASSWORD="$(cat "$DB_OWNER_PASSWORD_FILE")"
fi
: "${DB_OWNER_PASSWORD:?Falta DB_OWNER_PASSWORD (o DB_OWNER_PASSWORD_FILE)}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v owner="$DB_OWNER_USER" -v clave="$DB_OWNER_PASSWORD" -v base="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"owner" LOGIN CREATEROLE NOSUPERUSER NOBYPASSRLS PASSWORD :'clave';
ALTER DATABASE :"base" OWNER TO :"owner";
SQL
