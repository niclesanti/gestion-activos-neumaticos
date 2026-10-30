package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import java.util.List;
import java.util.UUID;

public record UnidadTransporteResponseDTO(
    UUID publicId,
    String patente,
    ConfiguracionUnidadResumenDTO configuracion,
    List<SlotUnidadTransporteDTO> slots
) {

}
