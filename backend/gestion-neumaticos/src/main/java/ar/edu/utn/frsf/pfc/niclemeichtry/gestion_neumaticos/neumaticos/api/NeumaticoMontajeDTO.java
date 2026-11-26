package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.UUID;

/**
 * Neumático que se acaba de montar o desmontar: el {@code id} interno (lo guarda el slot y lo usa el
 * evento de trazabilidad) y el número de serie para el snapshot. El {@code id} nunca sale por la API REST.
 */
public record NeumaticoMontajeDTO(Long id, UUID publicId, String numeroSerie) {

}
