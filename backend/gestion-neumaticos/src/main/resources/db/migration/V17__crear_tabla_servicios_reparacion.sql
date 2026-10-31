-- Módulo reparacion (HU-18): servicios de reparación / gomerías externas con
-- privilegios mínimos y RLS (ver docs/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS reparacion;
GRANT USAGE ON SCHEMA reparacion TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Servicios de reparación: administrador y editor leen, dan de alta y modifican;
-- el lector solo lee. Nadie borra (no hay borrado físico).
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en ServicioReparacion.
CREATE SEQUENCE reparacion.servicios_reparacion_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE reparacion.servicios_reparacion (
    id                                BIGINT       NOT NULL DEFAULT nextval('reparacion.servicios_reparacion_id_seq'),
    public_id                         UUID         NOT NULL,
    nombre                            VARCHAR(150) NOT NULL,
    -- Contacto opcional.
    email                             VARCHAR(254),
    telefono                          VARCHAR(30),
    -- Neumáticos derivados hoy a este servicio: lo incrementa el envío a
    -- reparación y lo decrementa el reingreso; nunca queda negativo.
    cantidad_neumaticos_en_reparacion INTEGER      NOT NULL DEFAULT 0,
    -- Referencia formal a common.domicilios: sin FK entre schemas de módulos. El
    -- domicilio se crea en la misma transacción que el servicio (fachada
    -- common.api.DomicilioApi) y nadie lo borra. Opcional; UNIQUE porque es
    -- una composición 1-1 (admite varios NULL).
    domicilio_id                      BIGINT,

    CONSTRAINT pk_servicios_reparacion PRIMARY KEY (id),
    CONSTRAINT uk_servicios_reparacion_public_id UNIQUE (public_id),
    CONSTRAINT uk_servicios_reparacion_domicilio UNIQUE (domicilio_id),
    -- La app recorta los extremos: un nombre en blanco no se guarda.
    CONSTRAINT ck_servicios_reparacion_nombre_formato CHECK (nombre = btrim(nombre) AND nombre <> ''),
    -- La app lo guarda recortado y en minúsculas.
    CONSTRAINT ck_servicios_reparacion_email_formato CHECK (email = lower(btrim(email)) AND email LIKE '_%@_%'),
    CONSTRAINT ck_servicios_reparacion_telefono_formato CHECK (telefono ~ '^\+?[0-9(][0-9 ()-]{4,27}[0-9]$'),
    CONSTRAINT ck_servicios_reparacion_cantidad CHECK (cantidad_neumaticos_en_reparacion >= 0)
);

-- El nombre no se repite, sin distinguir mayúsculas.
CREATE UNIQUE INDEX uk_servicios_reparacion_nombre ON reparacion.servicios_reparacion (lower(nombre));

ALTER TABLE reparacion.servicios_reparacion ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON reparacion.servicios_reparacion TO gn_administrador, gn_editor, gn_lector;
-- INSERT de todas las columnas: Hibernate envía el id que reservó con nextval.
GRANT INSERT ON reparacion.servicios_reparacion TO gn_administrador, gn_editor;
-- Los identificadores (id, public_id) no se modifican.
GRANT UPDATE (nombre, email, telefono, cantidad_neumaticos_en_reparacion, domicilio_id)
    ON reparacion.servicios_reparacion TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE reparacion.servicios_reparacion_id_seq TO gn_administrador, gn_editor;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE reparacion.servicios_reparacion_id_seq TO gn_app;

CREATE POLICY servicios_reparacion_requiere_autenticacion ON reparacion.servicios_reparacion
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY servicios_reparacion_select ON reparacion.servicios_reparacion
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY servicios_reparacion_insert ON reparacion.servicios_reparacion
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY servicios_reparacion_update ON reparacion.servicios_reparacion
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);
