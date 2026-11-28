# Implementación — HU-32 desasignar_neumatico_de_posicion

## Resumen

Backend (sin migración nueva; V20 alcanza):
- `exception/PosicionLibreException` (409) y su handler en `ControllerAdvisor`.
- `SlotUnidadTransporte.liberar()`.
- `UnidadTransporteRepository.findBloqueadaByPublicIdAndDeletedAtIsNull`: `@Lock(PESSIMISTIC_WRITE)` con `@Query` y sin entity graph (PostgreSQL no admite `FOR UPDATE` sobre el lado nullable de un outer join; los slots cargan lazy en la misma transacción).
- `UnidadTransporteService(Impl).desasignarNeumatico(publicId, codigo, usuario)`: unidad bloqueada, slot por código (sin distinguir mayúsculas), 404 si no existe/baja/código ajeno, 409 `PosicionLibreException` si está libre, `NeumaticoApi.desmontar`, `slot.liberar()`, firma la unidad (`registrarModificacion`), flush, publica `NeumaticoDesasignado` y devuelve la ficha. `DataIntegrityViolationException` pasa a 409.
- Refactor compartido con el reemplazo de HU-31: métodos privados `desmontarDelSlot` y `eventoDesasignacion`. `asignarNeumatico` conserva su contrato, orden de llamadas y eventos; sus tests no se tocaron.
- `UnidadTransporteController`: `DELETE /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico`, `hasRole('EDITOR')`, 200 con `UnidadTransporteDetalleDTO`.
- `docs/Seguridad/seguridad.md` 2.10: fila nueva.

Frontend:
- `lib/access.ts`: acción `transportUnits.unassignTire` (admin/editor).
- `api/transport-units-api.ts`: `unassignTire` (DELETE); `hooks/use-unassign-tire.ts` (guarda la ficha devuelta, invalida `["transport-units"]` salvo configuraciones, e invalida `["tires"]`).
- `components/slot-popover.tsx`: "Desmontar neumático" (outline) junto a "Reemplazar", solo con `onUnmount` y posición ocupada.
- `components/unassign-tire-dialog.tsx` (nuevo): `AlertDialog` con serie y posición, foco inicial en Cancelar, botón destructivo con spinner, sin cierre mientras corre. 409/404 refrescan la ficha, cierran y `notify.error`; 403 y otros `notify.error` sin cerrar.
- `transport-unit-detail-dialog.tsx`: estado y cableado; la posición se resalta mientras el diálogo está abierto.
- Textos `unmount.*` es/en (voseo en español) en `transport-units.json`.
- No se instaló ningún componente shadcn.

## Trazabilidad

- CA1: UI (navegador).
- CA2: `AsignacionNeumaticoIntegrationTests.desmonta`, `UnidadTransporteServiceImplTest.DesasignarNeumatico.desmonta`, `MontajeNeumaticoListenerTest` (desasignación, ya existente de HU-31).
- CA3 / CA7: `UnidadTransporteServiceImplTest.DesasignarNeumatico.posicionLibre`, `AsignacionNeumaticoIntegrationTests.desmontajeRechazado`, `UnidadTransporteControllerTest.DesasignarNeumatico.libre`, `ControllerAdvisorTest` (`PosicionLibreException`).
- CA4 / CA9 (403): `UnidadTransporteControllerTest.DesasignarNeumatico.lectorNoDesmonta` (y `sinToken`). UI: el Lector no ve la acción (navegador).
- CA5: `AsignacionNeumaticoIntegrationTests.desmonta` (en espera, sin slot ni centro, posición libre y re-montable), `UnidadTransporteControllerTest.DesasignarNeumatico.desmontaConEditor` / `desmontaConAdministrador` (REST Docs `unidad-transporte-desmontar-neumatico`).
- CA6: `AsignacionNeumaticoIntegrationTests.desmonta` (evento `DESASIGNACION` con serie, patente, posición y usuario; `updated_by` de la unidad).
- CA8: `UnidadTransporteServiceImplTest.DesasignarNeumatico.noEncontrados`, `AsignacionNeumaticoIntegrationTests.desmontajeRechazado`, `UnidadTransporteControllerTest.DesasignarNeumatico.noEncontrado`.
- Desmontaje doble (§4 del design): `AsignacionNeumaticoIntegrationTests.desmontajeDoble` (uno 200, otro 409, un solo evento) y `DesasignarNeumatico.carrera` (servicio).
- CA10 / CA11: UI (navegador).
- No regresión HU-31: `UnidadTransporteServiceImplTest.AsignarNeumatico.*`, `UnidadTransporteControllerTest.AsignarNeumatico.*` y `AsignacionNeumaticoIntegrationTests` (los 4 originales) siguen verdes sin cambios.

Autoverificación (solo puntual, sin `verify.sh` ni `verify` completo): `mvn test` de `UnidadTransporteServiceImplTest` (27), `UnidadTransporteControllerTest` (33), `ControllerAdvisorTest` (37), `AsignacionNeumaticoIntegrationTests` (7), `MontajeNeumaticoListenerTest`, `ModularityTests`, `AutorizacionBaseDatosTests`, `UnidadTransporteEntidadesTest`: todo verde. Frontend: `npm run lint` y `npm run typecheck` verdes (prettier aplicado a `features/transport-units`).

## Pasos para el navegador

