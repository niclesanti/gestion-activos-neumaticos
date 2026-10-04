package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real para los tests de integración: las migraciones usan roles,
 * GRANT y RLS, que no existen en una base embebida.
 *
 * <p>Igual que en dev y prod, Flyway migra con un owner dedicado sin
 * {@code SUPERUSER} ({@code gn_owner}, creado por {@code db/test/crear-owner.sql})
 * y la app se conecta como {@code gn_app}. No se usa {@code @ServiceConnection}
 * porque conectaría la app como superusuario y las políticas no se aplicarían.
 * El superusuario del contenedor solo se usa en los tests para preparar datos.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final String CLAVE_GN_APP = "gn_app_test";
	public static final String OWNER = "gn_owner";
	public static final String CLAVE_OWNER = "gn_owner_test";

	@Bean
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18-alpine").withInitScript("db/test/crear-owner.sql");
	}

	@Bean
	DynamicPropertyRegistrar propiedadesBaseDeDatos(PostgreSQLContainer postgres) {
		return registry -> {
			registry.add("spring.datasource.url", postgres::getJdbcUrl);
			registry.add("spring.datasource.username", () -> "gn_app");
			registry.add("spring.datasource.password", () -> CLAVE_GN_APP);
			registry.add("spring.flyway.user", () -> OWNER);
			registry.add("spring.flyway.password", () -> CLAVE_OWNER);
			registry.add("spring.flyway.placeholders.app_password", () -> CLAVE_GN_APP);
			registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
			registry.add("app.security.jwt.secret", () -> TestDataFactory.SECRETO_JWT);
		};
	}

}
