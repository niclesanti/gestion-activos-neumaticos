-- HU-43: auditoría automática. Las raíces de agregado de negocio registran quién
-- y cuándo las creó, las modificó por última vez y las dio de baja (lógica).
-- Las columnas las completa Spring Data JPA Auditing (common.auditoria.AuditableEntity);
-- acá la base de datos las vuelve inmutables o infalsificables (ver docs/seguridad.md 2.11).

-- ---------------------------------------------------------------------------
-- Columnas, coherencia e índice de orden por defecto, en las 6 tablas auditadas.
-- DEFAULT now() en created_at/updated_at: Hibernate siempre envía el valor, pero
-- las cargas del owner (seed, tests) no pasan por la aplicación. created_by y
-- updated_by quedan en NULL para esas filas ("anterior a la auditoría").
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    tabla text;
    nombre text;
BEGIN
    FOREACH tabla IN ARRAY ARRAY[
        'usuarios.usuarios',
        'neumaticos.neumaticos',
        'conductores.conductores',
        'proveedores.proveedores',
        'reparacion.servicios_reparacion',
        'unidades_transporte.unidades_transporte'
    ] LOOP
        nombre := split_part(tabla, '.', 2);
        EXECUTE format(
            'ALTER TABLE %s
                 ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                 ADD COLUMN created_by UUID,
                 ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                 ADD COLUMN updated_by UUID,
                 ADD COLUMN deleted_at TIMESTAMPTZ,
                 ADD COLUMN deleted_by UUID',
            tabla);
        -- La baja se registra completa o no se registra.
        EXECUTE format(
            'ALTER TABLE %s ADD CONSTRAINT ck_%s_baja_coherente CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))',
            tabla, nombre);
        EXECUTE format('CREATE INDEX ix_%s_updated_at ON %s (updated_at DESC)', nombre, tabla);
    END LOOP;
END
$$;

-- ---------------------------------------------------------------------------
-- Privilegios: created_* no entra en ningún GRANT UPDATE, así que nadie puede
-- reescribir quién creó un registro. Conductores y unidades de transporte todavía
-- no se modifican (sin UPDATE): lo recibirán con su historia de modificación/baja.
-- ---------------------------------------------------------------------------
GRANT UPDATE (updated_at, updated_by, deleted_at, deleted_by) ON usuarios.usuarios TO gn_administrador;
GRANT UPDATE (updated_at, updated_by, deleted_at, deleted_by) ON neumaticos.neumaticos TO gn_administrador, gn_editor;
GRANT UPDATE (updated_at, updated_by, deleted_at, deleted_by) ON proveedores.proveedores TO gn_administrador, gn_editor;
GRANT UPDATE (updated_at, updated_by, deleted_at, deleted_by) ON reparacion.servicios_reparacion TO gn_administrador, gn_editor;

-- ---------------------------------------------------------------------------
-- RLS anti-suplantación: la firma de la operación tiene que ser del usuario de la
-- sesión (seguridad.usuario_actual()), aunque la aplicación tuviera un bug.
-- ---------------------------------------------------------------------------

-- INSERT: created_by y updated_by son el usuario de la sesión.
DROP POLICY usuarios_insert_admin ON usuarios.usuarios;
CREATE POLICY usuarios_insert_admin ON usuarios.usuarios
    FOR INSERT
    TO gn_administrador
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

DROP POLICY neumaticos_insert ON neumaticos.neumaticos;
CREATE POLICY neumaticos_insert ON neumaticos.neumaticos
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

DROP POLICY conductores_insert ON conductores.conductores;
CREATE POLICY conductores_insert ON conductores.conductores
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

DROP POLICY proveedores_insert ON proveedores.proveedores;
CREATE POLICY proveedores_insert ON proveedores.proveedores
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

DROP POLICY servicios_reparacion_insert ON reparacion.servicios_reparacion;
CREATE POLICY servicios_reparacion_insert ON reparacion.servicios_reparacion
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

DROP POLICY unidades_transporte_insert ON unidades_transporte.unidades_transporte;
CREATE POLICY unidades_transporte_insert ON unidades_transporte.unidades_transporte
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (created_by = (SELECT seguridad.usuario_actual()) AND updated_by = created_by);

-- UPDATE: quien modifica firma como updated_by; si da de baja, firma también deleted_by.
DROP POLICY usuarios_update_admin ON usuarios.usuarios;
CREATE POLICY usuarios_update_admin ON usuarios.usuarios
    FOR UPDATE
    TO gn_administrador
    USING (true)
    WITH CHECK (updated_by = (SELECT seguridad.usuario_actual())
                AND (deleted_by IS NULL OR deleted_by = (SELECT seguridad.usuario_actual())));

DROP POLICY neumaticos_update ON neumaticos.neumaticos;
CREATE POLICY neumaticos_update ON neumaticos.neumaticos
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (updated_by = (SELECT seguridad.usuario_actual())
                AND (deleted_by IS NULL OR deleted_by = (SELECT seguridad.usuario_actual())));

DROP POLICY proveedores_update ON proveedores.proveedores;
CREATE POLICY proveedores_update ON proveedores.proveedores
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (updated_by = (SELECT seguridad.usuario_actual())
                AND (deleted_by IS NULL OR deleted_by = (SELECT seguridad.usuario_actual())));

DROP POLICY servicios_reparacion_update ON reparacion.servicios_reparacion;
CREATE POLICY servicios_reparacion_update ON reparacion.servicios_reparacion
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (updated_by = (SELECT seguridad.usuario_actual())
                AND (deleted_by IS NULL OR deleted_by = (SELECT seguridad.usuario_actual())));

-- ---------------------------------------------------------------------------
-- Consulta de auditoría (solo administrador): el endpoint muestra el nombre de
-- quien firmó cada registro. La RLS de usuarios ya deja al administrador leer
-- todas las filas, así que no hace falta ninguna función SECURITY DEFINER.
-- ---------------------------------------------------------------------------
