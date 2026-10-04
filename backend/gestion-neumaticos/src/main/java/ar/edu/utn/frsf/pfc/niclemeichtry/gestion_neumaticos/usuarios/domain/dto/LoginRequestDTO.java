package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.validation.IdentificadorValido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciales de inicio de sesión")
public record LoginRequestDTO(
    @Schema(description = "Correo electrónico o nombre de usuario", example = "administrador")
    @NotBlank(message = "El identificador es obligatorio")
    @Size(max = 254, message = "El identificador no puede superar los 254 caracteres")
    @IdentificadorValido
    String identifier,

    // @NotEmpty y no @NotBlank: los espacios son parte legítima de una contraseña.
    // Sin reglas de complejidad: eso corresponde al alta/cambio de contraseña.
    @Schema(description = "Contraseña", example = "Admin.1234", format = "password")
    @NotEmpty(message = "La contraseña es obligatoria")
    @Size(max = 128, message = "La contraseña no puede superar los 128 caracteres")
    String password
) {

    /** Nunca exponer la contraseña en logs. */
    @Override
    public String toString() {
        return "LoginRequestDTO[identifier=" + identifier + ", password=****]";
    }

}
