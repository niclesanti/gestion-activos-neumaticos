-- Nivel de acceso actual de un usuario, para autorizar cada request con el rol
-- vigente en la base y no con el que traía el token al emitirse: un cambio de
-- rol o una baja rigen en el request siguiente.
--
-- Se consulta mientras se autentica el token, antes de que haya un usuario en la
-- conexión: la ejecuta gn_app, que no puede leer la tabla. Igual que
-- buscar_para_login, corre con los permisos del owner (SECURITY DEFINER), con
-- search_path vacío y devuelve un único dato: el nivel, o NULL si el usuario no
-- existe.
CREATE FUNCTION usuarios.nivel_acceso_vigente(usuario uuid) RETURNS text
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = ''
AS $$
    SELECT nivel_acceso FROM usuarios.usuarios WHERE public_id = usuario
$$;

REVOKE ALL ON FUNCTION usuarios.nivel_acceso_vigente(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION usuarios.nivel_acceso_vigente(uuid) TO gn_app;
