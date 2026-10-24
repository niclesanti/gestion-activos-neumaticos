package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api;

/** Domicilio con su localidad y provincia, sin el id interno. */
public record DomicilioResponseDTO(
    String calle,
    String altura,
    String piso,
    String departamento,
    String localidad,
    String codigoPostal,
    Long provinciaId,
    String provincia
) {

}
