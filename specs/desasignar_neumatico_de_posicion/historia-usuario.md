# HU-32 - Desasignar un neumático de una posición de una unidad de transporte

**Historia**: Como Editor o Administrador, quiero desasignar (desmontar) un neumático que se encuentra actualmente montado en una posición de una unidad de transporte, para liberar dicha posición y disponer del neumático (reasignarlo, enviarlo a un centro de almacenamiento, enviarlo a reparación, o darlo de baja) sin necesidad de reemplazarlo inmediatamente por otro.

Al ejecutarse, el neumático queda en estado "en espera", sin unidad ni posición asociada, disponible para cualquier operación posterior. Habilita también la rotación de neumáticos entre posiciones (desmontar + volver a asignar en otra posición de una misma unidad).

**Criterios de Aceptación** (literales):

- CA1: Dado que un Editor o Administrador visualiza la ficha detalle de una unidad de transporte con al menos una posición ocupada, cuando hace clic sobre dicha posición y selecciona "Desmontar", entonces el sistema debe solicitar confirmación antes de ejecutar la acción.
- CA2: Dado que la desasignación es confirmada, cuando la operación se ejecuta, entonces el sistema debe liberar el slot de la unidad, marcar el neumático como "en espera", actualizar la representación gráfica del vehículo, y registrar un nuevo evento de desasignación en el historial del neumático (unidad, posición, fecha y usuario responsable).
- CA3: Dado que se intenta desmontar una posición que ya se encuentra libre, entonces el sistema debe impedir la operación.
- CA4: Dado que un usuario con nivel de acceso Lector intenta acceder a esta funcionalidad, entonces el sistema debe denegar el acceso a dicha pantalla o funcionalidad.

Precisiones verificables:

- CA5: Desmontar una posición ocupada responde 200 con la ficha actualizada (posición sin neumático); el neumático queda con situación "En espera", sin unidad, posición, centro ni servicio, y la posición queda libre para volver a montar. [CA2]
- CA6: Tras desmontar, el historial del neumático tiene un evento de desasignación con unidad (patente), posición, fecha y usuario; y la unidad queda firmada como modificada por ese usuario. [CA2]
- CA7: Desmontar una posición libre responde 409 con `ExceptionInfo`, sin cambios ni evento. [CA3]
- CA8: Unidad inexistente o dada de baja, o posición que no pertenece a la unidad, responden 404 sin cambios. [CA2]
- CA9: El Lector recibe 403 al desmontar y en la ficha no ve la acción "Desmontar" (las posiciones siguen mostrando sus datos). [CA4]
- CA10: En la ficha, "Desmontar" aparece en el popover de una posición ocupada (solo con permiso); al elegirla se abre una confirmación que nombra el neumático y la posición, y no se ejecuta nada hasta confirmarla; cancelar no cambia nada. [CA1]
- CA11: Al confirmar, la ficha se actualiza (la posición queda vacía), se notifica el éxito y el neumático figura "En espera" en el listado de neumáticos. [CA2]

**Notas / Supuestos**:

- Base: HU-31 (asignar), ya implementada; el reemplazo ya desmonta como parte de su flujo.
- La rotación entre posiciones no es una acción aparte: es desmontar y volver a asignar (el buscador de HU-31 ya incluye neumáticos "en espera"). Fuera de alcance.
- Fuera de alcance: kilometraje al desmontar, y consultar el historial (HU-33): solo se verifica que el evento se registre.
- Sin runner de tests en el frontend: CA1, CA9 (UI), CA10 y CA11 se verifican con lint + typecheck + build y prueba en navegador.