Preparación: como `victoria.editor` / `Editor.1234`, tener una unidad (Configuración > Unidades) y un neumático "En espera" (los registrados en HU-11 lo están). Montarlo con HU-31: `/transport-units` > ficha > rueda vacía > "Montar neumático" > elegirlo > Omitir conductor > marcar el checkbox > "Confirmar montaje".

1. **Desmontar (CA1, CA2, CA10, CA11)**: tocar la rueda ocupada. El popover muestra "Reemplazar neumático" y "Desmontar neumático" (outline). Elegir "Desmontar": el popover se cierra, se abre "¿Desmontar el neumático?" con la serie y el código de posición, la rueda queda resaltada y el foco está en "Cancelar".
2. **Cancelar**: "Cancelar" (o Esc) cierra sin cambios; la rueda sigue ocupada.
3. **Confirmar**: "Desmontar" (botón rojo, spinner mientras corre). Toast "Neumático desmontado", el diálogo se cierra y la rueda queda vacía (contador de ocupadas −1). La red muestra `DELETE /api/v1/unidades-transporte/{id}/slots/{codigo}/neumatico` con 200.
4. **En espera (CA11)**: `/tires` > el neumático figura "En espera" (sin unidad ni posición). Se puede volver a montar desde la rueda vacía.
5. **Posición ya libre (CA3, 409)**: abrir la misma ficha en dos pestañas con la rueda ocupada; desmontar en la primera; en la segunda (ficha vieja) elegir "Desmontar" y confirmar. Esperado: toast "La posición ya estaba libre. Actualizamos la ficha." y la ficha se refresca.
6. **Lector (CA4, CA9)**: `victoria.lector` / `Lector.1234` > `/transport-units` > ficha: los popovers muestran los datos de las posiciones pero ninguna acción ("Montar", "Reemplazar" ni "Desmontar").
7. **Administrador**: `victoria.administrador` / `Admin.1234` ve y ejecuta lo mismo que el editor.
8. **Historial**: el evento de desasignación (unidad, posición, fecha, usuario) queda en `trazabilidad.eventos_neumatico` con `tipo = 'DESASIGNACION'` (la consulta desde la UI es HU-33).

Limpieza de datos de prueba: desmontar los neumáticos montados desde la UI (quedan "En espera") y, si hace falta borrar lo creado, por SQL como owner: `DELETE FROM trazabilidad.eventos_neumatico` (solo en dev), luego `UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id = NULL` y los neumáticos/unidades de prueba.

## Bloqueos

Ninguno.

Notas:
- No se corrió `./verify.sh` ni `verify` completo, por pedido del leader.
- La búsqueda lazy de slots tras el `findBloqueada...` evita el `FOR UPDATE` sobre outer join; es la razón de que el método del repositorio no use `@EntityGraph`.
- Contexto de seguridad en el test concurrente: se fija en cada hilo, porque `SecurityContextHolder` es thread-local.

## Verificación del leader (2026-11-28)

- **Backend:** `mvn -B clean verify` con JDK 21 (equivale a `./mvnw -B verify`; el wrapper no descarga Maven en esta máquina): `[OK]`, 854 tests, 0 fallas, cobertura JaCoCo cumplida.
- **Frontend (`./verify.sh --frontend`):** `[OK]` en coherencia de `feature_list.json`/specs, lint, typecheck, build y `npm audit`. Verificación verde.

## Prueba en el stack real

Stack reconstruido con `docker compose up -d --build`; Chrome (puppeteer-core) contra `http://localhost:3000`. Datos de prueba por API: unidad `HQ132AA` y neumáticos `T32-A` y `T32-B`, montados con la función de HU-31. **Todo borrado al terminar** (incluidos los eventos de trazabilidad y los publicados durante la prueba); los 2 neumáticos que quedan en la base son anteriores y no se tocaron.

| Caso | Resultado |
|---|---|
| Popover de una posición ocupada (editor) | Muestra serie, marca, estado y kilometraje, y los botones "Reemplazar neumático" y "Desmontar neumático" (CA10) |
| "Desmontar neumático" | Se abre la confirmación "¿Desmontar el neumático?" con la serie `T32-A` y la posición `E1-IZQ`; el foco inicial queda en "Cancelar" (CA1, CA10) |
| Cancelar | La confirmación se cierra, la posición sigue ocupada y el neumático sigue "Montado": no se ejecuta nada (CA10) |
| Confirmar "Desmontar" | Toast "Neumático desmontado"; la posición pasa a "Vacía" en el diagrama; por API la posición queda libre y el neumático `EN_ESPERA` (CA2, CA5, CA11) |
| Evento | `trazabilidad.eventos_neumatico` tiene `ASIGNACION` y `DESASIGNACION` con la patente `HQ132AA`; la unidad queda firmada por el editor en `updated_by` (CA2, CA6) |
| `/tires` | `T32-A` figura "En espera" (CA11) |
| Posición ya libre (ficha desactualizada) | Se desmonta por API la `E1-DER` con la ficha abierta, luego se intenta desde la pantalla: mensaje "La posición ya estaba libre. Actualizamos la ficha.", la confirmación se cierra y el diagrama se actualiza (CA3, CA7) |
| API: posición libre / código inexistente | 409 / 404 (CA7, CA8) |
| Lector | El popover muestra los datos de la posición **sin ningún botón**; `DELETE` por API responde 403 y la posición sigue ocupada (CA4, CA9) |
| Consola del navegador | sin errores |
