package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwt;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

@ExtendWith(MockitoExtension.class)
class AutoridadesVigentesConverterTest {

	@Mock
	private NivelAccesoVigente nivelAccesoVigente;

	private AutoridadesVigentesConverter converter() {
		return new AutoridadesVigentesConverter(nivelAccesoVigente);
	}

	@Test
	void usaElNivelVigenteEnLaBaseYNoElDelToken() {
		UUID publicId = UUID.randomUUID();
		// El token dice administrador, pero en la base ya es lector.
		when(nivelAccesoVigente.buscar(publicId)).thenReturn(Optional.of("ROLE_LECTOR"));

		AbstractAuthenticationToken autenticacion = converter().convert(jwt(publicId, NivelAcceso.ROLE_ADMINISTRADOR));

		assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_LECTOR");
		assertThat(autenticacion.getName()).isEqualTo(publicId.toString());
	}

	@Test
	void unUsuarioQueYaNoExisteNoSeAutentica() {
		UUID publicId = UUID.randomUUID();
		when(nivelAccesoVigente.buscar(publicId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> converter().convert(jwt(publicId)))
				.isInstanceOf(InvalidBearerTokenException.class);
	}

	@Test
	void unNivelDesconocidoEnLaBaseNoLlegaASerAuthority() {
		UUID publicId = UUID.randomUUID();
		when(nivelAccesoVigente.buscar(publicId)).thenReturn(Optional.of("ROLE_SUPERUSUARIO"));

		assertThatThrownBy(() -> converter().convert(jwt(publicId)))
				.isInstanceOf(InvalidBearerTokenException.class);
	}

	@Test
	void unSubjectQueNoEsUuidSeRechazaSinConsultarLaBase() {
		assertThatThrownBy(() -> converter().convert(jwtBuilder().subject("admin").build()))
				.isInstanceOf(InvalidBearerTokenException.class);
		assertThatThrownBy(() -> converter().convert(jwtBuilder().build()))
				.isInstanceOf(InvalidBearerTokenException.class);

		verifyNoInteractions(nivelAccesoVigente);
	}

}
