package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event;

import java.time.Instant;

/**
 * Se desmontó un neumático de una posición de una unidad: hoy, al reemplazarlo por otro (HU-31, CA3). Se
 * publica dentro de la transacción y se entrega a los listeners después del commit, con snapshots.
 *
 * @param neumaticoId id interno del neumático.
 * @param numeroSerie número de serie (puede ser null).
 * @param unidadId    id interno de la unidad.
 * @param patente     patente de la unidad.
 * @param slotId      id interno de la posición.
 * @param posicion    código de la posición (ej. E2-IZQ-EXT).
 * @param usuario     identificador público del usuario que hizo el desmontaje.
 * @param fecha       instante del desmontaje.
 */
public record NeumaticoDesasignado(
        Long neumaticoId,
        String numeroSerie,
        Long unidadId,
        String patente,
        Long slotId,
        String posicion,
        String usuario,
        Instant fecha) {

}
