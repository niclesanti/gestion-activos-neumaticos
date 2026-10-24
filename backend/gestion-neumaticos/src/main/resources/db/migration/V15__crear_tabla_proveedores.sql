-- Módulo proveedores (HU-24): proveedores de neumáticos con privilegios mínimos
-- y RLS (ver docs/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS proveedores;
GRANT USAGE ON SCHEMA proveedores TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Proveedores: administrador y editor leen, dan de alta y modifican; el lector
-- solo lee. Nadie borra (no hay borrado físico).
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en ProveedorNeumaticos.
CREATE SEQUENCE proveedores.proveedores_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE proveedores.proveedores (
    id           BIGINT       NOT NULL DEFAULT nextval('proveedores.proveedores_id_seq'),
    public_id    UUID         NOT NULL,
    nombre       VARCHAR(150) NOT NULL,
    -- Contacto opcional.
    email        VARCHAR(254),
    telefono     VARCHAR(30),
    -- Referencia formal a common.domicilios: sin FK entre schemas de módulos. El
    -- domicilio se crea en la misma transacción que el proveedor (fachada
    -- common.api.DomicilioApi) y nadie lo borra. Opcional; UNIQUE porque es
    -- una composición 1-1 (admite varios NULL).
    domicilio_id BIGINT,

    CONSTRAINT pk_proveedores PRIMARY KEY (id),
    CONSTRAINT uk_proveedores_public_id UNIQUE (public_id),
    CONSTRAINT uk_proveedores_domicilio UNIQUE (domicilio_id),
    -- La app recorta los extremos: un nombre en blanco no se guarda.
    CONSTRAINT ck_proveedores_nombre_formato CHECK (nombre = btrim(nombre) AND nombre <> ''),
    -- La app lo guarda recortado y en minúsculas.
    CONSTRAINT ck_proveedores_email_formato CHECK (email = lower(btrim(email)) AND email LIKE '_%@_%'),
    CONSTRAINT ck_proveedores_telefono_formato CHECK (telefono ~ '^\+?[0-9(][0-9 ()-]{4,27}[0-9]$')
);

-- El nombre no se repite, sin distinguir mayúsculas.
CREATE UNIQUE INDEX uk_proveedores_nombre ON proveedores.proveedores (lower(nombre));

ALTER TABLE proveedores.proveedores ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON proveedores.proveedores TO gn_administrador, gn_editor, gn_lector;
-- INSERT de todas las columnas: Hibernate envía el id que reservó con nextval.
GRANT INSERT ON proveedores.proveedores TO gn_administrador, gn_editor;
-- Los identificadores (id, public_id) no se modifican.
GRANT UPDATE (nombre, email, telefono, domicilio_id) ON proveedores.proveedores TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE proveedores.proveedores_id_seq TO gn_administrador, gn_editor;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE proveedores.proveedores_id_seq TO gn_app;

CREATE POLICY proveedores_requiere_autenticacion ON proveedores.proveedores
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY proveedores_select ON proveedores.proveedores
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY proveedores_insert ON proveedores.proveedores
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY proveedores_update ON proveedores.proveedores
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);
