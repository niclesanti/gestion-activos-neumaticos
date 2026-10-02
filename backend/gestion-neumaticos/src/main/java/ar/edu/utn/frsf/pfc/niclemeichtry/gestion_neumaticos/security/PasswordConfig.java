package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	private static final String ARGON2 = "argon2";

	/**
	 * Argon2id (recomendación actual de OWASP). Se envuelve en un
	 * {@link DelegatingPasswordEncoder}: cada hash se guarda con el prefijo
	 * {@code {argon2}}, lo que permite cambiar de algoritmo o de parámetros más
	 * adelante sin invalidar las contraseñas ya almacenadas.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder(ARGON2,
				Map.of(ARGON2, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
	}

}
