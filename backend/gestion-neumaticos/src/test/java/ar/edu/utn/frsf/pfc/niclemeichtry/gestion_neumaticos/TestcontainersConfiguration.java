package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real para los tests de integración: las migraciones usan roles,
 * GRANT y RLS, que no existen en una base embebida.
 *
 * <p>Igual que en dev y prod, Flyway migra con el owner (el superusuario del
 * contenedor) y la app se conecta como {@code gn_app}. No se usa
 * {@code @ServiceConnection} porque conectaría la app como superusuario y las
 * políticas no se aplicarían.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final String CLAVE_GN_APP = "gn_app_test";

	@Bean
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18-alpine");
	}

	@Bean
	DynamicPropertyRegistrar propiedadesBaseDeDatos(PostgreSQLContainer postgres) {
		return registry -> {
			registry.add("spring.datasource.url", postgres::getJdbcUrl);
			registry.add("spring.datasource.username", () -> "gn_app");
			registry.add("spring.datasource.password", () -> CLAVE_GN_APP);
			registry.add("spring.flyway.user", postgres::getUsername);
			registry.add("spring.flyway.password", postgres::getPassword);
			registry.add("spring.flyway.placeholders.app_password", () -> CLAVE_GN_APP);
			registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
			registry.add("app.security.jwt.secret", () -> "secreto-de-prueba-de-al-menos-32-bytes-para-hs256");
		};
	}

}
