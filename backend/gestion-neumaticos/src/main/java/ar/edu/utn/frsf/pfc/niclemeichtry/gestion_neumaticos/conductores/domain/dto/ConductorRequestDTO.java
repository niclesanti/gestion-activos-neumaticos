package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un conductor. El frontend replica exactamente estas reglas en
 * {@code features/drivers/schemas/driver-schema.ts}: si cambia una, cambian ambas.
 */
public record ConductorRequestDTO(
    // Opcional: vacío o en blanco = sin DNI.
    @Pattern(regexp = PATRON_DNI, message = "debe tener 7 u 8 dígitos, sin puntos")
    String dni,

    @NotBlank(message = "es obligatorio")
    @Size(max = MAX_NOMBRE, message = "no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "es obligatorio")
    @Size(max = MAX_APELLIDO, message = "no puede superar los 100 caracteres")
    String apellido
) {

    // Los espacios de los extremos se recortan al guardar.
    public static final String PATRON_DNI = "^\\s*([0-9]{7,8})?\\s*$";
    public static final int MAX_NOMBRE = 100;
    public static final int MAX_APELLIDO = 100;

}
