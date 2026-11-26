package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Montaje de un neumático en una posición (HU-31). {@code reemplazar} es la aceptación de la advertencia
 * de posición ocupada: sin ella, una posición con neumático se rechaza con 409.
 *
 * @param neumaticoPublicId neumático a montar (en stock o en espera).
 * @param conductorPublicId conductor asociado al evento, opcional.
 * @param reemplazar        si la posición está ocupada, desmontar el actual (null = false).
 */
public record AsignacionNeumaticoRequestDTO(
    @NotNull(message = "es obligatorio")
    UUID neumaticoPublicId,

    UUID conductorPublicId,

    Boolean reemplazar
) {

    /** Si se aceptó reemplazar el neumático montado. */
    public boolean aceptaReemplazo() {
        return Boolean.TRUE.equals(reemplazar);
    }

}
