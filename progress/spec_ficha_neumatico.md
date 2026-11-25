# Spec — HU-14 ficha_neumatico

Spec en `specs/ficha_neumatico/` (status `spec_ready`). Lo de abajo está propuesto pero no lo resuelven la historia ni las decisiones ya tomadas.

## Dudas para la humana

- ¿Se muestran también `tipoActual` y `kilometrajeUltimaReparacion`? Propuse que sí (aparte de lo que pide la historia), porque el alta ya los maneja y son datos técnicos; si no, se quitan del DTO y del dialog.
- ¿Cómo se muestran los datos desconocidos (serie, kilometraje, recapados, comentario)? Propuse "No registrado" (y "Sin número de serie" en el título), como HU-10 y HU-28.
- ¿Qué se muestra si la situación pide ubicación pero el dato no se resuelve (id nulo o que ya no existe)? Propuse "Ubicación no disponible", sin error, porque hoy esos ids no tienen FK.
- ¿Una unidad, centro o servicio dado de baja se nombra igual? Propuse que sí, porque la ficha dice dónde está físicamente el neumático.
- ¿Orden y agrupación de la ficha? Propuse bloque "Datos técnicos" (colapsable, abierto) y bloque "Ubicación operativa", con badges de estado y situación en el título, como HU-10.
- ¿Cómo se indica "De baja"? Propuse badge "De baja" y texto "De baja" en la ubicación (sin unidad/centro/servicio), mostrando igual los datos técnicos.
- ¿Texto/forma de las acciones deshabilitadas? Propuse botones "Ver historial" y "Ver ubicación" en el pie, deshabilitados, con `Tooltip` "Próximamente".
- ¿Un neumático en espera además muestra la leyenda "En espera" o algo más? Propuse "En espera" como situación explícita, sin más texto.
- El `PUT`/edición del neumático (`NeumaticoUpdateDTO`) no forma parte de esta HU: la ficha es solo lectura.
