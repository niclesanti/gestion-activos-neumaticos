package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sesión iniciada: token de acceso y datos del usuario")
public record LoginResponseDTO(
    @Schema(description = "JWT a enviar en el header Authorization")
    String token,
    @Schema(example = "Bearer")
    String tipo,
    @Schema(description = "Instante de expiración del token (UTC)")
    Instant expiraEn,
    UsuarioSesionDTO usuario
) {

    public static final String TIPO_BEARER = "Bearer";

    @Override
    public String toString() {
        return "LoginResponseDTO[tipo=" + tipo + ", expiraEn=" + expiraEn + ", usuario=" + usuario + "]";
    }

}
