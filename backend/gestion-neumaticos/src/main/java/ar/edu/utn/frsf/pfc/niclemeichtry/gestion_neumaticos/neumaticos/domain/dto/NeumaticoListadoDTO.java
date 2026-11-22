package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;

/** Fila del listado de neumáticos: solo lo que muestra la tabla. */
public record NeumaticoListadoDTO(
    UUID publicId,
    String numeroSerie,
    String marca,
    EstadoNeumatico estadoActual,
    SituacionOperativa situacionOperativaVigente,
    Float kilometrajeTotal
) {

}
