package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.EXPIRACION;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.SECRETO_JWT;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.TOKEN;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtProperties;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El token emitido y el secreto JWT nunca deben aparecer en un log. */
class ToStringSinSecretosTest {

	@Test
	void tokenEmitidoOcultaElToken() {
		assertThat(new TokenEmitido(TOKEN, EXPIRACION).toString())
				.contains(EXPIRACION.toString())
				.doesNotContain(TOKEN);
	}

	@Test
	void jwtPropertiesOcultaElSecreto() {
		assertThat(jwtProperties().toString())
				.contains("PT8H")
				.doesNotContain(SECRETO_JWT);
	}

}
