package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

class ComoSistemaTest {

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void ejecutaSinUsuarioYRestauraElContexto() {
		Authentication editor = autenticacion(UUID.randomUUID(), NivelAcceso.ROLE_EDITOR);
		SecurityContextHolder.getContext().setAuthentication(editor);
		Authentication[] durante = new Authentication[1];

		ComoSistema.ejecutar(() -> durante[0] = SecurityContextHolder.getContext().getAuthentication());

		assertThat(durante[0]).isNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(editor);
	}

	@Test
	void restauraElContextoAunqueLaAccionFalle() {
		Authentication editor = autenticacion(UUID.randomUUID(), NivelAcceso.ROLE_EDITOR);
		SecurityContextHolder.getContext().setAuthentication(editor);

		assertThatThrownBy(() -> ComoSistema.ejecutar(() -> {
			throw new IllegalStateException("falla");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(editor);
	}

}
