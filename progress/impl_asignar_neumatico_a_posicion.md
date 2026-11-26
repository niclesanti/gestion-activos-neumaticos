# Implementación — HU-31 asignar_neumatico_a_posicion

## Resumen

Backend:
- **V20__asignacion_neumaticos.sql**: `GRANT UPDATE (neumatico_actual_id)` en slots con política `UPDATE` (admin/editor); `GRANT UPDATE (updated_at, updated_by)` en `unidades_transporte` con una política firmada igual que V19. También agrega índices únicos parciales `uk_slots_unidad_transporte_neumatico` y `uk_neumaticos_slot_montado`, y en `eventos_neumatico` las columnas `unidad_id`, `patente_unidad`, `slot_id`, `posicion_slot`, `conductor_id` y `nombre_conductor`, con dos CHECK: `ck_eventos_neumatico_datos_montaje` y `ck_eventos_neumatico_conductor_coherente`.
- **conductores**: nueva named interface `conductores.api` (`ConductorApi`, `ConductorResumenDTO`) con `ConductorApiImpl`. Nuevo `GET /api/v1/conductores?q=&page=&size=`, paginado, que devuelve solo activos y ordena siempre por apellido y nombre (ignora el `sort` del cliente). `q` busca en apellido, nombre o DNI.
- **almacenamiento**: el puerto `neumaticos.api.StockCentroAlmacenamiento` lo implementa `StockCentroAlmacenamientoImpl`, que resuelve el id por publicId, trae los nombres por lote y descuenta el stock con lock `PESSIMISTIC_WRITE` sobre la fila del centro. Nuevo `GET /api/v1/centros-almacenamiento`: activos con su stock, por nombre y sin paginar.
- **neumaticos**: `NeumaticoApi.montar(publicId, slotId)` y `desmontar(id)`. La regla de disponibilidad vive en `Neumatico.estaDisponible()`: activo, sin estado `DE_BAJA` y en stock o en espera. Si no está disponible responde 409 con `NeumaticoNoDisponibleException`; si no existe, 404. `desmontar` hace flush inmediato para liberar el índice único antes del montaje siguiente. En `GET /api/v1/neumaticos`, `situacion` pasa a ser repetible y se suma el filtro `centro` (un centro inexistente da página vacía). `NeumaticoListadoDTO` incluye ahora `centroAlmacenamiento`, resuelto por lote.
- **unidades_transporte**: `PUT /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico` (`hasRole('EDITOR')`) con `AsignacionNeumaticoRequestDTO`. `UnidadTransporteService.asignarNeumatico` corre en una sola transacción:
  1. Busca la unidad y la posición por código, sin distinguir mayúsculas.
  2. Valida el conductor opcional vía `ConductorApi`.
  3. Si la posición está ocupada: sin `reemplazar` responde 409 `PosicionOcupadaException`; con `reemplazar` llama a `desmontar`. Si el neumático a montar es el mismo que ya está, responde 409.
  4. Llama a `montar`, asigna el slot y firma la raíz con `AuditableEntity.registrarModificacion`.
  5. Un `DataIntegrityViolationException` (por carrera contra los índices) se traduce a 409 `PosicionOcupadaException`.
  6. Publica `NeumaticoDesasignado` (si hubo reemplazo) y `NeumaticoAsignado` en `unidades_transporte.event` (`@NamedInterface("event")`).

  `UnidadTransporte` lleva `@DynamicUpdate` y las columnas del slot que no cambian son `updatable = false`, para que el UPDATE quede dentro de los GRANT de V20.
- **trazabilidad**: nuevos subtipos `EventoAsignacion` y `EventoDesasignacion`. `MontajeNeumaticoListener` funciona igual que el de compra: asíncrono, después del commit y con `ComoSistema`.
- **exception**: nuevas `PosicionOcupadaException` y `NeumaticoNoDisponibleException`, ambas 409 en `ControllerAdvisor`. El 422 de `OperacionNoPermitidaException` no cambió.
- **Docs**: `docs/Seguridad/seguridad.md` tiene las 4 filas nuevas o ajustadas en la tabla 2.10 y la tabla de privilegios de unidades/slots actualizada en 2.11.

