-- Módulo usuarios: cada módulo de Modulith tiene su propio schema.
CREATE SCHEMA IF NOT EXISTS usuarios;

-- INCREMENT BY debe coincidir con el allocationSize de @SequenceGenerator en
-- Usuario: Hibernate reserva bloques de 50 ids por cada llamada a nextval.
CREATE SEQUENCE usuarios.usuarios_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE usuarios.usuarios (
    id              BIGINT       NOT NULL DEFAULT nextval('usuarios.usuarios_id_seq'),
    public_id       UUID         NOT NULL,
    nombre_usuario  VARCHAR(30)  NOT NULL,
    nombre_apellido VARCHAR(100) NOT NULL,
    email           VARCHAR(254) NOT NULL,
    clave           VARCHAR(255) NOT NULL,
    nivel_acceso    VARCHAR(20)  NOT NULL,

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    -- Índices únicos: public_id lo busca cada request autenticado (sub del JWT);
    -- nombre_usuario y email, el login (se puede ingresar con cualquiera de los dos).
    CONSTRAINT uk_usuarios_public_id UNIQUE (public_id),
    CONSTRAINT uk_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    -- El login normaliza a minúsculas: se exige lo mismo al persistir para que
    -- la unicidad no dependa de mayúsculas.
    CONSTRAINT ck_usuarios_nombre_usuario_minusculas CHECK (nombre_usuario = lower(nombre_usuario)),
    CONSTRAINT ck_usuarios_email_minusculas CHECK (email = lower(email)),
    CONSTRAINT ck_usuarios_nivel_acceso CHECK (nivel_acceso IN ('ROLE_ADMINISTRADOR', 'ROLE_EDITOR', 'ROLE_LECTOR'))
);
