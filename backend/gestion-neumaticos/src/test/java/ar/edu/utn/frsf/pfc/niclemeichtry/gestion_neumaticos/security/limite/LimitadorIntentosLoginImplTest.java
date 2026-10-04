package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.limite;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.limiteLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.DemasiadosIntentosException;
import io.github.bucket4j.TimeMeter;

/** Con un reloj manual: la reposición de intentos se prueba sin esperas reales. */
class LimitadorIntentosLoginImplTest {

	private static final String IP = "203.0.113.7";
	private static final String OTRA_IP = "198.51.100.20";

	/** Nanosegundos del reloj manual. */
	private final AtomicLong ahora = new AtomicLong();

	private final TimeMeter reloj = new TimeMeter() {
		@Override
		public long currentTimeNanos() {
			return ahora.get();
		}

		@Override
		public boolean isWallClockBased() {
			return false;
		}
	};

	/** 3 intentos por IP por minuto, 2 fallos por cuenta cada 10 minutos. */
	private final LimitadorIntentosLoginImpl limitador = new LimitadorIntentosLoginImpl(limiteLogin(), reloj);

	private void avanzar(Duration duracion) {
		ahora.addAndGet(duracion.toNanos());
	}

	@Test
	void limitaLosIntentosPorIpAunqueSeanACuentasDistintas() {
		limitador.verificar(IP, "uno");
		limitador.verificar(IP, "dos");
		limitador.verificar(IP, "tres");

		assertThatThrownBy(() -> limitador.verificar(IP, "cuatro"))
				.isInstanceOf(DemasiadosIntentosException.class)
				.satisfies(ex -> assertThat(((DemasiadosIntentosException) ex).getReintentarEn())
						.isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1)));
		// Otra IP no se ve afectada.
		assertThatCode(() -> limitador.verificar(OTRA_IP, "cuatro")).doesNotThrowAnyException();
	}

	@Test
	void losIntentosPorIpSeReponenConElTiempo() {
		limitador.verificar(IP, "a");
		limitador.verificar(IP, "b");
		limitador.verificar(IP, "c");

		avanzar(Duration.ofMinutes(1));

		assertThatCode(() -> limitador.verificar(IP, "d")).doesNotThrowAnyException();
	}

	@Test
	void bloqueaLaCuentaDesdeCualquierIp() {
		limitador.verificar(IP, "victima");
		limitador.verificar(OTRA_IP, "victima");

		assertThatThrownBy(() -> limitador.verificar("192.0.2.1", "victima"))
				.isInstanceOf(DemasiadosIntentosException.class)
				.hasMessage(DemasiadosIntentosException.MENSAJE);
	}

	@Test
	void consumeElIntentoDeLaCuentaAntesDeVerificarLaClave() {
		// Intentos en vuelo (todavía sin resultado) ya cuentan: una ráfaga
		// concurrente no puede superar el límite mientras se calculan los hashes.
		limitador.verificar(IP, "victima");
		limitador.verificar(OTRA_IP, "victima");

		assertThatThrownBy(() -> limitador.verificar("192.0.2.1", "victima"))
				.isInstanceOf(DemasiadosIntentosException.class);
	}

	@Test
	void laCuentaSeDesbloqueaAlReponerse() {
		limitador.verificar(IP, "victima");
		limitador.verificar(OTRA_IP, "victima");

		avanzar(Duration.ofMinutes(10));

		assertThatCode(() -> limitador.verificar("192.0.2.1", "victima")).doesNotThrowAnyException();
	}

	@Test
	void unLoginExitosoDevuelveLosIntentosDeLaCuenta() {
		limitador.verificar(IP, "usuario");
		limitador.verificar(OTRA_IP, "usuario");
		limitador.registrarExito("usuario");

		assertThatCode(() -> limitador.verificar("192.0.2.1", "usuario")).doesNotThrowAnyException();
	}

	@Test
	void elConstructorPublicoUsaElRelojDelSistema() {
		assertThatCode(() -> new LimitadorIntentosLoginImpl(limiteLogin()).verificar(IP, "x"))
				.doesNotThrowAnyException();
	}

}
