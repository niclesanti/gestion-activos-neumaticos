package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.time.Clock;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	/** HS256 exige una clave de al menos 256 bits. */
	static final int LONGITUD_MINIMA_SECRETO = 32;

	static final String ERROR_SECRETO = "app.security.jwt.secret (JWT_SECRET) debe ser base64 de al menos "
			+ LONGITUD_MINIMA_SECRETO + " bytes aleatorios (generarlo con: openssl rand -base64 32)";

	/**
	 * El secreto se recibe en base64 y se decodifica: así es material aleatorio y
	 * no una frase que se pueda adivinar offline a partir de un token.
	 */
	@Bean
	public SecretKey jwtSecretKey(JwtProperties properties) {
		String secret = properties.secret();
		byte[] clave;
		try {
			clave = Base64.getDecoder().decode(secret == null ? "" : secret.strip());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException(ERROR_SECRETO);
		}
		if (clave.length < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException(ERROR_SECRETO);
		}
		return new SecretKeySpec(clave, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties,
			TokenNoRevocadoValidator tokenNoRevocadoValidator) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(properties.issuer()),
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						aud -> aud != null && aud.contains(properties.audience())),
				tokenNoRevocadoValidator));
		return decoder;
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
