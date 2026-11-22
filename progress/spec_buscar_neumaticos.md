# Spec — HU-28 buscar_neumaticos

Spec en `specs/buscar_neumaticos/` (status `spec_ready`). Lo de abajo está propuesto en el spec pero no lo resuelven la historia ni las decisiones ya tomadas.

## Dudas para la humana

- ¿El número de serie distingue mayúsculas? Propuse que no (se normaliza el término a mayúsculas como en el alta), porque se guardan en mayúsculas y si no, buscar en minúscula nunca encontraría nada.
- ¿Marca y Estado aceptan un valor o varios? Propuse uno solo cada uno (`Select` con "Todas" / "Todos"), porque la historia habla de "marca y estado" en singular y es lo más simple de usar; varios valores necesitarían un multiselect que no está instalado.
- ¿El filtro Estado ofrece "De baja"? Propuse que no, porque un neumático de baja nunca es activo y siempre daría vacío (el backend igual responde 200 vacío).
- ¿Qué columnas muestra el listado? Propuse Número de serie, Marca, Estado, Situación operativa y Kilometraje, porque son los datos que la historia y la ficha (HU-10) usan para identificar un neumático.
- ¿Orden por defecto y campos ordenables? Propuse `numeroSerie` ascendente como único campo ordenable (sin serie al final), siguiendo HU-27 (ordena por su campo de búsqueda).
- ¿Cómo se muestran los neumáticos sin número de serie? Propuse el texto atenuado "Sin número de serie" en esa celda, y que un filtro por serie nunca los devuelva.
- ¿Dónde va la búsqueda? Propuse una barra de filtros entre el encabezado y la tabla, que busca sola al cambiar cualquier filtro (sin botón "Buscar"), con "Limpiar filtros", como HU-27.
- ¿Un `marcaId` inexistente da 404 o vacío? Propuse 200 vacío, porque es un filtro de búsqueda, no un recurso pedido.
- ¿Mensaje cuando no hay resultados? La historia no lo pide; propuse "No se encontraron neumáticos" (con filtros) y "Todavía no hay neumáticos registrados" (sin filtros), como HU-27, en el design y no como CA.
