-- Usuarios de prueba, SOLO para el perfil dev (application-dev.properties suma
-- esta ubicación a spring.flyway.locations). Nunca se cargan en prod.
-- Es repetible (R__): se reaplica si cambia el archivo, sin pisar filas existentes.
--
-- Contraseñas (hash Argon2id generado con el PasswordEncoder de la app):
--   victoria.administrador, santiago.administrador / Admin.1234
--   victoria.editor,        santiago.editor        / Editor.1234
--   victoria.lector,        santiago.lector        / Lector.1234
-- Si cambia este listado, actualizar SinCredencialesPorDefecto (aborta prod si
-- alguno sobrevive con su clave por defecto).
INSERT INTO usuarios.usuarios (public_id, nombre_usuario, nombre_apellido, email, clave, nivel_acceso)
VALUES
    (uuidv7(), 'victoria.administrador', 'Victoria', 'victoria.administrador@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$CAvkpR3H0coqJBq0zauNQQ$s9zySEsPZuOLLPIIyOD3f1CoR5EY+nEk30nm4NkF8xw',
     'ROLE_ADMINISTRADOR'),
    (uuidv7(), 'victoria.editor', 'Victoria', 'victoria.editor@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$FiA1CPclxVkbmTHjgP2BWg$yh86jr5NTYhOkLG/O1Rgd9ajl5LKCjDg/qg06FmcY+E',
     'ROLE_EDITOR'),
    (uuidv7(), 'victoria.lector', 'Victoria', 'victoria.lector@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$EVvLz9TOBdzPptPL1xXzhw$9PiydCQ7vianTjjdNwBcYwBZpcSOBmEk99gC/a+YfD8',
     'ROLE_LECTOR'),
    (uuidv7(), 'santiago.administrador', 'Santiago', 'santiago.administrador@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$CAvkpR3H0coqJBq0zauNQQ$s9zySEsPZuOLLPIIyOD3f1CoR5EY+nEk30nm4NkF8xw',
     'ROLE_ADMINISTRADOR'),
    (uuidv7(), 'santiago.editor', 'Santiago', 'santiago.editor@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$FiA1CPclxVkbmTHjgP2BWg$yh86jr5NTYhOkLG/O1Rgd9ajl5LKCjDg/qg06FmcY+E',
     'ROLE_EDITOR'),
    (uuidv7(), 'santiago.lector', 'Santiago', 'santiago.lector@neumaticos.local',
     '{argon2}$argon2id$v=19$m=16384,t=2,p=1$EVvLz9TOBdzPptPL1xXzhw$9PiydCQ7vianTjjdNwBcYwBZpcSOBmEk99gC/a+YfD8',
     'ROLE_LECTOR')
ON CONFLICT DO NOTHING;
