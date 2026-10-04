-- Tokens cerrados con logout (HU-02). Un JWT es stateless: la única forma de
-- invalidarlo antes de que expire es recordar su jti hasta esa fecha.
CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE seguridad.tokens_revocados (
    jti       UUID        NOT NULL,
    expira_en TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_tokens_revocados PRIMARY KEY (jti)
);

-- La limpieza diaria borra por rango de expiración.
CREATE INDEX ix_tokens_revocados_expira_en ON seguridad.tokens_revocados (expira_en);
