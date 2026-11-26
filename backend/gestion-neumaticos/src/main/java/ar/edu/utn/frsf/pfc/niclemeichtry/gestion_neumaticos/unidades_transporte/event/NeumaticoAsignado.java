package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event;

import java.time.Instant;

/**
 * Se montó un neumático en una posición de una unidad (HU-31). Se publica dentro de la transacción del
 * montaje y se entrega a los listeners recién después del commit.
 *
 * <p>Lleva los datos "tal como se veían" en el momento del hecho, para que trazabilidad los guarde como
 * snapshot sin consultar a otros módulos.
 *
 * @param neumaticoId     id interno del neumático.
 * @param numeroSerie     número de serie (puede ser null).
 * @param unidadId        id interno de la unidad.
 * @param patente         patente de la unidad.
 * @param slotId          id interno de la posición.
 * @param posicion        código de la posición (ej. E2-IZQ-EXT).
 * @param conductorId     id interno del conductor asociado (null si no se indicó).
 * @param nombreConductor nombre y apellido del conductor (null si no se indicó).
 * @param usuario         identificador público del usuario que hizo el montaje.
 * @param fecha           instante del montaje.
 */
public record NeumaticoAsignado(
        Long neumaticoId,
        String numeroSerie,
        Long unidadId,
        String patente,
        Long slotId,
        String posicion,
        Long conductorId,
        String nombreConductor,
        String usuario,
        Instant fecha) {

}
