-- Usuarios de prueba, SOLO para el perfil dev (application-dev.properties suma
-- esta ubicación a spring.flyway.locations). Nunca se cargan en prod.
-- Es repetible (R__): se reaplica si cambia el archivo, sin pisar filas existentes.
--
-- Contraseñas (hash Argon2id generado con el PasswordEncoder de la app):
--   administrador / Admin.1234
--   editor        / Editor.1234
--   lector        / Lector.1234
--   editor2       / Editor.1234
--   lector2       / Lector.1234
-- Hay más de un editor y un lector para comprobar que cada uno ve solo su fila
-- (RLS) y que el administrador ve todas.
INSERT INTO usuarios.usuarios (public_id, nombre_usuario, nombre_apellido, email, clave, nivel_acceso)
VALUES
    (uuidv7(), 'administrador', 'Ana Administradora', 'administrador@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$CAvkpR3H0coqJBq0zauNQQ$s9zySEsPZuOLLPIIyOD3f1CoR5EY+nEk30nm4NkF8xw',
     'ROLE_ADMINISTRADOR'),
    (uuidv7(), 'editor', 'Ernesto Editor', 'editor@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$FiA1CPclxVkbmTHjgP2BWg$yh86jr5NTYhOkLG/O1Rgd9ajl5LKCjDg/qg06FmcY+E',
     'ROLE_EDITOR'),
    (uuidv7(), 'lector', 'Lucía Lectora', 'lector@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$EVvLz9TOBdzPptPL1xXzhw$9PiydCQ7vianTjjdNwBcYwBZpcSOBmEk99gC/a+YfD8',
     'ROLE_LECTOR'),
    (uuidv7(), 'editor2', 'Elena Editora', 'editor2@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$FiA1CPclxVkbmTHjgP2BWg$yh86jr5NTYhOkLG/O1Rgd9ajl5LKCjDg/qg06FmcY+E',
     'ROLE_EDITOR'),
    (uuidv7(), 'lector2', 'Lorenzo Lector', 'lector2@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$EVvLz9TOBdzPptPL1xXzhw$9PiydCQ7vianTjjdNwBcYwBZpcSOBmEk99gC/a+YfD8',
     'ROLE_LECTOR')
ON CONFLICT DO NOTHING;
