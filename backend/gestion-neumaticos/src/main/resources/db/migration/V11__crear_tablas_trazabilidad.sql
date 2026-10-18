-- Módulo trazabilidad: historial de eventos de cada neumático (id + snapshot).
--
-- Lo escriben solo los listeners asíncronos de Modulith, que corren después del
-- commit y sin usuario autenticado: la conexión queda como gn_app. Por eso gn_app
-- es el único rol que inserta, igual que es el que completa event_publication
-- (V7). Ningún rol de usuario escribe ni modifica el historial; la lectura se
-- habilitará junto con el endpoint de consulta del historial.
--
-- Todos los subtipos de evento viven en una sola tabla (herencia SINGLE_TABLE,
-- con `tipo` como discriminador): la consulta principal es "todo el historial de
-- un neumático", que así es un solo recorrido del índice (neumatico_id, fecha)
-- en vez de un LEFT JOIN por subtipo, y cada evento es un solo INSERT. Las
-- columnas propias de cada subtipo son NULL en los demás (lo garantiza un CHECK
-- por subtipo); cada subtipo nuevo agrega sus columnas y su CHECK en su propia
-- migración.
CREATE SCHEMA IF NOT EXISTS trazabilidad;
GRANT USAGE ON SCHEMA trazabilidad TO gn_app, gn_administrador, gn_editor, gn_lector;

-- INCREMENT BY = allocationSize de @SequenceGenerator en EventoNeumatico.
CREATE SEQUENCE trazabilidad.eventos_neumatico_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE trazabilidad.eventos_neumatico (
    id                     BIGINT                      NOT NULL DEFAULT nextval('trazabilidad.eventos_neumatico_id_seq'),
    tipo                   VARCHAR(30)                 NOT NULL,
    fecha                  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    usuario                VARCHAR(100)                NOT NULL,
    comentario             VARCHAR(512),
    -- Referencia formal al neumático de otro módulo: sin FK entre schemas de módulos.
    neumatico_id           BIGINT                      NOT NULL,
    numero_serie_neumatico VARCHAR(100),
    -- COMPRA: el proveedor es opcional.
    proveedor_id           BIGINT,
    nombre_proveedor       VARCHAR(150),

    CONSTRAINT pk_eventos_neumatico PRIMARY KEY (id),
    CONSTRAINT ck_eventos_neumatico_tipo CHECK (tipo IN ('COMPRA', 'ENVIO_REPARACION', 'REGRESO_REPARACION',
        'ASIGNACION', 'DESASIGNACION', 'ENVIO_STOCK', 'REGRESO_STOCK', 'BAJA_OPERATIVA', 'OTROS')),
    CONSTRAINT ck_eventos_neumatico_datos_compra CHECK (
        tipo = 'COMPRA' OR (proveedor_id IS NULL AND nombre_proveedor IS NULL))
);

CREATE INDEX ix_eventos_neumatico_neumatico_fecha ON trazabilidad.eventos_neumatico (neumatico_id, fecha);

ALTER TABLE trazabilidad.eventos_neumatico ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON trazabilidad.eventos_neumatico TO gn_app;
-- USAGE para nextval al insertar; SELECT también lo pide la validación de Hibernate.
GRANT USAGE, SELECT ON SEQUENCE trazabilidad.eventos_neumatico_id_seq TO gn_app;

CREATE POLICY eventos_neumatico_requiere_autenticacion ON trazabilidad.eventos_neumatico
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY eventos_neumatico_insert_sistema ON trazabilidad.eventos_neumatico
    FOR INSERT
    TO gn_app
    WITH CHECK (true);
