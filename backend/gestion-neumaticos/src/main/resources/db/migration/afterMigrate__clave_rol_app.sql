-- Corre después de cada migrate: la clave de gn_app sigue a DB_APP_PASSWORD, así
-- se rota cambiando solo la variable de entorno.
ALTER ROLE gn_app WITH LOGIN PASSWORD '${app_password}';
