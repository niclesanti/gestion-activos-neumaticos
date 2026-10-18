-- El evento serializado de Modulith no entra en 255 caracteres (ej. el evento de
-- compra de un neumático con número de serie y comentario largos). TEXT, como
-- el esquema de referencia de Spring Modulith para PostgreSQL.
ALTER TABLE public.event_publication ALTER COLUMN serialized_event TYPE TEXT;
