-- Un usuario autenticado solo inserta en la lista de revocación el jti de su
-- propio token al cerrar sesión, y ese token vence en horas. Acotar la
-- expiración evita que, con una inyección SQL, se infle la tabla con filas que
-- la purga diaria nunca borraría.
DROP POLICY tokens_revocados_insert ON seguridad.tokens_revocados;

CREATE POLICY tokens_revocados_insert ON seguridad.tokens_revocados
    FOR INSERT
    TO gn_administrador, gn_editor, gn_lector
    WITH CHECK (expira_en <= now() + interval '1 day');
