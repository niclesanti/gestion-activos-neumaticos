-- Módulo trazabilidad: historial de eventos de cada neumático (id + snapshot).
--
-- Lo escriben solo los listeners asíncronos de Modulith, que corren después del
-- commit y sin usuario autenticado: la conexión queda como gn_app. Por eso gn_app
-- es el único rol que inserta, igual que es el que completa event_publication
-- (V7). Ningún rol de usuario escribe ni modifica el historial; la lectura se
-- habilitará junto con el endpoint de consulta del historial.
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

    CONSTRAINT pk_eventos_neumatico PRIMARY KEY (id),
    CONSTRAINT ck_eventos_neumatico_tipo CHECK (tipo IN ('COMPRA', 'ENVIO_REPARACION', 'REGRESO_REPARACION',
        'ASIGNACION', 'DESASIGNACION', 'ENVIO_STOCK', 'REGRESO_STOCK', 'BAJA_OPERATIVA', 'OTROS'))
);

CREATE INDEX ix_eventos_neumatico_neumatico_fecha ON trazabilidad.eventos_neumatico (neumatico_id, fecha);

CREATE TABLE trazabilidad.eventos_compra (
    id               BIGINT       NOT NULL,
    proveedor_id     BIGINT,
    nombre_proveedor VARCHAR(150),

    CONSTRAINT pk_eventos_compra PRIMARY KEY (id),
    CONSTRAINT fk_eventos_compra_evento FOREIGN KEY (id) REFERENCES trazabilidad.eventos_neumatico (id)
);

ALTER TABLE trazabilidad.eventos_neumatico ENABLE ROW LEVEL SECURITY;
ALTER TABLE trazabilidad.eventos_compra ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON trazabilidad.eventos_neumatico, trazabilidad.eventos_compra TO gn_app;
-- USAGE para nextval al insertar; SELECT también lo pide la validación de Hibernate.
GRANT USAGE, SELECT ON SEQUENCE trazabilidad.eventos_neumatico_id_seq TO gn_app;

CREATE POLICY eventos_neumatico_requiere_autenticacion ON trazabilidad.eventos_neumatico
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY eventos_compra_requiere_autenticacion ON trazabilidad.eventos_compra
    AS RESTRICTIVE
    FOR ALL
    TO gn_administrador, gn_editor, gn_lector
    USING ((SELECT seguridad.usuario_actual()) IS NOT NULL)
    WITH CHECK ((SELECT seguridad.usuario_actual()) IS NOT NULL);

CREATE POLICY eventos_neumatico_insert_sistema ON trazabilidad.eventos_neumatico
    FOR INSERT
    TO gn_app
    WITH CHECK (true);

CREATE POLICY eventos_compra_insert_sistema ON trazabilidad.eventos_compra
    FOR INSERT
    TO gn_app
    WITH CHECK (true);
