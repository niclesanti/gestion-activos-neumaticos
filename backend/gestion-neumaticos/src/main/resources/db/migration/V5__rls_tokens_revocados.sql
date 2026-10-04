-- Permisos sobre seguridad.tokens_revocados.
--   gn_app (sin usuario): lee la lista, porque el chequeo de revocación es parte
--   de autenticar el token, y purga solo los tokens ya vencidos (tarea diaria).
--   Usuarios autenticados: registran el logout (Spring Data hace SELECT + INSERT).

ALTER TABLE seguridad.tokens_revocados ENABLE ROW LEVEL SECURITY;

GRANT SELECT, DELETE ON seguridad.tokens_revocados TO gn_app;
GRANT SELECT, INSERT ON seguridad.tokens_revocados TO gn_administrador, gn_editor, gn_lector;

CREATE POLICY tokens_revocados_requiere_autenticacion ON seguridad.tokens_revocados
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY tokens_revocados_select ON seguridad.tokens_revocados
    FOR SELECT
    TO gn_app, gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY tokens_revocados_insert ON seguridad.tokens_revocados
    FOR INSERT
    TO gn_administrador, gn_editor, gn_lector
    WITH CHECK (true);

-- La purga no puede borrar un token que todavía está vigente.
CREATE POLICY tokens_revocados_delete_vencidos ON seguridad.tokens_revocados
    FOR DELETE
    TO gn_app
    USING (expira_en < now());
