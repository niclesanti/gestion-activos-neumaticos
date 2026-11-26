package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto;

import java.util.UUID;

/** Fila del listado de conductores activos (buscador del montaje de neumáticos). */
public record ConductorListadoDTO(
    UUID publicId,
    String nombre,
    String apellido,
    String dni
) {

}
