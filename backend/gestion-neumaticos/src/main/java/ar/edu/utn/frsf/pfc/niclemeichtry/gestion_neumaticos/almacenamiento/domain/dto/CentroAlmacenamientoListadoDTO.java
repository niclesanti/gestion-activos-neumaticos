package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto;

import java.util.UUID;

/** Centro activo para elegir el origen de un neumático (sin domicilio). */
public record CentroAlmacenamientoListadoDTO(
    UUID publicId,
    String nombre,
    Integer cantidadNeumaticosEnStock
) {

}