Frontend:
- **Componentes shadcn instalados**: `drawer` (Base UI), `combobox` (más su dependencia `input-group`), `radio-group`, `checkbox` y `alert`. Les cambié el import de `cn` a `@/lib/utils`. La CLI quería sobrescribir `button`, `input` y `textarea`: los respaldé y los restauré sin cambios.
- `lib/access.ts`: nueva acción `transportUnits.assignTire` (admin y editor).
- `lib/tire-picker.ts`: contrato `PickedTire`/`TirePickerProps`/`RenderTirePicker` más `TirePickerContext`. `router.tsx` le pasa `renderTirePicker={(p) => <TirePicker {...p} />}` a `TransportUnitsPage`, que lo expone por el contexto.
- `features/tires`: `searchTires` admite `situaciones[]` y `centro`, y serializa repitiendo la clave `situacion`. Nuevos `storage-centers-api.ts` y `use-storage-centers.ts`. `FilterSelect` ahora se exporta y acepta `className`. Nuevo `components/tire-picker.tsx`:
  - búsqueda por serie con foco automático;
  - select "Origen" con cuatro opciones: todos los disponibles, en espera, en stock, o un centro con su cantidad;
  - lista `RadioGroup` con badge "En espera" o el nombre del centro;
  - paginación de a 6, con skeleton, estados de vacío, sin resultados y error con reintento.

  Un clic o Enter elige y avanza; las flechas solo marcan.
- `features/transport-units`:
  - **Datos**: `drivers-api.ts` y `use-driver-search.ts` para la búsqueda en servidor; `assignTire` y `use-assign-tire.ts`, que guarda la ficha devuelta e invalida `["transport-units"]` (salvo configuraciones) y `["tires"]`.
  - **`slot-popover.tsx`**: con `onMount` muestra "Montar neumático" o "Reemplazar neumático" y cierra el popover. Sin `onMount` (lector) no muestra ninguna acción.
  - **`transport-unit-detail-dialog.tsx`**: resalta la posición destino con un anillo y `motion-safe:animate-pulse`. Mientras el panel está abierto, en `lg` corre la ficha a la izquierda para que el chasis quede a la vista.
  - **`mount-tire-drawer.tsx`**: `Drawer` no modal con `disablePointerDismissal`, a la derecha en escritorio (28rem) y abajo en mobile (75dvh). Tiene cabecera fija "PATENTE · CÓDIGO · tipo de eje", indicador de pasos con `aria-current="step"` y pie fijo con Atrás/Cancelar, Omitir y Continuar/Confirmar. El foco pasa al título de cada paso (al buscador en el paso 1). Los pasos:
    - **Neumático**: si es un reemplazo, avisa cuál sale.
    - **Conductor**: `Combobox` con búsqueda en servidor; se puede omitir.
    - **Confirmar**: resumen con botón "Cambiar" en cada bloque, `Alert` de registro inmutable y checkbox que habilita "Confirmar montaje" o "Confirmar reemplazo". En reemplazo, ese checkbox es la advertencia de CA3 y manda `reemplazar: true`. La aceptación vale solo para el neumático saliente que se mostró: si la ficha cambia, hay que volver a aceptar.

    Manejo de errores: un 409 vuelve a leer la ficha. Si otro neumático ocupó la posición, vuelve al paso 3 como reemplazo con un aviso; si no, vuelve al paso 1 con el aviso "ya no está disponible". Un 404 vuelve al paso 1 con un aviso inline. Un 403 o un error inesperado se informan con `notify.error`; el éxito, con `notify.success`, y el panel se cierra.
- Textos es/en: `transport-units.json` (`mount.*`) y `tires.json` (`picker.*`).

`./verify.sh --all` corrió una vez y salió todo verde: coherencia de `feature_list.json`, `mvnw verify` con JaCoCo, lint, typecheck, build y `npm audit`.

## Trazabilidad

