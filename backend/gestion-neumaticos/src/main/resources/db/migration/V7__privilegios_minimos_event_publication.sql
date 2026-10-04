-- Privilegio mínimo sobre public.event_publication (Spring Modulith). V6 daba
-- SELECT/INSERT/UPDATE/DELETE a los cuatro roles: con una inyección SQL se
-- podían falsificar o borrar eventos (por ejemplo, los de auditoría).
--   - Usuarios que operan (administrador, editor): publican eventos dentro de su
--     transacción (INSERT) y los listeners síncronos los marcan completos
--     (SELECT + UPDATE). Nunca borran.
--   - Lector: solo consulta, no publica eventos de negocio. Sin acceso.
--   - gn_app (sin usuario): los listeners asíncronos y la republicación de
--     eventos incompletos corren sin usuario; marcan, reintentan y purgan.
-- La tabla sigue sin RLS: es infraestructura del framework y no guarda datos
-- por usuario.

REVOKE ALL ON public.event_publication FROM gn_app, gn_administrador, gn_editor, gn_lector;

GRANT SELECT, INSERT, UPDATE ON public.event_publication TO gn_administrador, gn_editor;
GRANT SELECT, UPDATE, DELETE ON public.event_publication TO gn_app;
