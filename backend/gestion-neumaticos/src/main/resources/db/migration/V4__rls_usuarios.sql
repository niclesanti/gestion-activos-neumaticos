-- Permisos sobre usuarios.usuarios.
--   Administrador: ve todos, inserta y actualiza (salvo id y public_id). Nunca borra.
--   Editor y lector: solo ven su propio registro.
-- Sin FORCE ROW LEVEL SECURITY: el owner (Flyway, el seed y la función de login)
-- no queda sujeto a las políticas.

ALTER TABLE usuarios.usuarios ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON usuarios.usuarios TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT ON usuarios.usuarios TO gn_administrador;
GRANT UPDATE (nombre_usuario, nombre_apellido, email, clave, nivel_acceso) ON usuarios.usuarios TO gn_administrador;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE usuarios.usuarios_id_seq TO gn_administrador;
-- La validación de esquema de Hibernate (ddl-auto=validate) corre como gn_app y
-- busca las secuencias en information_schema.sequences, que solo muestra las
-- que el rol tiene permitidas. SELECT solo deja leer last_value, no generar ids.
GRANT SELECT ON SEQUENCE usuarios.usuarios_id_seq TO gn_app;

-- Regla global: toda operación exige un usuario autenticado. Al ser RESTRICTIVE
-- se combina con AND con cualquier política permisiva.
CREATE POLICY usuarios_requiere_autenticacion ON usuarios.usuarios
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY usuarios_select_propio ON usuarios.usuarios
    FOR SELECT
    TO gn_editor, gn_lector
    USING (public_id = (SELECT seguridad.usuario_actual()));

CREATE POLICY usuarios_select_admin ON usuarios.usuarios
    FOR SELECT
    TO gn_administrador
    USING (true);

CREATE POLICY usuarios_insert_admin ON usuarios.usuarios
    FOR INSERT
    TO gn_administrador
    WITH CHECK (true);

CREATE POLICY usuarios_update_admin ON usuarios.usuarios
    FOR UPDATE
    TO gn_administrador
    USING (true)
    WITH CHECK (true);

-- El login ocurre antes de que haya un usuario autenticado. En lugar de abrir la
-- tabla a gn_app, se le permite solo esta búsqueda puntual. Corre con los
-- permisos del owner (SECURITY DEFINER) y con search_path vacío, para que no se
-- pueda secuestrar con objetos de otro schema.
CREATE FUNCTION usuarios.buscar_para_login(identificador text) RETURNS SETOF usuarios.usuarios
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = ''
AS $$
    SELECT * FROM usuarios.usuarios WHERE nombre_usuario = identificador OR email = identificador
$$;

REVOKE ALL ON FUNCTION usuarios.buscar_para_login(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION usuarios.buscar_para_login(text) TO gn_app;
