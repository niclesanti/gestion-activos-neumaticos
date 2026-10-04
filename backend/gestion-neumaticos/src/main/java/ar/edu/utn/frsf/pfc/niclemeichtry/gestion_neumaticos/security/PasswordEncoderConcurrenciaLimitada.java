package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ServicioSaturadoException;

/**
 * Bulkhead: como mucho {@code concurrencia} hashes a la vez. Si no se libera un
 * cupo en {@code esperaMaxima}, se rechaza con {@link ServicioSaturadoException}
 * (503) en lugar de acumular hilos esperando.
 */
class PasswordEncoderConcurrenciaLimitada implements PasswordEncoder {

	private final PasswordEncoder delegado;
	private final Semaphore cupos;
	private final Duration esperaMaxima;

	PasswordEncoderConcurrenciaLimitada(PasswordEncoder delegado, int concurrencia, Duration esperaMaxima) {
		this.delegado = delegado;
		this.cupos = new Semaphore(concurrencia, true);
		this.esperaMaxima = esperaMaxima;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		return conCupo(() -> delegado.encode(rawPassword));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return conCupo(() -> delegado.matches(rawPassword, encodedPassword));
	}

	@Override
	public boolean upgradeEncoding(String encodedPassword) {
		return delegado.upgradeEncoding(encodedPassword);
	}

	private <T> T conCupo(Supplier<T> operacion) {
		boolean adquirido;
		try {
			adquirido = cupos.tryAcquire(esperaMaxima.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ServicioSaturadoException(esperaMaxima);
		}
		if (!adquirido) {
			throw new ServicioSaturadoException(esperaMaxima);
		}
		try {
			return operacion.get();
		} finally {
			cupos.release();
		}
	}

}
