-- Módulo neumaticos (HU-11): marcas y neumáticos, con privilegios mínimos y RLS
-- (ver docs/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS neumaticos;
GRANT USAGE ON SCHEMA neumaticos TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Marcas: catálogo que solo se carga por migraciones. Cualquier rol
-- autenticado lo lee; nadie lo modifica desde la app.
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en MarcaNeumatico.
CREATE SEQUENCE neumaticos.marcas_neumaticos_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE neumaticos.marcas_neumaticos (
    id     BIGINT       NOT NULL DEFAULT nextval('neumaticos.marcas_neumaticos_id_seq'),
    nombre VARCHAR(100) NOT NULL,

    CONSTRAINT pk_marcas_neumaticos PRIMARY KEY (id),
    CONSTRAINT uk_marcas_neumaticos_nombre UNIQUE (nombre)
);

-- Marcas de neumáticos de camión más difundidas en Argentina.
INSERT INTO neumaticos.marcas_neumaticos (nombre)
VALUES ('Bridgestone'), ('Fate'), ('Pirelli');

ALTER TABLE neumaticos.marcas_neumaticos ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON neumaticos.marcas_neumaticos TO gn_administrador, gn_editor, gn_lector;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE neumaticos.marcas_neumaticos_id_seq TO gn_app;

CREATE POLICY marcas_neumaticos_requiere_autenticacion ON neumaticos.marcas_neumaticos
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY marcas_neumaticos_select ON neumaticos.marcas_neumaticos
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

-- ---------------------------------------------------------------------------
-- Neumáticos: administrador y editor leen, dan de alta y modifican; el lector
-- solo lee. Nadie borra (no hay borrado físico).
-- ---------------------------------------------------------------------------
CREATE SEQUENCE neumaticos.neumaticos_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE neumaticos.neumaticos (
    id                            BIGINT       NOT NULL DEFAULT nextval('neumaticos.neumaticos_id_seq'),
    public_id                     UUID         NOT NULL,
    -- Opcional (marcas genéricas sin número de serie): UNIQUE admite varios NULL.
    numero_serie                  VARCHAR(100),
    estado_actual                 VARCHAR(30)  NOT NULL,
    en_espera                     BOOLEAN      NOT NULL DEFAULT true,
    kilometraje_total             REAL         NOT NULL DEFAULT 0,
    kilometraje_ultima_reparacion REAL         NOT NULL DEFAULT 0,
    tipo_origen                   VARCHAR(20)  NOT NULL,
    tipo_actual                   VARCHAR(20)  NOT NULL,
    cantidad_recapados            INTEGER      NOT NULL DEFAULT 0,
    situacion_operativa_vigente   VARCHAR(20)  NOT NULL,
    comentario                    VARCHAR(512),
    slot_montado_id               BIGINT,
    centro_almacenamiento_id      BIGINT,
    servicio_reparacion_id        BIGINT,
    marca_id                      BIGINT       NOT NULL,

    CONSTRAINT pk_neumaticos PRIMARY KEY (id),
    CONSTRAINT uk_neumaticos_public_id UNIQUE (public_id),
    CONSTRAINT uk_neumaticos_numero_serie UNIQUE (numero_serie),
    CONSTRAINT fk_neumaticos_marca FOREIGN KEY (marca_id) REFERENCES neumaticos.marcas_neumaticos (id),
    -- La app lo normaliza (recortado y en mayúsculas): así la unicidad no
    -- distingue mayúsculas y un número de serie vacío no ocupa el valor ''.
    CONSTRAINT ck_neumaticos_numero_serie_formato
        CHECK (numero_serie = upper(btrim(numero_serie)) AND numero_serie <> ''),
    CONSTRAINT ck_neumaticos_estado_actual CHECK (estado_actual IN
        ('NUEVO', 'NUEVO_USADO', 'HOMBROS', 'RECAPADO', 'RECAPADO_USADO', 'RECAPADO_SIN_GARANTIA', 'DE_BAJA')),
    CONSTRAINT ck_neumaticos_tipo_origen CHECK (tipo_origen IN
        ('DIRECCION', 'TRACCION', 'ARRASTRE', 'AUXILIO', 'SIN_ESPECIFICAR')),
    CONSTRAINT ck_neumaticos_tipo_actual CHECK (tipo_actual IN
        ('DIRECCION', 'TRACCION', 'ARRASTRE', 'AUXILIO', 'SIN_ESPECIFICAR')),
    CONSTRAINT ck_neumaticos_situacion_operativa CHECK (situacion_operativa_vigente IN
        ('EN_STOCK', 'MONTADO', 'EN_REPARACION', 'EN_ESPERA', 'DE_BAJA')),
    CONSTRAINT ck_neumaticos_kilometraje CHECK (kilometraje_total >= 0 AND kilometraje_ultima_reparacion >= 0),
    CONSTRAINT ck_neumaticos_cantidad_recapados CHECK (cantidad_recapados >= 0)
);

CREATE INDEX ix_neumaticos_marca_id ON neumaticos.neumaticos (marca_id);

ALTER TABLE neumaticos.neumaticos ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON neumaticos.neumaticos TO gn_administrador, gn_editor, gn_lector;
GRANT INSERT ON neumaticos.neumaticos TO gn_administrador, gn_editor;
-- Sin id, public_id ni numero_serie: el número de serie no se puede cambiar
-- una vez registrado (HU-11).
GRANT UPDATE (estado_actual, en_espera, kilometraje_total, kilometraje_ultima_reparacion, tipo_origen,
              tipo_actual, cantidad_recapados, situacion_operativa_vigente, comentario, slot_montado_id,
              centro_almacenamiento_id, servicio_reparacion_id, marca_id)
    ON neumaticos.neumaticos TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE neumaticos.neumaticos_id_seq TO gn_administrador, gn_editor;
GRANT SELECT ON SEQUENCE neumaticos.neumaticos_id_seq TO gn_app;

CREATE POLICY neumaticos_requiere_autenticacion ON neumaticos.neumaticos
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY neumaticos_select ON neumaticos.neumaticos
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY neumaticos_insert ON neumaticos.neumaticos
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY neumaticos_update ON neumaticos.neumaticos
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);
