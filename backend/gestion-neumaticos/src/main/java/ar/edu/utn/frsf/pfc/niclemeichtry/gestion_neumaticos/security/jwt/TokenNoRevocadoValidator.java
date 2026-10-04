package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocadoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Rechaza los tokens cerrados con logout: un JWT es stateless, así que la única
 * forma de invalidarlo antes de su expiración es recordar su {@code jti}.
 */
@Component
@RequiredArgsConstructor
public class TokenNoRevocadoValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error TOKEN_INVALIDO = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN, "El token fue revocado o no es válido", null);

	private final TokenRevocadoRepository tokenRevocadoRepository;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		UUID jti;
		try {
			jti = UUID.fromString(jwt.getId());
		} catch (IllegalArgumentException | NullPointerException ex) {
			return OAuth2TokenValidatorResult.failure(TOKEN_INVALIDO);
		}
		return tokenRevocadoRepository.existsById(jti)
				? OAuth2TokenValidatorResult.failure(TOKEN_INVALIDO)
				: OAuth2TokenValidatorResult.success();
	}

}
