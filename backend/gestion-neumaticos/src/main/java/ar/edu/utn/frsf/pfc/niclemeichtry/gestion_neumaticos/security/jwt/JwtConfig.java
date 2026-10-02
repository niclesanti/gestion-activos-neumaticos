package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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

	@Bean
	public SecretKey jwtSecretKey(JwtProperties properties) {
		String secret = properties.secret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException(
					"app.security.jwt.secret (JWT_SECRET) debe tener al menos " + LONGITUD_MINIMA_SECRETO + " bytes");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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
				tokenNoRevocadoValidator));
		return decoder;
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