- CA1 (buscador de disponibles y conductor opcional):
  - `BusquedaNeumaticoIntegrationTests.disponiblesYCentro`, `NeumaticoServiceImplTest.Buscar.centroInexistente` / `conCentroYNombres`, `NeumaticoControllerTest.Buscar.buscaConLector` (situación repetida y `centro`).
  - `CentroAlmacenamientoControllerTest.listaCentros`, `ConductorControllerTest.Buscar.*`, `ConductorServiceImplTest.Buscar.ordenFijo`.
  - `UnidadTransporteServiceImplTest.AsignarNeumatico.conConductor`.
  - UI: navegador.
- CA2 (montar, retirar del stock, actualizar la ficha y registrar la asignación):
  - `AsignacionNeumaticoIntegrationTests.montaDesdeStock`, `UnidadTransporteServiceImplTest.AsignarNeumatico.montaEnPosicionLibre`.
  - `NeumaticoApiImplTest.Montar.desdeEspera`, `MontajeNeumaticoListenerTest.asignacion`.
  - `UnidadTransporteControllerTest.AsignarNeumatico.montaConEditor` (REST Docs `unidad-transporte-montar-neumatico`).
  - UI: navegador.
- CA3 (advertencia y reemplazo): `AsignacionNeumaticoIntegrationTests.reemplazo`, `UnidadTransporteServiceImplTest.AsignarNeumatico.reemplaza`. La advertencia y el checkbox del paso 3 se comprueban en el navegador.
- CA4 / CA8 (no disponibles → 409, inexistente → 404):
  - `AsignacionNeumaticoIntegrationTests.noDisponibles`.
  - `NeumaticoApiImplTest.Montar.situacionNoDisponible` / `deBaja` / `inexistente`.
  - `UnidadTransporteServiceImplTest.AsignarNeumatico.neumaticoNoDisponible` / `reemplazoPorElMismo`.
  - `UnidadTransporteControllerTest.AsignarNeumatico.noDisponible`, `ControllerAdvisorTest`.
- CA5 / CA10 (el lector no monta):
  - `UnidadTransporteControllerTest.AsignarNeumatico.lectorNoMonta` (403).
  - `AutorizacionBaseDatosTests.UnidadesTransporte.lectorSoloLee` (el UPDATE del slot sigue denegado para el lector).
  - UI: el lector no ve la acción en el popover (navegador).
- CA6 (stock del centro): `AsignacionNeumaticoIntegrationTests.montaDesdeStock` (el stock pasa de 1 a 0 y el centro queda null), `NeumaticoApiImplTest.Montar.desdeStock`, `StockCentroAlmacenamientoImplTest.retira` / `errores`.
- CA7 (ocupada sin aceptar → 409 sin cambios; aceptada → saliente en espera y evento de desasignación):
  - `AsignacionNeumaticoIntegrationTests.reemplazo`.
  - `UnidadTransporteServiceImplTest.AsignarNeumatico.ocupadaSinReemplazo` / `carrera`.
  - `NeumaticoApiImplTest.Desmontar.quedaEnEspera`, `MontajeNeumaticoListenerTest.desasignacion`.
  - `UnidadTransporteControllerTest.AsignarNeumatico.ocupada`.
- CA9 (404 sin cambios): `AsignacionNeumaticoIntegrationTests.noEncontradosSinCambios` (verifica el rollback), `UnidadTransporteServiceImplTest.AsignarNeumatico.noEncontrados`, `UnidadTransporteControllerTest.AsignarNeumatico.noEncontrado`.
- Migración V20 y RLS: `AutorizacionBaseDatosTests.UnidadesTransporte.editorYAdministradorInsertan` / `modificacionDeLaUnidadFirmada` / `montajeUnico`, `Trazabilidad.datosDeMontajeSoloEnAsignaciones`. Límites de módulos: `ModularityTests`.

## Pasos para el navegador

Antes de probar hacen falta datos en dev. Desde Configuración, como `victoria.editor` / `Editor.1234`:
- al menos un centro de almacenamiento y un conductor;
- al menos una unidad (tractor común), desde Unidades de transporte.

Los neumáticos registrados (HU-11) quedan "En espera". Para tener uno "En stock" con centro hay que cargarlo a mano en la base: no hay HU de envío a stock todavía.

