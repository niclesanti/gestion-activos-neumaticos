package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import java.util.List;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoUnidad;

/** Configuración seleccionable en el alta, con sus ejes en orden (para la vista previa). */
public record ConfiguracionUnidadDTO(
    Long id,
    String nombre,
    TipoUnidad tipoUnidad,
    Integer cantSlotsAuxilio,
    Integer totalEjes,
    Integer totalSlots,
    List<EjeConfiguracionDTO> ejes
) {

}
