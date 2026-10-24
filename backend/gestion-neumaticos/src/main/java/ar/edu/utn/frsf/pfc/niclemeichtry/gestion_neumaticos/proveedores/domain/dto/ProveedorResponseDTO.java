package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;

public record ProveedorResponseDTO(
    UUID publicId,
    String nombre,
    String email,
    String telefono,
    DomicilioResponseDTO domicilio
) {

}
