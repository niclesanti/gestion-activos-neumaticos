package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto;

import java.util.UUID;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos del usuario autenticado")
public record UsuarioSesionDTO(
    @Schema(description = "Identificador público del usuario")
    UUID publicId,
    @Schema(example = "administrador")
    String nombreUsuario,
    @Schema(example = "Ana Administradora")
    String nombreApellido,
    @Schema(example = "administrador@neumaticos.local")
    String email,
    @Schema(description = "Nivel de acceso", example = "ROLE_ADMINISTRADOR")
    NivelAcceso nivelAcceso
) {

}
