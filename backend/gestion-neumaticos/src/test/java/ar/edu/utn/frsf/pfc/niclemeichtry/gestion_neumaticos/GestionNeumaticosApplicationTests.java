package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.security.jwt.secret=secreto-de-prueba-de-al-menos-32-bytes-para-hs256")
class GestionNeumaticosApplicationTests {

	@Test
	void contextLoads() {
	}

}
