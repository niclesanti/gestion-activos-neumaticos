# Spec — HU-32 desasignar_neumatico_de_posicion

Estado: `spec_ready`. Verificado en código: `NeumaticoApi.desmontar` deja `EN_ESPERA`, `enEspera=true` y sin slot (centro y servicio ya son nulos desde el montaje); `EventoDesasignacion` ya guarda unidad, patente, slot y posición (fecha y usuario, del evento base); V20 alcanza, no hace falta migración.

## Dudas para la humana

1. Método, ruta y respuesta: ¿`DELETE .../slots/{codigo}/neumatico` con 200 + ficha actualizada, o 204? Propuesta: 200 + ficha, para refrescar el diagrama sin GET extra, igual que HU-31.
2. Posición ya libre: ¿409 o 422? Propuesta: 409 con `PosicionLibreException` nueva, coherente con `PosicionOcupadaException` y `NeumaticoNoDisponibleException`.
3. Carrera: ¿bloqueo pesimista de la unidad al desmontar? Propuesta: sí, porque dos desmontajes simultáneos no chocan con ningún índice único y duplicarían el evento; con el bloqueo el segundo recibe 409. Montar vs desmontar ya lo cubren los índices de V20.
4. Refactor: ¿extraer el desmontaje (desmontar + evento) a un método privado compartido por `asignarNeumatico` y `desasignarNeumatico`? Propuesta: sí, sin cambiar el contrato ni los eventos de HU-31, para no duplicar el flujo.
5. Interfaz: ¿"Desmontar neumático" en el `SlotPopover` junto a "Reemplazar", confirmación con `AlertDialog` (serie + posición, foco en Cancelar), tras confirmar cierra, `notify.success` y actualiza el diagrama; 409/404 refrescan la ficha con `notify.error`? Propuesta: sí, mismo criterio que el drawer de HU-31.
6. Migración y privilegios: ¿ninguna migración nueva? Propuesta: ninguna, V20 ya permite `UPDATE (neumatico_actual_id)` y la firma de la unidad (`updated_*`, que el servicio toca como en HU-31).
7. Textos y accesibilidad: ¿es/en en `transport-units.json` (`unmount.*`), `AlertDialog` con título y descripción asociados, botón destructivo con estado de carga? Propuesta: sí, con voseo en es.
8. Permiso de UI: ¿nueva acción `transportUnits.unassignTire` (admin/editor) en vez de reutilizar `assignTire`? Propuesta: nueva, para que cada acción tenga su regla (`ROUTE_ACCESS` no cambia).
