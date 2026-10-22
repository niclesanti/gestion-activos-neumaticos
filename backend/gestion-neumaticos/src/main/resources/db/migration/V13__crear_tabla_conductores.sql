-- Módulo conductores (alta de conductor): conductores con privilegios mínimos
-- y RLS (ver docs/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS conductores;
GRANT USAGE ON SCHEMA conductores TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Conductores: administrador y editor leen y dan de alta; el lector solo lee.
-- Todavía no hay modificación ni borrado.
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en Conductor.
CREATE SEQUENCE conductores.conductores_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE conductores.conductores (
    id        BIGINT       NOT NULL DEFAULT nextval('conductores.conductores_id_seq'),
    public_id UUID         NOT NULL,
    -- Opcional: UNIQUE admite varios NULL.
    dni       VARCHAR(8),
    nombre    VARCHAR(100) NOT NULL,
    apellido  VARCHAR(100) NOT NULL,

    CONSTRAINT pk_conductores PRIMARY KEY (id),
    CONSTRAINT uk_conductores_public_id UNIQUE (public_id),
    CONSTRAINT uk_conductores_dni UNIQUE (dni),
    -- La app lo normaliza (recortado, vacío = sin DNI): solo 7 u 8 dígitos, sin puntos.
    CONSTRAINT ck_conductores_dni_formato CHECK (dni ~ '^[0-9]{7,8}$'),
    -- La app recorta los extremos: un nombre o apellido en blanco no se guarda.
    CONSTRAINT ck_conductores_nombre_formato CHECK (nombre = btrim(nombre) AND nombre <> ''),
    CONSTRAINT ck_conductores_apellido_formato CHECK (apellido = btrim(apellido) AND apellido <> '')
);

ALTER TABLE conductores.conductores ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON conductores.conductores TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT ON conductores.conductores TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE conductores.conductores_id_seq TO gn_administrador, gn_editor;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE conductores.conductores_id_seq TO gn_app;

CREATE POLICY conductores_requiere_autenticacion ON conductores.conductores
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY conductores_select ON conductores.conductores
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY conductores_insert ON conductores.conductores
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);
