package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import java.util.UUID;

public record UnidadTransporteListadoDTO(UUID publicId, String patente, ConfiguracionUnidadListadoDTO configuracion) {

}
