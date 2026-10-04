package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	private static final String ARGON2 = "argon2";

	/** Parámetros mínimos de OWASP para Argon2id: m=19 MiB, t=2, p=1. */
	static final int ARGON2_MEMORIA_KIB = 19 * 1024;
	static final int ARGON2_ITERACIONES = 2;

	/**
	 * Cuánto espera un hash a que se libere un cupo antes de responder 503. Es
	 * corto a propósito: mientras espera retiene un hilo de Tomcat, y una espera
	 * larga dejaría a todo el resto de la API sin hilos ante una ráfaga de logins.
	 */
	static final Duration ESPERA_MAXIMA_HASH = Duration.ofMillis(500);

	/**
	 * Argon2id (recomendación actual de OWASP). Se envuelve en un
	 * {@link DelegatingPasswordEncoder}: cada hash se guarda con el prefijo
	 * {@code {argon2}} y sus parámetros, así que los hashes con parámetros
	 * anteriores siguen validando.
	 *
	 * <p>Cada hash pide 19 MiB y varios milisegundos de CPU: el encoder queda
	 * detrás de un límite de concurrencia (bulkhead) para que una ráfaga de
	 * logins no agote memoria ni hilos.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, ARGON2_MEMORIA_KIB, ARGON2_ITERACIONES);
		PasswordEncoder delegating = new DelegatingPasswordEncoder(ARGON2, Map.of(ARGON2, argon2));
		int concurrencia = Math.max(2, Runtime.getRuntime().availableProcessors());
		return new PasswordEncoderConcurrenciaLimitada(delegating, concurrencia, ESPERA_MAXIMA_HASH);
	}

}
