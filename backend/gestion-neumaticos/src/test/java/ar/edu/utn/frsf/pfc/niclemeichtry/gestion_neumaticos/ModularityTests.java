package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifica los límites entre módulos (sin ciclos ni acceso a paquetes internos). */
class ModularityTests {

	@Test
	void verificaLaEstructuraModular() {
		ApplicationModules.of(GestionNeumaticosApplication.class).verify();
	}

}
