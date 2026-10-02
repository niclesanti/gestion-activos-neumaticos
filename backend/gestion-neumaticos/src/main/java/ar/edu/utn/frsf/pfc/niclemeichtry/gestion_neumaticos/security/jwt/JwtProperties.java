package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret     clave HMAC para firmar los tokens (mínimo 32 bytes, HS256).
 * @param expiracion vigencia del token; por defecto un turno de trabajo.
 * @param issuer     emisor que se firma en el token y se exige al validarlo.
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
		String secret,
		@DefaultValue("PT8H") Duration expiracion,
		@DefaultValue("gestion-neumaticos") String issuer) {

	@Override
	public String toString() {
		return "JwtProperties[expiracion=" + expiracion + ", issuer=" + issuer + "]";
	}

}
