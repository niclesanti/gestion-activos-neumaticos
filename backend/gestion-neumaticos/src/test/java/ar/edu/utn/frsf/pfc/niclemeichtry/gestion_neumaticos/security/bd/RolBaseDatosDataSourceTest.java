package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.bd;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Qué rol y usuario se fijan en cada conexión según el {@code SecurityContext}.
 * El efecto real en PostgreSQL lo cubre {@link AutorizacionBaseDatosTests}; acá
 * se prueban los casos que la cadena de seguridad no deja llegar a la base
 * (tokens sin nivel, sin {@code sub}, etc.): ante cualquier duda, sesión anónima.
 */
@ExtendWith(MockitoExtension.class)
class RolBaseDatosDataSourceTest {

	@Mock
	private DataSource destino;

	@Mock
	private Connection conexion;

	@Mock
	private PreparedStatement statement;

	private RolBaseDatosDataSource dataSource;

	/** Cableado de los mocks JDBC: los datos de sesión vienen de {@code TestDataFactory}. */
	@BeforeEach
	void conectarMocks() throws SQLException {
		dataSource = new RolBaseDatosDataSource(destino);
		when(conexion.prepareStatement(anyString())).thenReturn(statement);
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void sinAutenticacionVuelveAlRolDeConexion() throws SQLException {
		when(destino.getConnection()).thenReturn(conexion);

		assertThat(dataSource.getConnection()).isSameAs(conexion);

		verificarSesion("none", "");
	}

	@Test
	void conJwtValidoAsumeElRolDelNivelYFijaElUsuario() throws SQLException {
		UUID publicId = UUID.randomUUID();
		autenticar(autenticacion(publicId, NivelAcceso.ROLE_EDITOR));
		when(destino.getConnection()).thenReturn(conexion);

		dataSource.getConnection();

		verificarSesion("gn_editor", publicId.toString());
	}

	@Test
	void conUsuarioYClaveExplicitosTambienFijaLaSesion() throws SQLException {
		UUID publicId = UUID.randomUUID();
		autenticar(autenticacion(publicId, NivelAcceso.ROLE_ADMINISTRADOR));
		when(destino.getConnection("usuario", "clave")).thenReturn(conexion);

		assertThat(dataSource.getConnection("usuario", "clave")).isSameAs(conexion);

		verificarSesion("gn_administrador", publicId.toString());
	}

	@Test
	void unaAutenticacionQueNoEsJwtEsAnonima() throws SQLException {
		autenticar(new UsernamePasswordAuthenticationToken("usuario", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
		when(destino.getConnection()).thenReturn(conexion);

		dataSource.getConnection();

		verificarSesion("none", "");
	}

	@Test
	void unNivelDeAccesoDesconocidoEsAnonimo() throws SQLException {
		autenticarJwt(jwtBuilder().subject(UUID.randomUUID().toString())
				.claim(TokenService.CLAIM_NIVEL_ACCESO, "ROLE_SUPERUSUARIO").build());
		when(destino.getConnection()).thenReturn(conexion);

		dataSource.getConnection();

		verificarSesion("none", "");
	}

	@Test
	void unTokenSinSubjectEsAnonimo() throws SQLException {
		autenticarJwt(jwtBuilder().claim(TokenService.CLAIM_NIVEL_ACCESO, "ROLE_EDITOR").build());
		when(destino.getConnection()).thenReturn(conexion);

		dataSource.getConnection();

		verificarSesion("none", "");
	}

	@Test
	void unSubjectQueNoEsUuidEsAnonimo() throws SQLException {
		autenticarJwt(jwtBuilder().subject("'; DROP ROLE gn_app; --")
				.claim(TokenService.CLAIM_NIVEL_ACCESO, "ROLE_EDITOR").build());
		when(destino.getConnection()).thenReturn(conexion);

		dataSource.getConnection();

		verificarSesion("none", "");
	}

	@Test
	void siNoPuedeFijarLaSesionCierraLaConexionYPropagaElError() throws SQLException {
		SQLException error = new SQLException("permission denied to set role");
		when(destino.getConnection()).thenReturn(conexion);
		when(statement.execute()).thenThrow(error);

		assertThatThrownBy(dataSource::getConnection).isSameAs(error);

		verify(conexion).close();
	}

	private static void autenticar(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private static void autenticarJwt(Jwt jwt) {
		autenticar(new JwtAuthenticationToken(jwt));
	}

	private void verificarSesion(String rol, String usuarioId) throws SQLException {
		verify(statement).setString(1, rol);
		verify(statement).setString(2, usuarioId);
		verify(statement).execute();
	}

}
