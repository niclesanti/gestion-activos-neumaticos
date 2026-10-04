-- Registro de publicación de eventos de Spring Modulith (JPA). Antes lo creaba
-- Modulith al arrancar, pero la app ya no se conecta con un rol que pueda
-- ejecutar DDL. IF NOT EXISTS: en las bases existentes la tabla ya se creó así.
-- Es infraestructura del framework y queda sin RLS, porque los listeners
-- asíncronos corren sin usuario autenticado.
CREATE TABLE IF NOT EXISTS public.event_publication (
    id                     UUID                        NOT NULL,
    completion_attempts    INTEGER                     NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    event_type             VARCHAR(255)                NOT NULL,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    listener_id            VARCHAR(255)                NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    serialized_event       VARCHAR(255)                NOT NULL,
    status                 VARCHAR(255),

    CONSTRAINT event_publication_pkey PRIMARY KEY (id),
    CONSTRAINT event_publication_status_check
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

GRANT USAGE ON SCHEMA public TO gn_app, gn_administrador, gn_editor, gn_lector;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.event_publication
    TO gn_app, gn_administrador, gn_editor, gn_lector;
