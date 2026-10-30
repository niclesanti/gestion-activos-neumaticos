-- Módulo unidades_transporte (HU-07): configuraciones de ejes precargadas,
-- unidades de transporte y sus slots, con privilegios mínimos y RLS (ver
-- docs/Seguridad/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS unidades_transporte;
GRANT USAGE ON SCHEMA unidades_transporte TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Configuraciones de unidad y sus ejes: catálogo que solo se carga por
-- migraciones (patrón Type Object). Cualquier rol autenticado lo lee; nadie lo
-- modifica desde la app.
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en ConfiguracionUnidad.
CREATE SEQUENCE unidades_transporte.configuraciones_unidad_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE unidades_transporte.configuraciones_unidad (
    id                 BIGINT       NOT NULL DEFAULT nextval('unidades_transporte.configuraciones_unidad_id_seq'),
    nombre             VARCHAR(100) NOT NULL,
    tipo_unidad        VARCHAR(20)  NOT NULL,
    cant_slots_auxilio INTEGER      NOT NULL,

    CONSTRAINT pk_configuraciones_unidad PRIMARY KEY (id),
    CONSTRAINT uk_configuraciones_unidad_nombre UNIQUE (nombre),
    CONSTRAINT ck_configuraciones_unidad_tipo_unidad CHECK (tipo_unidad IN ('TRACTOR', 'SEMIRREMOLQUE')),
    CONSTRAINT ck_configuraciones_unidad_cant_slots_auxilio CHECK (cant_slots_auxilio >= 0)
);

-- INCREMENT BY = allocationSize de @SequenceGenerator en EjeConfiguracion.
CREATE SEQUENCE unidades_transporte.ejes_configuracion_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE unidades_transporte.ejes_configuracion (
    id               BIGINT      NOT NULL DEFAULT nextval('unidades_transporte.ejes_configuracion_id_seq'),
    configuracion_id BIGINT      NOT NULL,
    -- Posición del eje de adelante hacia atrás: es el número de eje de los slots.
    orden            INTEGER     NOT NULL,
    tipo_eje         VARCHAR(20) NOT NULL,
    rodado           VARCHAR(10) NOT NULL,

    CONSTRAINT pk_ejes_configuracion PRIMARY KEY (id),
    CONSTRAINT fk_ejes_configuracion_configuracion FOREIGN KEY (configuracion_id)
        REFERENCES unidades_transporte.configuraciones_unidad (id),
    -- También sirve de índice de la FK (columna líder).
    CONSTRAINT uk_ejes_configuracion_orden UNIQUE (configuracion_id, orden),
    CONSTRAINT ck_ejes_configuracion_orden CHECK (orden >= 1),
    CONSTRAINT ck_ejes_configuracion_tipo_eje CHECK (tipo_eje IN ('DIRECCION', 'TRACCION', 'ARRASTRE')),
    CONSTRAINT ck_ejes_configuracion_rodado CHECK (rodado IN ('SIMPLE', 'DUAL'))
);

-- Las tres configuraciones de la empresa (esquema de ejes y neumáticos del
-- cliente). Cada una con un neumático de auxilio.
INSERT INTO unidades_transporte.configuraciones_unidad (nombre, tipo_unidad, cant_slots_auxilio)
VALUES ('Tractor común', 'TRACTOR', 1),
       ('Tractor para cargas pesadas', 'TRACTOR', 1),
       ('Semirremolque', 'SEMIRREMOLQUE', 1);

INSERT INTO unidades_transporte.ejes_configuracion (configuracion_id, orden, tipo_eje, rodado)
SELECT c.id, e.orden, e.tipo_eje, e.rodado
FROM unidades_transporte.configuraciones_unidad c
JOIN (VALUES ('Tractor común', 1, 'DIRECCION', 'SIMPLE'),
             ('Tractor común', 2, 'TRACCION', 'DUAL'),
             ('Tractor para cargas pesadas', 1, 'DIRECCION', 'SIMPLE'),
             ('Tractor para cargas pesadas', 2, 'TRACCION', 'DUAL'),
             ('Tractor para cargas pesadas', 3, 'TRACCION', 'DUAL'),
             ('Semirremolque', 1, 'ARRASTRE', 'DUAL'),
             ('Semirremolque', 2, 'ARRASTRE', 'DUAL'),
             ('Semirremolque', 3, 'ARRASTRE', 'DUAL'))
    AS e (configuracion, orden, tipo_eje, rodado) ON e.configuracion = c.nombre;

ALTER TABLE unidades_transporte.configuraciones_unidad ENABLE ROW LEVEL SECURITY;
ALTER TABLE unidades_transporte.ejes_configuracion ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON unidades_transporte.configuraciones_unidad, unidades_transporte.ejes_configuracion
    TO gn_administrador, gn_editor, gn_lector;
-- Solo para la validación de esquema de Hibernate (ver V4): no generan ids.
GRANT SELECT ON SEQUENCE unidades_transporte.configuraciones_unidad_id_seq,
                         unidades_transporte.ejes_configuracion_id_seq TO gn_app;

CREATE POLICY configuraciones_unidad_requiere_autenticacion ON unidades_transporte.configuraciones_unidad
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY configuraciones_unidad_select ON unidades_transporte.configuraciones_unidad
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY ejes_configuracion_requiere_autenticacion ON unidades_transporte.ejes_configuracion
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY ejes_configuracion_select ON unidades_transporte.ejes_configuracion
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

-- ---------------------------------------------------------------------------
-- Unidades de transporte: administrador y editor leen y dan de alta; el lector
-- solo lee. Todavía ninguna historia modifica ni borra una unidad: sin UPDATE
-- ni DELETE para nadie.
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en UnidadTransporte.
CREATE SEQUENCE unidades_transporte.unidades_transporte_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE unidades_transporte.unidades_transporte (
    id               BIGINT      NOT NULL DEFAULT nextval('unidades_transporte.unidades_transporte_id_seq'),
    public_id        UUID        NOT NULL,
    patente          VARCHAR(10) NOT NULL,
    configuracion_id BIGINT      NOT NULL,

    CONSTRAINT pk_unidades_transporte PRIMARY KEY (id),
    CONSTRAINT uk_unidades_transporte_public_id UNIQUE (public_id),
    CONSTRAINT uk_unidades_transporte_patente UNIQUE (patente),
    CONSTRAINT fk_unidades_transporte_configuracion FOREIGN KEY (configuracion_id)
        REFERENCES unidades_transporte.configuraciones_unidad (id),
    -- La app la normaliza (mayúsculas, sin espacios ni guiones): formato
    -- anterior ABC123 o Mercosur AB123CD. Así la unicidad no depende de cómo
    -- se escribió.
    CONSTRAINT ck_unidades_transporte_patente_formato
        CHECK (patente ~ '^([A-Z]{3}[0-9]{3}|[A-Z]{2}[0-9]{3}[A-Z]{2})$')
);

CREATE INDEX ix_unidades_transporte_configuracion_id ON unidades_transporte.unidades_transporte (configuracion_id);

ALTER TABLE unidades_transporte.unidades_transporte ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON unidades_transporte.unidades_transporte TO gn_administrador, gn_editor, gn_lector;
-- INSERT de todas las columnas: Hibernate envía el id que reservó con nextval.
GRANT INSERT ON unidades_transporte.unidades_transporte TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE unidades_transporte.unidades_transporte_id_seq TO gn_administrador, gn_editor;
GRANT SELECT ON SEQUENCE unidades_transporte.unidades_transporte_id_seq TO gn_app;

CREATE POLICY unidades_transporte_requiere_autenticacion ON unidades_transporte.unidades_transporte
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY unidades_transporte_select ON unidades_transporte.unidades_transporte
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY unidades_transporte_insert ON unidades_transporte.unidades_transporte
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

-- ---------------------------------------------------------------------------
-- Slots: las posiciones de neumático de cada unidad, generadas en el alta a
-- partir de su configuración. Administrador y editor leen y dan de alta; el
-- lector solo lee. El montaje de neumáticos agregará UPDATE (neumatico_actual_id).
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en SlotUnidadTransporte.
CREATE SEQUENCE unidades_transporte.slots_unidad_transporte_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE unidades_transporte.slots_unidad_transporte (
    id                  BIGINT      NOT NULL DEFAULT nextval('unidades_transporte.slots_unidad_transporte_id_seq'),
    unidad_id           BIGINT      NOT NULL,
    tipo_slot           VARCHAR(10) NOT NULL,
    -- Slot de eje: nro_eje, tipo_eje, lado y posicion_rueda; slot de auxilio:
    -- solo nro_auxilio (ver ck_slots_unidad_transporte_consistencia).
    nro_eje             INTEGER,
    tipo_eje            VARCHAR(20),
    lado                VARCHAR(10),
    posicion_rueda      VARCHAR(10),
    nro_auxilio         INTEGER,
    -- Referencia formal a neumaticos.neumaticos: sin FK entre schemas de módulos.
    neumatico_actual_id BIGINT,

    CONSTRAINT pk_slots_unidad_transporte PRIMARY KEY (id),
    CONSTRAINT fk_slots_unidad_transporte_unidad FOREIGN KEY (unidad_id)
        REFERENCES unidades_transporte.unidades_transporte (id),
    -- Ambas sirven de índice de la FK (columna líder); los NULL no chocan entre sí.
    CONSTRAINT uk_slots_unidad_transporte_posicion UNIQUE (unidad_id, nro_eje, lado, posicion_rueda),
    CONSTRAINT uk_slots_unidad_transporte_auxilio UNIQUE (unidad_id, nro_auxilio),
    CONSTRAINT ck_slots_unidad_transporte_tipo_slot CHECK (tipo_slot IN ('EJE', 'AUXILIO')),
    CONSTRAINT ck_slots_unidad_transporte_tipo_eje CHECK (tipo_eje IN ('DIRECCION', 'TRACCION', 'ARRASTRE')),
    CONSTRAINT ck_slots_unidad_transporte_lado CHECK (lado IN ('IZQUIERDO', 'DERECHO')),
    CONSTRAINT ck_slots_unidad_transporte_posicion_rueda CHECK (posicion_rueda IN ('SIMPLE', 'EXTERIOR', 'INTERIOR')),
    CONSTRAINT ck_slots_unidad_transporte_numeros CHECK (nro_eje >= 1 AND nro_auxilio >= 1),
    CONSTRAINT ck_slots_unidad_transporte_consistencia CHECK (
        (tipo_slot = 'EJE'
            AND nro_eje IS NOT NULL AND tipo_eje IS NOT NULL AND lado IS NOT NULL AND posicion_rueda IS NOT NULL
            AND nro_auxilio IS NULL)
        OR (tipo_slot = 'AUXILIO'
            AND nro_eje IS NULL AND tipo_eje IS NULL AND lado IS NULL AND posicion_rueda IS NULL
            AND nro_auxilio IS NOT NULL))
);

ALTER TABLE unidades_transporte.slots_unidad_transporte ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON unidades_transporte.slots_unidad_transporte TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT ON unidades_transporte.slots_unidad_transporte TO gn_administrador, gn_editor;
GRANT USAGE ON SEQUENCE unidades_transporte.slots_unidad_transporte_id_seq TO gn_administrador, gn_editor;
GRANT SELECT ON SEQUENCE unidades_transporte.slots_unidad_transporte_id_seq TO gn_app;

CREATE POLICY slots_unidad_transporte_requiere_autenticacion ON unidades_transporte.slots_unidad_transporte
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY slots_unidad_transporte_select ON unidades_transporte.slots_unidad_transporte
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY slots_unidad_transporte_insert ON unidades_transporte.slots_unidad_transporte
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);
