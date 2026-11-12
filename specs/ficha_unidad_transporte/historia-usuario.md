# HU-10 - Consultar la ficha detalle de una unidad de transporte

**Historia**: Como usuario del sistema (cualquier nivel de acceso), quiero visualizar la ficha detalle de una unidad de transporte, incluyendo una representación gráfica de su chasis y los neumáticos actualmente montados en cada posición, para conocer de un vistazo el estado físico completo del vehículo.

**Criterios de Aceptación**:
- CA1: Dado que un usuario (administrador, editor o lector) selecciona una unidad de transporte del listado, cuando accede a su ficha detalle, entonces el sistema muestra una representación gráfica esquemática del chasis con sus posiciones de eje (dirección, tracción, arrastre) y de auxilio.
- CA2: Dado que un usuario visualiza la ficha detalle de una unidad, entonces cada posición de la representación gráfica indica si tiene un neumático montado o si está vacía (ausencia de neumático), con una diferencia visible entre ambos casos.
- CA3: Dado que un usuario hace clic o selecciona una posición ocupada dentro de la representación gráfica, entonces el sistema muestra los datos principales del neumático montado en dicha posición: número de serie, marca, estado y kilometraje.

**Notas / Supuestos**:
- Esta historia cubre solo la visualización; la asignación de neumáticos se detalla en HU-31, que reutilizará esta misma representación gráfica.
- Hoy ninguna funcionalidad monta neumáticos (llega con HU-31): hasta entonces todas las posiciones de una unidad real se verán vacías.
- Una unidad dada de baja no tiene ficha consultable (se comporta como inexistente).
- Si el neumático montado no tiene número de serie o kilometraje registrados, la ficha lo indica como dato no registrado.
