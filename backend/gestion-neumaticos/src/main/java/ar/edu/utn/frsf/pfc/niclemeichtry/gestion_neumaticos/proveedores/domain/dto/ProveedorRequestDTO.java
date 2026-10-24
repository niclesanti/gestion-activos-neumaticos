package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un proveedor. El frontend replica exactamente estas reglas en
 * {@code features/suppliers/schemas/supplier-schema.ts}: si cambia una, cambian ambas.
 */
public record ProveedorRequestDTO(
    @NotBlank(message = "es obligatorio")
    @Size(max = MAX_NOMBRE, message = "no puede superar los 150 caracteres")
    String nombre,

    // Opcional: vacío o en blanco = sin email.
    @Pattern(regexp = PATRON_EMAIL, message = "no es un email válido")
    @Size(max = MAX_EMAIL, message = "no puede superar los 254 caracteres")
    String email,

    // Opcional: vacío o en blanco = sin teléfono.
    @Pattern(regexp = PATRON_TELEFONO, message = "solo admite dígitos, espacios, +, -, ( y ), entre 6 y 30 caracteres")
    String telefono,

    // Opcional: sin domicilio = null.
    @Valid
    DomicilioRequestDTO domicilio
) {

    public static final int MAX_NOMBRE = 150;
    public static final int MAX_EMAIL = 254;
    // Los espacios de los extremos se recortan al guardar.
    public static final String PATRON_EMAIL = "^\\s*([^@\\s]+@[^@\\s]+\\.[^@\\s]+)?\\s*$";
    public static final String PATRON_TELEFONO = "^\\s*(\\+?[0-9(][0-9 ()-]{4,27}[0-9])?\\s*$";

}
