package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;

public record ServicioReparacionResponseDTO(
    UUID publicId,
    String nombre,
    String email,
    String telefono,
    Integer cantidadNeumaticosEnReparacion,
    DomicilioResponseDTO domicilio
) {

}
