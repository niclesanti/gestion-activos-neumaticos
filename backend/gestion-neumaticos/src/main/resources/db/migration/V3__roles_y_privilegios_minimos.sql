-- Autorización en la base: privilegio mínimo y denegación por defecto.
--
-- Patrón "authenticator" (PostgREST/Supabase): la app se conecta con gn_app, que
-- por sí mismo casi no tiene permisos, y en cada conexión asume (SET ROLE) el rol
-- del nivel de acceso del usuario autenticado. Las políticas RLS identifican al
-- usuario con seguridad.usuario_actual(). El owner (usuario de Flyway) solo
-- ejecuta DDL.
--
-- Los roles son del cluster, no de la base: se crean de forma idempotente.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gn_app') THEN
        -- La clave se fija en el callback afterMigrate, desde DB_APP_PASSWORD.
        CREATE ROLE gn_app LOGIN NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gn_administrador') THEN
        CREATE ROLE gn_administrador NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gn_editor') THEN
        CREATE ROLE gn_editor NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gn_lector') THEN
        CREATE ROLE gn_lector NOLOGIN;
    END IF;
END
$$;

-- gn_app puede asumir cada rol (SET ROLE) pero nunca hereda sus privilegios: una
-- conexión sin usuario autenticado se queda con los permisos mínimos de gn_app.
GRANT gn_administrador TO gn_app WITH INHERIT FALSE, SET TRUE;
GRANT gn_editor TO gn_app WITH INHERIT FALSE, SET TRUE;
GRANT gn_lector TO gn_app WITH INHERIT FALSE, SET TRUE;

-- Denegar por defecto: nadie se conecta ni usa public salvo lo declarado.
DO $$
BEGIN
    EXECUTE format('REVOKE ALL ON DATABASE %I FROM PUBLIC', current_database());
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO gn_app', current_database());
END
$$;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- Las funciones nacen ejecutables por PUBLIC; las tablas, sin grants. Se revoca
-- lo primero para que todo objeto nuevo arranque denegado.
ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

-- Usuario autenticado de la conexión (publicId del JWT), o NULL si no hay
-- sesión. Lo fija la app en cada conexión y lo usan las políticas de todos los
-- módulos. En las políticas se invoca como (SELECT seguridad.usuario_actual())
-- para que se evalúe una vez por consulta y no una vez por fila.
CREATE FUNCTION seguridad.usuario_actual() RETURNS uuid
    LANGUAGE sql
    STABLE
AS $$
    SELECT nullif(current_setting('app.usuario_id', true), '')::uuid
$$;

GRANT USAGE ON SCHEMA seguridad TO gn_app, gn_administrador, gn_editor, gn_lector;
GRANT USAGE ON SCHEMA usuarios TO gn_app, gn_administrador, gn_editor, gn_lector;
GRANT EXECUTE ON FUNCTION seguridad.usuario_actual() TO gn_administrador, gn_editor, gn_lector;
