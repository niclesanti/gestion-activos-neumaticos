package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.bd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

class RolBaseDatosConfigTest {

	@Nested
	class PostProcessor {

		private final BeanPostProcessor postProcessor = RolBaseDatosConfig.rolBaseDatosDataSourcePostProcessor();

		@Test
		void envuelveElDataSourceDeLaApp() {
			DataSource original = mock(DataSource.class);

			Object resultado = postProcessor.postProcessAfterInitialization(original, "dataSource");

			assertThat(resultado).isInstanceOf(RolBaseDatosDataSource.class);
			assertThat(((RolBaseDatosDataSource) resultado).getTargetDataSource()).isSameAs(original);
		}

		@Test
		void noVuelveAEnvolverUnDataSourceYaEnvuelto() {
			RolBaseDatosDataSource envuelto = new RolBaseDatosDataSource(mock(DataSource.class));

			assertThat(postProcessor.postProcessAfterInitialization(envuelto, "dataSource")).isSameAs(envuelto);
		}

		@Test
		void ignoraLosBeansQueNoSonDataSource() {
			Object bean = new Object();

			assertThat(postProcessor.postProcessAfterInitialization(bean, "otroBean")).isSameAs(bean);
		}

	}

	@Nested
	class VerificacionDelRolDeConexion {

		private final DataSource dataSource = mock(DataSource.class);
		private final Connection conexion = mock(Connection.class);
		private final Statement statement = mock(Statement.class);
		private final ResultSet resultado = mock(ResultSet.class);

		private InitializingBean verificacion() throws SQLException {
			when(dataSource.getConnection()).thenReturn(conexion);
			when(conexion.createStatement()).thenReturn(statement);
			when(statement.executeQuery(anyString())).thenReturn(resultado);
			return new RolBaseDatosConfig().verificarRolDeConexionSinPrivilegios(dataSource);
		}

		@Test
		void arrancaConUnRolSinPrivilegios() throws Exception {
			InitializingBean verificacion = verificacion();
			when(resultado.next()).thenReturn(true);
			when(resultado.getBoolean(2)).thenReturn(false);

			assertThatCode(verificacion::afterPropertiesSet).doesNotThrowAnyException();
		}

		@Test
		void arrancaSiElRolNoApareceEnPgRoles() throws Exception {
			InitializingBean verificacion = verificacion();
			when(resultado.next()).thenReturn(false);

			assertThatCode(verificacion::afterPropertiesSet).doesNotThrowAnyException();
		}

		@Test
		void noArrancaConUnSuperusuarioOBypassRls() throws Exception {
			InitializingBean verificacion = verificacion();
			when(resultado.next()).thenReturn(true);
			when(resultado.getBoolean(2)).thenReturn(true);
			when(resultado.getString(1)).thenReturn("postgres");

			assertThatThrownBy(verificacion::afterPropertiesSet)
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("'postgres'")
					.hasMessageContaining("gn_app");
		}

		@Test
		void noArrancaSiNoPuedeVerificarElRol() throws Exception {
			SQLException error = new SQLException("connection refused");
			when(dataSource.getConnection()).thenThrow(error);
			InitializingBean verificacion = new RolBaseDatosConfig().verificarRolDeConexionSinPrivilegios(dataSource);

			assertThatThrownBy(verificacion::afterPropertiesSet)
					.isInstanceOf(IllegalStateException.class)
					.hasCause(error);
		}

	}

}
