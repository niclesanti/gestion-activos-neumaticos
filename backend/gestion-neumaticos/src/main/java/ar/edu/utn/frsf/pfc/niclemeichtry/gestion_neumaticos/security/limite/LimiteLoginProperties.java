package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.limite;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param intentosPorIp     intentos de login (exitosos o no) por IP en {@code ventanaIp}.
 * @param ventanaIp         período en que se reponen los intentos por IP.
 * @param fallosPorCuenta   fallos tolerados por identificador en {@code ventanaCuenta}.
 * @param ventanaCuenta     período en que se reponen los fallos por identificador.
 * @param maximoEntradas    tope de IPs/identificadores recordados (acota la memoria
 *                          ante identificadores aleatorios).
 */
@ConfigurationProperties("app.security.login")
public record LimiteLoginProperties(
		@DefaultValue("10") int intentosPorIp,
		@DefaultValue("PT1M") Duration ventanaIp,
		@DefaultValue("5") int fallosPorCuenta,
		@DefaultValue("PT15M") Duration ventanaCuenta,
		@DefaultValue("100000") long maximoEntradas) {
}
