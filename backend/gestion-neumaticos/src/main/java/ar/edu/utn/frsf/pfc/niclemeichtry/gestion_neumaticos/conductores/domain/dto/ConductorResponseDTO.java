package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto;

import java.util.UUID;

public record ConductorResponseDTO(
    UUID publicId,
    String dni,
    String nombre,
    String apellido
) {

}
