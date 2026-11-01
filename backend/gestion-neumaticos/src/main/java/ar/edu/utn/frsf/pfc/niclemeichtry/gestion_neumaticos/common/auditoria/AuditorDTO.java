package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.util.UUID;

/**
 * Usuario que firmó una operación. {@code nombreUsuario} y {@code nombreApellido}
 * son {@code null} si el usuario ya no se puede resolver.
 */
public record AuditorDTO(UUID publicId, String nombreUsuario, String nombreApellido) {

    public static AuditorDTO desconocido(UUID publicId) {
        return new AuditorDTO(publicId, null, null);
    }
}
