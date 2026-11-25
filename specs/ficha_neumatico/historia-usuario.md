# HU-14 - Consultar la ficha detalle de un neumático

**Historia**: Como usuario del sistema (cualquier nivel de acceso), quiero visualizar la ficha detalle de un neumático con sus datos técnicos, estado actual y ubicación operativa vigente, para tener una vista consolidada de su situación sin recurrir a distintas pantallas.

**Descripción**: Actúa como pantalla principal de acceso al neumático, desde la cual se navega hacia su historial de trazabilidad (HU-33) y su ubicación operativa (HU-34). Debe consolidar: número de serie, marca, estado técnico actual, kilometraje, tipo de eje de origen, cantidad de recapados, comentarios y situación operativa vigente (en stock, montado en unidad/posición, en reparación, en espera, o de baja).

**Criterios de Aceptación**:
- CA1: Dado que un usuario selecciona un neumático del listado de `/tires`, cuando accede a su ficha detalle, entonces el sistema muestra número de serie, marca, estado técnico actual, kilometraje, tipo de eje de origen, cantidad de recapados, comentarios y su situación operativa vigente.
- CA2: Dado un neumático montado, en stock o en reparación, cuando un usuario ve su ficha, entonces se indica respectivamente la unidad (patente) y la posición (código), el centro de almacenamiento (nombre) o el servicio de reparación (nombre).
- CA3: Dado que un neumático está "en espera", cuando un usuario visualiza su ficha, entonces el sistema lo indica explícitamente, sin mostrar unidad, centro o servicio asociado.
- CA4: Dado un neumático de baja (`estadoActual` o situación `DE_BAJA`), cuando un usuario ve su ficha, entonces se indica "De baja" y no se muestra unidad, centro ni servicio.
- CA5: Dado un usuario en la ficha, cuando la observa, entonces ve las acciones "Ver historial" y "Ver ubicación" deshabilitadas con el texto "Próximamente". [parcial: se completa con HU-33/HU-34]
- CA6: Dado un dato que el neumático no tiene registrado (serie, kilometraje, recapados, comentario), cuando se ve la ficha, entonces se indica como no registrado.
- CA7: Dado un usuario autenticado (administrador, editor o lector), cuando pide `GET /api/v1/neumaticos/{publicId}` de un neumático activo, entonces responde 200 con el detalle y sin ids internos.
- CA8: Dado un `publicId` inexistente o de un neumático con baja lógica, cuando se pide la ficha, entonces responde 404; con un `publicId` que no es UUID, 400; sin token, 401.
- CA9: Dado un usuario Lector, cuando inicia sesión, entonces ve "Neumáticos" en la navegación, entra a `/tires`, abre la ficha y no ve el botón "Registrar neumático". ["cualquier nivel de acceso"]

**Notas / Supuestos**:
- El tercer criterio original queda cumplido solo en parte: HU-33 (historial) y HU-34 (ubicación) no existen todavía; las acciones se activarán cuando existan.
- Hoy ninguna funcionalidad monta, almacena ni envía a reparar neumáticos (HU-31 en adelante): hasta entonces los neumáticos reales están en espera.
- La ficha de un neumático de baja es consultable (decisión de la humana); el listado de HU-28 no los muestra.
- Abrir `/tires` al Lector cambia lo decidido en HU-28 (seguía cerrada).
