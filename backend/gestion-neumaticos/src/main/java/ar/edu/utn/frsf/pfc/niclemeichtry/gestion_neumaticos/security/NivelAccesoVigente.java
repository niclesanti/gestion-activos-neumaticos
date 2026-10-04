package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto que el módulo de usuarios implementa: el nivel de acceso actual del
 * usuario según la base. El de seguridad no conoce a los usuarios, pero
 * necesita el rol vigente en cada request (y no el que traía el token al
 * emitirse) para que un cambio de rol o una baja rijan de inmediato.
 */
public interface NivelAccesoVigente {

	/**
	 * @return el nivel de acceso (ej. {@code ROLE_EDITOR}), o vacío si el usuario
	 *         ya no existe o no puede iniciar sesión.
	 */
	Optional<String> buscar(UUID publicId);

}
