# HU-31 - Asignar un neumático a una posición de una unidad de transporte

**Historia**: Como Editor o Administrador, quiero asignar un neumático disponible en stock a una posición específica (slot) de una unidad de transporte, interactuando directamente mediante un clic sobre la representación gráfica del vehículo, para agilizar la operación de montaje y reducir el error humano de transcripción manual.

**Criterios de Aceptación** (literales):

- CA1: Dado que un Editor o Administrador visualiza la ficha detalle de una unidad de transporte con al menos una posición libre, cuando hace clic sobre dicha posición, entonces el sistema debe desplegar un buscador de neumáticos disponibles para asignar, incluyendo tanto los que se encuentran en stock de cualquier centro de almacenamiento como los que se encuentran en el buffer de "en espera". Además de seleccionar el neumático, se puede seleccionar opcionalmente el conductor de esa unidad de transporte para asignarlo al evento.
- CA2: Dado que se selecciona un neumático disponible y confirma la asignación a la posición elegida, cuando la operación se ejecuta, entonces el sistema debe montar el neumático en dicha posición, retirarlo del stock del centro de almacenamiento en el que se encontraba, actualizar la representación gráfica del vehículo y registrar un nuevo evento de asignación en el historial del neumático (indicando unidad, posición, conductor asociado si corresponde, fecha y usuario responsable).
- CA3: Dado que se intenta asignar a una posición que ya posee un neumático montado, entonces el sistema debe lanzar una advertencia, que de ser aceptada se produce la acción de desasignar el neumático montado y finalmente se coloca el neumático en la posición elegida.
- CA4: Dado que se intenta asignar un neumático que no se encuentra actualmente en stock ni en espera (que ya está montado en otra unidad, en reparación o dado de baja), entonces el sistema debe impedir la operación.
- CA5: Dado que un usuario con nivel de acceso Lector intenta acceder a esta funcionalidad, entonces el sistema debe denegar el acceso a dicha pantalla o funcionalidad.

Precisiones verificables:

- CA6: Tras asignar, el neumático queda con situación "Montado" en esa unidad y posición; si venía de un centro, el centro tiene un neumático menos en stock y el neumático ya no figura en él. [CA2]
- CA7: Asignar a una posición ocupada sin haber aceptado la advertencia responde 409 y no cambia nada; aceptada, el neumático desmontado queda "En espera" y su historial registra un evento de desasignación (unidad, posición, fecha, usuario). [CA3]
- CA8: Un neumático montado, en reparación, de baja (estado o situación) o con baja lógica es rechazado con 409 y `ExceptionInfo`, sin cambios; uno inexistente, 404. [CA4]
- CA9: Unidad inexistente o dada de baja, posición que no pertenece a la unidad o conductor inexistente responden 404 sin cambios. [CA1, CA2]
- CA10: El Lector recibe 403 al asignar y en la ficha no ve la acción (las posiciones siguen mostrando sus datos como hoy). [CA5]

**Notas / Supuestos**:

- Hoy no existe vínculo entre conductor y unidad: "el conductor de esa unidad" se elige entre todos los conductores activos.
- "Disponible" = activo (sin baja lógica ni estado "De baja") con situación "En stock" o "En espera".
- El buscador permite filtrar por número de serie y por origen: todos los disponibles, en espera, en stock de cualquier centro o de un centro en particular.
- El desmontaje como operación independiente es HU-32; acá solo ocurre como parte del reemplazo (CA3).
- Fuera de alcance: kilometraje al montar/desmontar y consultar el historial (HU-33).
- Sin runner de tests en el frontend: CA1, CA3 (advertencia) y CA10 (UI) se verifican con lint + typecheck + build y prueba en navegador.
