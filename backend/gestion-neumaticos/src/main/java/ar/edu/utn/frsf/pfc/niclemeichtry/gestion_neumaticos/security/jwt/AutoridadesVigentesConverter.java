package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;

/**
 * Arma la autenticación de cada request con el nivel de acceso <b>vigente en la
 * base</b>, no con el claim del token: un cambio de rol o una baja rigen en el
 * request siguiente, y un claim {@code nivelAcceso} alterado no da permisos
 * (aun con la clave de firma filtrada). Un usuario que ya no existe responde 401.
 */
public class AutoridadesVigentesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	/** Lista blanca: un valor desconocido en la base nunca llega a ser authority. */
	static final Set<String> NIVELES = Set.of("ROLE_ADMINISTRADOR", "ROLE_EDITOR", "ROLE_LECTOR");

	private final NivelAccesoVigente nivelAccesoVigente;

	public AutoridadesVigentesConverter(NivelAccesoVigente nivelAccesoVigente) {
		this.nivelAccesoVigente = nivelAccesoVigente;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UUID publicId;
		try {
			publicId = UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidBearerTokenException("El token no identifica a un usuario válido");
		}
		String nivel = nivelAccesoVigente.buscar(publicId)
				.filter(NIVELES::contains)
				.orElseThrow(() -> new InvalidBearerTokenException("El usuario del token no está habilitado"));
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(nivel)), jwt.getSubject());
	}

}
