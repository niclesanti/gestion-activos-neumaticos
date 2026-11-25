package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;

/** Ficha de un neumático: datos técnicos, situación vigente y su ubicación. Sin ids internos. */
public record NeumaticoDetalleDTO(
    UUID publicId,
    String numeroSerie,
    String marca,
    EstadoNeumatico estadoActual,
    SituacionOperativa situacionOperativaVigente,
    Float kilometrajeTotal,
    Float kilometrajeUltimaReparacion,
    TipoEjeUnidadTransporte tipoOrigen,
    TipoEjeUnidadTransporte tipoActual,
    Integer cantidadRecapados,
    String comentario,
    UbicacionOperativaDTO ubicacion
) {

}
