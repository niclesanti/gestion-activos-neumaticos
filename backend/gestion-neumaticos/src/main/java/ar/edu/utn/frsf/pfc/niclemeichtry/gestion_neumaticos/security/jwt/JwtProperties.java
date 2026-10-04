package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret     clave HMAC para firmar los tokens, en base64 (mínimo 32 bytes
 *                   decodificados, HS256). Generarla con {@code openssl rand -base64 32}.
 * @param expiracion vigencia del token.
 * @param issuer     emisor que se firma en el token y se exige al validarlo.
 * @param audience   destinatario ({@code aud}) que se firma y se exige al validar.
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
		String secret,
		@DefaultValue("PT2H") Duration expiracion,
		@DefaultValue("gestion-neumaticos") String issuer,
		@DefaultValue("gestion-neumaticos-api") String audience) {

	@Override
	public String toString() {
		return "JwtProperties[expiracion=" + expiracion + ", issuer=" + issuer + ", audience=" + audience + "]";
	}

}
