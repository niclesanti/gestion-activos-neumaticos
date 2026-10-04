package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.limite;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.DemasiadosIntentosException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.LimitadorIntentosLogin;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import lombok.extern.slf4j.Slf4j;

/**
 * Token bucket (Bucket4j) por IP y por identificador, en memoria. Los buckets
 * viven en un caché Caffeine acotado: una entrada sin uso durante su ventana ya
 * se repuso por completo, así que se puede descartar sin perder información.
 *
 * <p>Es por instancia: con más de una réplica del backend hay que pasar a un
 * backend distribuido de Bucket4j (Redis/JDBC) o limitar en el proxy (ver
 * {@code docs/despliegue-seguro.md}).
 */
@Service
@EnableConfigurationProperties(LimiteLoginProperties.class)
@Slf4j
public class LimitadorIntentosLoginImpl implements LimitadorIntentosLogin {

	private final LimiteLoginProperties properties;
	private final TimeMeter reloj;
	private final Cache<String, Bucket> porIp;
	private final Cache<String, Bucket> porCuenta;

	@Autowired
	public LimitadorIntentosLoginImpl(LimiteLoginProperties properties) {
		this(properties, TimeMeter.SYSTEM_MILLISECONDS);
	}

	LimitadorIntentosLoginImpl(LimiteLoginProperties properties, TimeMeter reloj) {
		this.properties = properties;
		this.reloj = reloj;
		this.porIp = cache(properties.ventanaIp());
		this.porCuenta = cache(properties.ventanaCuenta());
	}

	private Cache<String, Bucket> cache(Duration ventana) {
		return Caffeine.newBuilder()
				.maximumSize(properties.maximoEntradas())
				.expireAfterAccess(ventana)
				.build();
	}

	@Override
	public void verificar(String ip, String identificador) {
		ConsumptionProbe intentoIp = bucketIp(ip).tryConsumeAndReturnRemaining(1);
		if (!intentoIp.isConsumed()) {
			log.warn("Login bloqueado por límite de IP: ip={}", ip);
			throw new DemasiadosIntentosException(Duration.ofNanos(intentoIp.getNanosToWaitForRefill()));
		}
		// El intento de la cuenta se consume acá, antes del hash, y no al confirmar
		// el fallo: si no, una ráfaga de requests concurrentes pasaría el chequeo
		// mientras las anteriores todavía están hasheando. Un login correcto
		// devuelve el cupo (registrarExito).
		ConsumptionProbe intentoCuenta = bucketCuenta(identificador).tryConsumeAndReturnRemaining(1);
		if (!intentoCuenta.isConsumed()) {
			log.warn("Login bloqueado por límite de fallos de la cuenta: ip={}, identificador={}", ip, identificador);
			throw new DemasiadosIntentosException(Duration.ofNanos(intentoCuenta.getNanosToWaitForRefill()));
		}
	}

	@Override
	public void registrarExito(String identificador) {
		porCuenta.invalidate(identificador);
	}

	private Bucket bucketIp(String ip) {
		return porIp.get(ip, clave -> bucket(properties.intentosPorIp(), properties.ventanaIp()));
	}

	private Bucket bucketCuenta(String identificador) {
		return porCuenta.get(identificador, clave -> bucket(properties.fallosPorCuenta(), properties.ventanaCuenta()));
	}

	/** Capacidad {@code cantidad}, que se repone de a poco a lo largo de {@code ventana}. */
	private Bucket bucket(int cantidad, Duration ventana) {
		return Bucket.builder()
				.addLimit(limite -> limite.capacity(cantidad).refillGreedy(cantidad, ventana))
				.withCustomTimePrecision(reloj)
				.build();
	}

}
