package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un centro de almacenamiento. La cantidad de neumáticos en stock no se
 * recibe: el alta siempre la deja en 0. El frontend replica exactamente estas reglas en
 * {@code features/storage-centers/schemas/storage-center-schema.ts}: si cambia una, cambian ambas.
 */
public record CentroAlmacenamientoRequestDTO(
    @NotBlank(message = "es obligatorio")
    @Size(max = MAX_NOMBRE, message = "no puede superar los 150 caracteres")
    String nombre,

    // Obligatorio, a diferencia de proveedores y servicios de reparación.
    @NotNull(message = "es obligatorio")
    @Valid
    DomicilioRequestDTO domicilio
) {

    private static final int MAX_NOMBRE = 150;

}
