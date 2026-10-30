package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.Patentes;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una unidad de transporte. El frontend replica exactamente estas
 * reglas en {@code features/transport-units/schemas/transport-unit-schema.ts}:
 * si cambia una, cambian ambas.
 */
public record UnidadTransporteRequestDTO(
    // Se acepta con espacios, guiones y minúsculas; se guarda normalizada (ver Patentes).
    @NotBlank(message = "es obligatoria")
    @Size(max = 15, message = "no puede superar los 15 caracteres")
    String patente,

    @NotNull(message = "es obligatoria")
    Long configuracionId
) {

    @JsonIgnore
    @AssertTrue(message = "la patente debe tener el formato ABC123 o AB123CD")
    public boolean isPatenteValida() {
        // En blanco ya lo informa @NotBlank.
        return patente == null || patente.isBlank() || Patentes.esValida(patente);
    }

}
