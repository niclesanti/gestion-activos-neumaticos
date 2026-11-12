package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.UUID;

/** Datos principales de un neumático montado, para mostrar en otros módulos. */
public record NeumaticoMontadoDTO(
    UUID publicId,
    String numeroSerie,
    String marca,
    String estado,
    Float kilometrajeTotal
) {

}
