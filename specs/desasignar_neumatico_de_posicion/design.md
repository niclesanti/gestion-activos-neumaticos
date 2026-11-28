# Design — HU-32 desasignar_neumatico_de_posicion

## 1. Contrato

**`DELETE /api/v1/unidades-transporte/{publicId}/slots/{codigo}/neumatico`** — `EDITOR`. Sin body. 200 `UnidadTransporteDetalleDTO` actualizado. Errores: 403 Lector; 404 unidad inexistente o de baja, o `codigo` ajeno a la unidad; 409 `PosicionLibreException` (nueva) si la posición no tiene neumático (conflicto de estado, como `PosicionOcupadaException`; no 422).

`UnidadTransporteService.desasignarNeumatico(publicId, codigo, usuario)`, una transacción: unidad + slot (con bloqueo de escritura sobre la fila de la unidad, ver §4) → si libre, 409 → `NeumaticoApi.desmontar(id)` (ya deja `EN_ESPERA`, `enEspera=true`, sin `slotMontadoId`; centro y servicio ya son nulos desde el montaje; hace flush) → slot libre (`neumatico_actual_id = NULL`) y `updated_*` de la unidad → publica `NeumaticoDesasignado` (mismo evento de HU-31; el listener y `EventoDesasignacion` ya guardan unidad, patente, slot, posición, fecha y usuario) → devuelve la ficha. `DataIntegrityViolationException` → 409.

Refactor sin cambiar el contrato de HU-31: `asignarNeumatico` y `desasignarNeumatico` comparten un método privado que desmonta el neumático del slot y arma el evento `NeumaticoDesasignado`; el reemplazo sigue publicando sus dos eventos como hoy.

**Frontend**: en el popover de una posición ocupada, con `canPerform('transportUnits.unassignTire')` (admin/editor), junto a "Reemplazar" aparece "Desmontar neumático" (outline). Al elegirla se cierra el popover y se abre un `AlertDialog`: "¿Desmontar el neumático?", con serie y código de posición ("queda en espera; el desmontaje se registra en el historial"), Cancelar / "Desmontar" (destructivo, deshabilitado con estado de carga mientras corre). Éxito: guarda la ficha devuelta (como `use-assign-tire`), invalida `["transport-units"]` y `["tires"]`, `notify.success`, cierra. 409 (ya libre) → refetch de la ficha, cierra y `notify.error`; 404 → `notify.error` y refetch; 403/otros → `notify.error` genérico. Foco inicial en Cancelar. La posición se resalta como en HU-31 mientras el diálogo está abierto. Lector: sin acciones.

Reutiliza `alert-dialog`, `popover`, `button`, `toast`. Nada que instalar.

## 2. Archivos

Backend (`.../gestion_neumaticos/`):
- `unidades_transporte/domain/entity/SlotUnidadTransporte.java` — liberar la posición.
- `unidades_transporte/{controller,service,repository}/**` — endpoint `DELETE`, `desasignarNeumatico`, método compartido con el reemplazo, bloqueo de la unidad.
- `exception/{PosicionLibreException,ControllerAdvisor}.java` — 409.

Frontend (`src/`):
- `lib/access.ts` — acción `transportUnits.unassignTire`.
- `features/transport-units/api/*` y `hooks/use-unassign-tire.ts` — request y mutación.
- `features/transport-units/components/{slot-popover,transport-unit-detail-dialog,unassign-tire-dialog}.tsx` — acción, confirmación y estado.
- `lib/i18n/locales/{es,en}/transport-units.json` — `unmount.*`.

Docs: `docs/Seguridad/seguridad.md` 2.10 (fila del `DELETE`).

## 3. Migración

Ninguna: `V20` ya da `GRANT UPDATE (neumatico_actual_id)` en slots con política `UPDATE` (admin/editor), `UPDATE (updated_at, updated_by)` firmado en la unidad y las columnas/CHECK del evento de desasignación. Poner el id en NULL entra en esos privilegios; el Lector sigue sin `UPDATE`.

## 4. Desvíos de convención

- `DELETE` devuelve 200 con la ficha (no 204): el frontend refresca el diagrama con la respuesta, como el `PUT` de HU-31.
- Slot por `codigo` en la URL (como HU-31).
- Bloqueo `PESSIMISTIC_WRITE` de la unidad al desmontar: dos desmontajes simultáneos no chocan con ningún índice único (el UPDATE del neumático es idempotente) y duplicarían el evento; con el bloqueo el segundo ve la posición libre y recibe 409. Montar vs desmontar ya lo cubren los índices únicos de V20.

## 5. Alternativas descartadas

- **`POST .../desmontar`**: verbo en la URL, contra REST.
- **204 sin cuerpo**: obligaría a un GET extra para refrescar la ficha.
- **Acción de rotación dedicada**: ya cubierta por desmontar + montar.
