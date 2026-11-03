package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;

public record CentroAlmacenamientoResponseDTO(
    UUID publicId,
    String nombre,
    Integer cantidadNeumaticosEnStock,
    DomicilioResponseDTO domicilio
) {

}
