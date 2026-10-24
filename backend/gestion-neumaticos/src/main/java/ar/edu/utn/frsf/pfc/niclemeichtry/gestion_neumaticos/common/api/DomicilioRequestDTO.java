package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un domicilio. La localidad se indica por nombre dentro de la
 * provincia: si no existe, se crea. El frontend replica estas reglas en
 * {@code features/suppliers/schemas/supplier-schema.ts}: si cambia una, cambian ambas.
 */
public record DomicilioRequestDTO(
    @NotBlank(message = "es obligatoria")
    @Size(max = MAX_CALLE, message = "no puede superar los 100 caracteres")
    String calle,

    @NotBlank(message = "es obligatoria")
    @Size(max = MAX_ALTURA, message = "no puede superar los 10 caracteres")
    String altura,

    @Size(max = MAX_PISO, message = "no puede superar los 10 caracteres")
    String piso,

    @Size(max = MAX_DEPARTAMENTO, message = "no puede superar los 10 caracteres")
    String departamento,

    @NotNull(message = "es obligatoria")
    Long provinciaId,

    @NotBlank(message = "es obligatoria")
    @Size(max = MAX_LOCALIDAD, message = "no puede superar los 100 caracteres")
    String localidad,

    // Opcional: vacío o en blanco = sin código postal.
    @Pattern(regexp = PATRON_CODIGO_POSTAL, message = "debe tener 4 dígitos o el formato CPA (ej. S3000ABC)")
    String codigoPostal
) {

    public static final int MAX_CALLE = 100;
    public static final int MAX_ALTURA = 10;
    public static final int MAX_PISO = 10;
    public static final int MAX_DEPARTAMENTO = 10;
    public static final int MAX_LOCALIDAD = 100;
    // 4 dígitos o CPA; sin distinguir mayúsculas (se guarda en mayúsculas) y
    // con los extremos recortados al guardar.
    public static final String PATRON_CODIGO_POSTAL = "^\\s*([0-9]{4}|[A-Za-z][0-9]{4}[A-Za-z]{3})?\\s*$";

}
