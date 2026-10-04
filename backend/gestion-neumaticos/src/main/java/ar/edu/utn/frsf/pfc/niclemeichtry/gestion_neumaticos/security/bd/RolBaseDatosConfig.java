package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autorización en la base: envuelve el {@link DataSource} de la app para que
 * cada conexión asuma el rol del usuario autenticado (ver
 * {@link RolBaseDatosDataSource}). Separada de {@code SecurityConfig} para que
 * los tests web, que no tienen base, no la carguen.
 */
@Configuration(proxyBeanMethods = false)
public class RolBaseDatosConfig {

	@Bean
	static BeanPostProcessor rolBaseDatosDataSourcePostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof DataSource dataSource && !(bean instanceof RolBaseDatosDataSource)) {
					return new RolBaseDatosDataSource(dataSource);
				}
				return bean;
			}
		};
	}

	/**
	 * Un superusuario o un rol con BYPASSRLS ignora todas las políticas: si la
	 * app se conectara así (por ejemplo, con las credenciales del owner), la
	 * autorización en la base dejaría de existir sin que nada fallara. Se
	 * prefiere no arrancar.
	 */
	@Bean
	InitializingBean verificarRolDeConexionSinPrivilegios(DataSource dataSource) {
		return () -> {
			try (Connection conexion = dataSource.getConnection();
					Statement statement = conexion.createStatement();
					ResultSet resultado = statement.executeQuery(
							"SELECT session_user, rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = session_user")) {
				if (resultado.next() && resultado.getBoolean(2)) {
					throw new IllegalStateException("La app no puede conectarse a la base como '"
							+ resultado.getString(1)
							+ "': es superusuario o tiene BYPASSRLS y saltearía las políticas de seguridad. "
							+ "Usar el rol gn_app (spring.datasource.username).");
				}
			} catch (SQLException ex) {
				throw new IllegalStateException("No se pudo verificar el rol de conexión a la base", ex);
			}
		};
	}

}
