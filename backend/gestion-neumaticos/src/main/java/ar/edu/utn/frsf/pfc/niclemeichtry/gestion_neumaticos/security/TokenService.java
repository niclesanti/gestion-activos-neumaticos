package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * API pública del módulo de seguridad para emitir y revocar los JWT de sesión.
 */
public interface TokenService {

	/** Claim con el nivel de acceso del usuario (ej. {@code ROLE_ADMINISTRADOR}). */
	String CLAIM_NIVEL_ACCESO = "nivelAcceso";

	/**
	 * Emite un token para el usuario. El {@code sub} es su identificador público:
	 * el id interno nunca sale del módulo de usuarios.
	 */
	TokenEmitido generar(UUID publicId, String nivelAcceso);

	/** Invalida el token hasta su expiración natural (cierre de sesión). */
	void revocar(Jwt jwt);

}
