package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import java.util.List;
import java.util.UUID;

/** Ficha de una unidad: configuración completa y posiciones con su neumático montado. */
public record UnidadTransporteDetalleDTO(
    UUID publicId,
    String patente,
    ConfiguracionUnidadDTO configuracion,
    List<SlotDetalleDTO> slots
) {

}
