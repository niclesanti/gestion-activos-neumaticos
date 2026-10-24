-- Módulo common (alta de domicilio, HU-24): provincias, localidades y domicilios
-- compartidos por los módulos de dominio, con privilegios mínimos y RLS (ver
-- docs/seguridad.md 2.11).
--
-- Los otros módulos guardan solo el id del domicilio (referencia formal, sin FK
-- entre schemas de módulos) y lo crean o leen por la fachada
-- common.api.DomicilioApi, dentro de su propia transacción.
--
-- En las tres tablas: administrador y editor leen, dan de alta y modifican; el
-- lector solo lee. Nadie borra (no hay borrado físico).
CREATE SCHEMA IF NOT EXISTS common;
GRANT USAGE ON SCHEMA common TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Provincias: las 24 jurisdicciones, cargadas por esta migración.
-- ---------------------------------------------------------------------------
-- Los ids los da el DEFAULT (Provincia usa GenerationType.IDENTITY).
CREATE SEQUENCE common.provincias_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE common.provincias (
    id     BIGINT      NOT NULL DEFAULT nextval('common.provincias_id_seq'),
    nombre VARCHAR(60) NOT NULL,

    CONSTRAINT pk_provincias PRIMARY KEY (id),
    CONSTRAINT uk_provincias_nombre UNIQUE (nombre),
    CONSTRAINT ck_provincias_nombre_formato CHECK (nombre = btrim(nombre) AND nombre <> '')
);

INSERT INTO common.provincias (nombre)
VALUES ('Buenos Aires'), ('Catamarca'), ('Chaco'), ('Chubut'), ('Ciudad Autónoma de Buenos Aires'),
       ('Córdoba'), ('Corrientes'), ('Entre Ríos'), ('Formosa'), ('Jujuy'), ('La Pampa'), ('La Rioja'),
       ('Mendoza'), ('Misiones'), ('Neuquén'), ('Río Negro'), ('Salta'), ('San Juan'), ('San Luis'),
       ('Santa Cruz'), ('Santa Fe'), ('Santiago del Estero'), ('Tierra del Fuego, Antártida e Islas del Atlántico Sur'),
       ('Tucumán');

ALTER TABLE common.provincias ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON common.provincias TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT (nombre), UPDATE (nombre) ON common.provincias TO gn_administrador, gn_editor;
GRANT USAGE ON SEQUENCE common.provincias_id_seq TO gn_administrador, gn_editor;

CREATE POLICY provincias_requiere_autenticacion ON common.provincias
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY provincias_select ON common.provincias
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY provincias_insert ON common.provincias
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY provincias_update ON common.provincias
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);

-- ---------------------------------------------------------------------------
-- Localidades: se crean al registrar un domicilio con una localidad nueva
-- (buscar o crear por provincia + nombre, sin distinguir mayúsculas).
-- ---------------------------------------------------------------------------
-- Los ids los da el DEFAULT: la app las inserta con un upsert nativo
-- (INSERT ... ON CONFLICT), nunca con un id generado por Hibernate.
CREATE SEQUENCE common.localidades_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE common.localidades (
    id            BIGINT       NOT NULL DEFAULT nextval('common.localidades_id_seq'),
    nombre        VARCHAR(100) NOT NULL,
    -- Opcional: 4 dígitos o CPA (letra de provincia + 4 dígitos + 3 letras).
    codigo_postal VARCHAR(8),
    provincia_id  BIGINT       NOT NULL,

    CONSTRAINT pk_localidades PRIMARY KEY (id),
    CONSTRAINT fk_localidades_provincia FOREIGN KEY (provincia_id) REFERENCES common.provincias (id),
    -- La app recorta los extremos y deja un solo espacio entre palabras.
    CONSTRAINT ck_localidades_nombre_formato
        CHECK (nombre = btrim(regexp_replace(nombre, '\s+', ' ', 'g')) AND nombre <> ''),
    CONSTRAINT ck_localidades_codigo_postal_formato
        CHECK (codigo_postal ~ '^([0-9]{4}|[A-Z][0-9]{4}[A-Z]{3})$')
);

-- Una localidad por nombre dentro de cada provincia: es el blanco del ON CONFLICT.
CREATE UNIQUE INDEX uk_localidades_provincia_nombre ON common.localidades (provincia_id, lower(nombre));

ALTER TABLE common.localidades ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON common.localidades TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT (nombre, codigo_postal, provincia_id), UPDATE (nombre, codigo_postal)
    ON common.localidades TO gn_administrador, gn_editor;
GRANT USAGE ON SEQUENCE common.localidades_id_seq TO gn_administrador, gn_editor;

CREATE POLICY localidades_requiere_autenticacion ON common.localidades
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY localidades_select ON common.localidades
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY localidades_insert ON common.localidades
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY localidades_update ON common.localidades
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);

-- ---------------------------------------------------------------------------
-- Domicilios: cada uno pertenece a una sola entidad de otro módulo (composición
-- 1-1, la unicidad la garantiza esa entidad). Sin public_id: no sale por la API.
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en Domicilio.
CREATE SEQUENCE common.domicilios_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE common.domicilios (
    id           BIGINT       NOT NULL DEFAULT nextval('common.domicilios_id_seq'),
    calle        VARCHAR(100) NOT NULL,
    altura       VARCHAR(10)  NOT NULL,
    piso         VARCHAR(10),
    departamento VARCHAR(10),
    localidad_id BIGINT       NOT NULL,

    CONSTRAINT pk_domicilios PRIMARY KEY (id),
    CONSTRAINT fk_domicilios_localidad FOREIGN KEY (localidad_id) REFERENCES common.localidades (id),
    -- La app recorta los extremos; los opcionales vacíos se guardan como NULL.
    CONSTRAINT ck_domicilios_calle_formato CHECK (calle = btrim(calle) AND calle <> ''),
    CONSTRAINT ck_domicilios_altura_formato CHECK (altura = btrim(altura) AND altura <> ''),
    CONSTRAINT ck_domicilios_piso_formato CHECK (piso = btrim(piso) AND piso <> ''),
    CONSTRAINT ck_domicilios_departamento_formato CHECK (departamento = btrim(departamento) AND departamento <> '')
);

ALTER TABLE common.domicilios ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON common.domicilios TO gn_administrador, gn_editor, gn_lector;
-- INSERT de todas las columnas: Hibernate envía el id que reservó con nextval.
GRANT INSERT ON common.domicilios TO gn_administrador, gn_editor;
GRANT UPDATE (calle, altura, piso, departamento, localidad_id) ON common.domicilios TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE common.domicilios_id_seq TO gn_administrador, gn_editor;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE common.domicilios_id_seq TO gn_app;

CREATE POLICY domicilios_requiere_autenticacion ON common.domicilios
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY domicilios_select ON common.domicilios
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY domicilios_insert ON common.domicilios
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY domicilios_update ON common.domicilios
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);
