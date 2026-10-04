package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ServicioSaturadoException;

class PasswordEncoderConcurrenciaLimitadaTest {

	private final PasswordEncoder delegado = mock(PasswordEncoder.class);

	@Test
	void delegaEncodeMatchesYUpgrade() {
		PasswordEncoder encoder = new PasswordEncoderConcurrenciaLimitada(delegado, 1, Duration.ofSeconds(1));
		when(delegado.encode("clave")).thenReturn("hash");
		when(delegado.matches("clave", "hash")).thenReturn(true);
		when(delegado.upgradeEncoding("hash")).thenReturn(true);

		assertThat(encoder.encode("clave")).isEqualTo("hash");
		assertThat(encoder.matches("clave", "hash")).isTrue();
		assertThat(encoder.upgradeEncoding("hash")).isTrue();
	}

	@Test
	void sinCupoLibreRechazaCon503EnLugarDeEncolar() throws Exception {
		PasswordEncoder encoder = new PasswordEncoderConcurrenciaLimitada(delegado, 1, Duration.ofMillis(50));
		CountDownLatch hashEnCurso = new CountDownLatch(1);
		CountDownLatch liberar = new CountDownLatch(1);
		when(delegado.matches("lenta", "hash")).thenAnswer(invocacion -> {
			hashEnCurso.countDown();
			liberar.await(5, TimeUnit.SECONDS);
			return true;
		});

		CompletableFuture<Boolean> ocupado = CompletableFuture.supplyAsync(() -> encoder.matches("lenta", "hash"));
		assertThat(hashEnCurso.await(5, TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> encoder.matches("otra", "hash"))
				.isInstanceOf(ServicioSaturadoException.class)
				.satisfies(ex -> assertThat(((ServicioSaturadoException) ex).getReintentarEn())
						.isEqualTo(Duration.ofMillis(50)));

		liberar.countDown();
		assertThat(ocupado.get(5, TimeUnit.SECONDS)).isTrue();
		// Al terminar, el cupo vuelve a estar disponible.
		when(delegado.matches("otra", "hash")).thenReturn(false);
		assertThat(encoder.matches("otra", "hash")).isFalse();
	}

	@Test
	void unHiloInterrumpidoNoQuedaEsperandoUnCupo() {
		PasswordEncoder encoder = new PasswordEncoderConcurrenciaLimitada(delegado, 1, Duration.ofSeconds(5));

		Thread.currentThread().interrupt();
		try {
			assertThatThrownBy(() -> encoder.encode("clave")).isInstanceOf(ServicioSaturadoException.class);
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void elEncoderDeLaAppEsArgon2idConLosParametrosDeOwasp() {
		PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

		String hash = encoder.encode("Admin.1234");

		assertThat(hash).startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$");
		assertThat(encoder.matches("Admin.1234", hash)).isTrue();
		// Un hash con los parámetros anteriores (m=16 MiB, seed de dev) sigue validando.
		assertThat(encoder.matches("Admin.1234",
				"{argon2}$argon2id$v=19$m=16384,t=2,p=1$CAvkpR3H0coqJBq0zauNQQ$s9zySEsPZuOLLPIIyOD3f1CoR5EY+nEk30nm4NkF8xw"))
				.isTrue();
	}

}