1. **Editor, montaje en posición libre (CA1, CA2)**: `victoria.editor` → `/transport-units` → abrir la ficha de la unidad → pasar el mouse o tocar una rueda vacía.
   - El popover muestra "Montar neumático". Al tocarlo se abre el panel a la derecha, la ficha se corre a la izquierda y la rueda destino queda resaltada con anillo pulsante.
   - El foco queda en "Número de serie". Probar el filtro "Origen" (todos, en espera, en stock, por centro), la paginación y los estados de vacío.
   - Elegir con clic o con flechas + Enter: pasa a "Conductor". Buscar por apellido o DNI y elegir uno, o tocar "Omitir".
   - En "Confirmar" revisar el resumen, que los "Cambiar" vuelven al paso correcto conservando lo elegido, y que "Confirmar montaje" queda deshabilitado hasta marcar el checkbox.
   - Al confirmar: toast "Neumático montado", el panel se cierra y la rueda queda ocupada con el neumático en su popover.
   - En `/tires`, ese neumático figura "Montado"; si venía de un centro, el stock de ese centro bajó en uno (se ve en el origen del buscador).
2. **Editor, reemplazo (CA3)**: tocar la rueda ocupada → "Reemplazar neumático". El paso 1 avisa qué neumático sale. En el paso 3, el checkbox dice que se desmonta el saliente y el botón dice "Confirmar reemplazo". Al confirmar, el saliente aparece "En espera" en `/tires`.
3. **Teclado y accesibilidad**: Esc cierra el panel. Tab recorre pie y contenido. El indicador de pasos anuncia el paso actual. Un clic en el chasis no cierra el panel.
4. **Mobile (ancho < 768 px)**: el panel sale desde abajo (75 % de alto) y la cabecera y el pie quedan fijos.
5. **Administrador**: `victoria.administrador` / `Admin.1234` ve y puede ejecutar lo mismo.
6. **Lector (CA5, CA10)**: `victoria.lector` / `Lector.1234` → `/transport-units` → ficha. Los popovers muestran los datos de la posición pero **ninguna** acción "Montar"/"Reemplazar".
7. **Consola y red**: sin errores en consola. El PUT va a `/api/v1/unidades-transporte/{id}/slots/{codigo}/neumatico` y responde 200. El GET de neumáticos lleva `situacion=EN_STOCK&situacion=EN_ESPERA`, sin `[]`.

## Bloqueos

Ninguno.

Para tener en cuenta:
- El panel no modal se renderiza dentro del `DialogContent` (modal) de la ficha, como popup anidado de Base UI. Conviene confirmar en el navegador que el foco y los clics dentro del panel funcionan con la ficha abierta.
- Al cerrarse después de un éxito, el título del panel puede pasar un instante a "Reemplazar" mientras corre la animación, porque la ficha ya llegó actualizada.

verify.sh: [OK]

## Prueba en el stack real

2026-11-25, `docker compose up -d --build`, Claude in Chrome (sin capturas de pantalla: el renderer no respondió a `captureScreenshot`; se verificó por árbol de accesibilidad, DOM y base de datos). Datos: dos neumáticos pasados a `EN_STOCK` en "Deposito Santa Fe" por SQL.
- Editor, montaje (CA1, CA2, CA6): popover de E1-IZQ vacía → "Montar neumático"; drawer a la derecha y ficha corrida a la izquierda (sin solaparse), foco en la búsqueda; Origen lista centros con su stock y filtra por centro (2 resultados); clic avanza a Conductor; combobox busca en servidor; resumen correcto; "Confirmar montaje" deshabilitado hasta el checkbox. Toast "Neumático montado", slot ocupado. BD: `MONTADO`, sin centro, stock 2 → 1, evento `ASIGNACION` con unidad, posición, conductor y usuario. OK.
- Editor, reemplazo (CA3, CA7): popover ocupado → "Reemplazar neumático"; aviso del saliente en paso 1; el montado no figura como disponible; "Omitir" conductor; paso 3 muestra el saliente, checkbox específico y "Confirmar reemplazo". BD: saliente `EN_ESPERA`, eventos `DESASIGNACION` + `ASIGNACION`. OK.
- Lector (CA10): popovers sin acciones. OK.
- Consola sin errores. No verificado visualmente: anillo/pulso del slot y vista mobile.
