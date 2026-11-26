-- HU-31: asignar un neumático a una posición (slot) de una unidad de transporte.
-- El montaje cambia el slot (neumatico_actual_id), firma la modificación de la
-- unidad (updated_*), mueve el neumático y descuenta el stock del centro; los
-- dos últimos ya tienen UPDATE (V10, V18, V19). Ver docs/Seguridad/seguridad.md 2.11.

-- ---------------------------------------------------------------------------
-- Slots: administrador y editor cambian solo el neumático montado. El resto de
-- la posición es inmutable (sin GRANT) y nadie borra.
-- ---------------------------------------------------------------------------
GRANT UPDATE (neumatico_actual_id) ON unidades_transporte.slots_unidad_transporte TO gn_administrador, gn_editor;

CREATE POLICY slots_unidad_transporte_update ON unidades_transporte.slots_unidad_transporte
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);

-- ---------------------------------------------------------------------------
-- Unidades: el montaje cambia un hijo del agregado, así que toca la raíz para
-- que quede firmada (HU-43). Solo updated_*: patente, configuración y baja no
-- se modifican todavía; created_* nunca.
-- ---------------------------------------------------------------------------
GRANT UPDATE (updated_at, updated_by) ON unidades_transporte.unidades_transporte TO gn_administrador, gn_editor;

CREATE POLICY unidades_transporte_update ON unidades_transporte.unidades_transporte
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (updated_by = (SELECT seguridad.usuario_actual())
                AND (deleted_by IS NULL OR deleted_by = (SELECT seguridad.usuario_actual())));

-- ---------------------------------------------------------------------------
-- Un neumático está en una sola posición y una posición tiene un solo
-- neumático, aunque dos montajes concurrentes pasen la validación de la app.
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX uk_slots_unidad_transporte_neumatico
    ON unidades_transporte.slots_unidad_transporte (neumatico_actual_id)
    WHERE neumatico_actual_id IS NOT NULL;

CREATE UNIQUE INDEX uk_neumaticos_slot_montado
    ON neumaticos.neumaticos (slot_montado_id)
    WHERE slot_montado_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Trazabilidad: datos de los eventos de asignación y desasignación (id +
-- snapshot de la unidad, la posición y, opcional, el conductor). Solo esos dos
-- tipos los llevan, y siempre con unidad y posición.
-- ---------------------------------------------------------------------------
ALTER TABLE trazabilidad.eventos_neumatico
    ADD COLUMN unidad_id        BIGINT,
    ADD COLUMN patente_unidad   VARCHAR(10),
    ADD COLUMN slot_id          BIGINT,
    ADD COLUMN posicion_slot    VARCHAR(20),
    ADD COLUMN conductor_id     BIGINT,
    ADD COLUMN nombre_conductor VARCHAR(201);

ALTER TABLE trazabilidad.eventos_neumatico
    ADD CONSTRAINT ck_eventos_neumatico_datos_montaje CHECK (
        (tipo IN ('ASIGNACION', 'DESASIGNACION')
            AND unidad_id IS NOT NULL AND patente_unidad IS NOT NULL
            AND slot_id IS NOT NULL AND posicion_slot IS NOT NULL)
        OR (tipo NOT IN ('ASIGNACION', 'DESASIGNACION')
            AND unidad_id IS NULL AND patente_unidad IS NULL AND slot_id IS NULL AND posicion_slot IS NULL
            AND conductor_id IS NULL AND nombre_conductor IS NULL)),
    ADD CONSTRAINT ck_eventos_neumatico_conductor_coherente CHECK (
        (conductor_id IS NULL) = (nombre_conductor IS NULL));
