-- Módulo almacenamiento: centros de almacenamiento (depósitos) de neumáticos
-- con privilegios mínimos y RLS (ver docs/seguridad.md 2.11).
CREATE SCHEMA IF NOT EXISTS almacenamiento;
GRANT USAGE ON SCHEMA almacenamiento TO gn_app, gn_administrador, gn_editor, gn_lector;

-- ---------------------------------------------------------------------------
-- Centros de almacenamiento: administrador y editor leen, dan de alta y
-- modifican; el lector solo lee. Nadie borra (no hay borrado físico).
-- ---------------------------------------------------------------------------
-- INCREMENT BY = allocationSize de @SequenceGenerator en CentroAlmacenamiento.
CREATE SEQUENCE almacenamiento.centros_almacenamiento_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE almacenamiento.centros_almacenamiento (
    id                           BIGINT       NOT NULL DEFAULT nextval('almacenamiento.centros_almacenamiento_id_seq'),
    public_id                    UUID         NOT NULL,
    nombre                       VARCHAR(150) NOT NULL,
    -- Neumáticos guardados hoy en el centro: lo incrementa el ingreso al stock
    -- y lo decrementa la salida; nunca queda negativo.
    cantidad_neumaticos_en_stock INTEGER      NOT NULL DEFAULT 0,
    -- Referencia formal a common.domicilios: sin FK entre schemas de módulos. El
    -- domicilio se crea en la misma transacción que el centro (fachada
    -- common.api.DomicilioApi) y nadie lo borra. Obligatorio; UNIQUE porque es
    -- una composición 1-1.
    domicilio_id                 BIGINT       NOT NULL,

    CONSTRAINT pk_centros_almacenamiento PRIMARY KEY (id),
    CONSTRAINT uk_centros_almacenamiento_public_id UNIQUE (public_id),
    CONSTRAINT uk_centros_almacenamiento_domicilio UNIQUE (domicilio_id),
    -- La app recorta los extremos: un nombre en blanco no se guarda.
    CONSTRAINT ck_centros_almacenamiento_nombre_formato CHECK (nombre = btrim(nombre) AND nombre <> ''),
    CONSTRAINT ck_centros_almacenamiento_cantidad CHECK (cantidad_neumaticos_en_stock >= 0)
);

ALTER TABLE almacenamiento.centros_almacenamiento ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON almacenamiento.centros_almacenamiento TO gn_administrador, gn_editor, gn_lector;
-- INSERT de todas las columnas: Hibernate envía el id que reservó con nextval.
GRANT INSERT ON almacenamiento.centros_almacenamiento TO gn_administrador, gn_editor;
-- Los identificadores (id, public_id) no se modifican.
GRANT UPDATE (nombre, cantidad_neumaticos_en_stock, domicilio_id)
    ON almacenamiento.centros_almacenamiento TO gn_administrador, gn_editor;
-- Hibernate pide los ids con nextval antes de insertar.
GRANT USAGE ON SEQUENCE almacenamiento.centros_almacenamiento_id_seq TO gn_administrador, gn_editor;
-- Solo para la validación de esquema de Hibernate (ver V4): no genera ids.
GRANT SELECT ON SEQUENCE almacenamiento.centros_almacenamiento_id_seq TO gn_app;

CREATE POLICY centros_almacenamiento_requiere_autenticacion ON almacenamiento.centros_almacenamiento
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY centros_almacenamiento_select ON almacenamiento.centros_almacenamiento
    FOR SELECT
    TO gn_administrador, gn_editor, gn_lector
    USING (true);

CREATE POLICY centros_almacenamiento_insert ON almacenamiento.centros_almacenamiento
    FOR INSERT
    TO gn_administrador, gn_editor
    WITH CHECK (true);

CREATE POLICY centros_almacenamiento_update ON almacenamiento.centros_almacenamiento
    FOR UPDATE
    TO gn_administrador, gn_editor
    USING (true)
    WITH CHECK (true);
