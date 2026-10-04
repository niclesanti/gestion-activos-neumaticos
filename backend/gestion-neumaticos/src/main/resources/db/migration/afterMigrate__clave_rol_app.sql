-- Corre después de cada migrate: la clave de gn_app sigue a DB_APP_PASSWORD, así
-- se rota cambiando solo la variable de entorno.
--
-- La clave llega como placeholder de Flyway y se pasa por format('%L'), así que
-- una comilla no rompe la sentencia (que corre como owner). Lo único que podría
-- cortar el string es una de las etiquetas de dollar quoting de este archivo,
-- y las dos llevan "$": por eso la clave no puede contener "$" (el job de
-- migraciones de compose.yaml la rechaza antes de correr; las generadas con
-- "openssl rand -hex" nunca lo tienen).
-- El "#" inicial (que substr descarta) evita escribir "$${", que Flyway
-- interpreta como un placeholder escapado y no lo reemplaza.
DO $bloque_clave_gn_app$
BEGIN
    EXECUTE format('ALTER ROLE gn_app WITH LOGIN PASSWORD %L',
                   substr($clave_gn_app$#${app_password}$clave_gn_app$, 2));
END
$bloque_clave_gn_app$;
