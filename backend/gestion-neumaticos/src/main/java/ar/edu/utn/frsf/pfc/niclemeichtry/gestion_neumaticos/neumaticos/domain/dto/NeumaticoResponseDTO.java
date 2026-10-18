package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;

public record NeumaticoResponseDTO(
    UUID publicId,
    String numeroSerie,
    EstadoNeumatico estadoActual,
    Boolean enEspera,
    Float kilometrajeTotal,
    Float kilometrajeUltimaReparacion,
    TipoEjeUnidadTransporte tipoOrigen,
    TipoEjeUnidadTransporte tipoActual,
    Integer cantidadRecapados,
    SituacionOperativa situacionOperativaVigente,
    String comentario,
    String marcaNombre,
    // Datos si estuviera montado en una unidad de transporte (pueden ser null)
    String patenteUnidadTransporte,
    String posicionEjeUnidadTransporte,
    Integer nroEjeUnidadTransporte,
    String tipoEjeUnidadTransporte,
    // Datos si estuviera en un centro de almacenamiento o servicio de reparación (pueden ser null)
    String nombreCentroAlmacenamiento,
    String nombreServicioReparacion,
    String ubicacionActual
) {

}
