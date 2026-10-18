package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.event;

import java.time.Instant;

/**
 * Se registró un neumático nuevo (compra). Se publica dentro de la
 * transacción del alta y se entrega a los listeners recién después del commit.
 *
 * <p>Lleva los datos "tal como se veían" en el momento del hecho, para que
 * trazabilidad los guarde como snapshot sin consultar a otros módulos.
 *
 * @param neumaticoId     id interno del neumático.
 * @param numeroSerie     número de serie (puede ser null).
 * @param proveedorId     id interno del proveedor (null si no se indicó).
 * @param nombreProveedor nombre del proveedor en ese momento (null si no se indicó).
 * @param usuario         identificador público del usuario que registró la compra.
 * @param fecha           instante del alta.
 * @param comentario      comentario del alta (puede ser null).
 */
public record NeumaticoComprado(
        Long neumaticoId,
        String numeroSerie,
        Long proveedorId,
        String nombreProveedor,
        String usuario,
        Instant fecha,
        String comentario) {

}
