package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service.ConductorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta con su rol de base, bajo
 * RLS, y las restricciones de la tabla respaldan las reglas del servicio.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroConductorIntegrationTests {

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private ConductorService conductorService;

	/** Superusuario del contenedor, no sujeto a RLS: solo para verificar datos. */
	private JdbcTemplate owner;

	@Autowired
	void owner(PostgreSQLContainer postgres) {
		owner = new JdbcTemplate(new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	private void autenticar(NivelAcceso nivel) {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, nivel));
	}

	/** DNI de 8 dígitos distinto en cada llamada, para no chocar con otros tests. */
	private static String dniAleatorio() {
		return String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
	}

	@Test
	@DisplayName("el conductor queda registrado normalizado y un DNI repetido se rechaza")
	void registraYRechazaDuplicado() {
		autenticar(NivelAcceso.ROLE_EDITOR);
		String dni = dniAleatorio();

		ConductorResponseDTO registrado = conductorService.registrarConductor(
				conductorRequest(" " + dni + " ", " Juan ", " Pérez "), usuario.toString());

		assertThat(owner.queryForMap("SELECT dni, nombre, apellido FROM conductores.conductores WHERE public_id = ?",
				registrado.publicId()))
				.isEqualTo(Map.of("dni", dni, "nombre", "Juan", "apellido", "Pérez"));
		assertThatThrownBy(() -> conductorService.registrarConductor(
				conductorRequest(dni, "Otro", "Conductor"), usuario.toString()))
				.isInstanceOf(EntidadDuplicadaException.class);
	}

	@Test
	@DisplayName("varios conductores sin DNI conviven: la unicidad no aplica a NULL")
	void variosSinDni() {
		autenticar(NivelAcceso.ROLE_ADMINISTRADOR);

		ConductorResponseDTO primero = conductorService.registrarConductor(conductorRequest(null, "Ana", "Gómez"),
				usuario.toString());
		ConductorResponseDTO segundo = conductorService.registrarConductor(conductorRequest("", "Luis", "Díaz"),
				usuario.toString());

		assertThat(owner.queryForObject(
				"SELECT count(*) FROM conductores.conductores WHERE dni IS NULL AND public_id IN (?, ?)",
				Integer.class, primero.publicId(), segundo.publicId())).isEqualTo(2);
	}

	@Test
	@DisplayName("el lector no puede insertar ni saltándose el controller: la base no le da INSERT")
	void lectorNoInserta() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		assertThatThrownBy(() -> conductorService.registrarConductor(conductorRequest(dniAleatorio(), "Juan",
				"Pérez"), usuario.toString()))
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

}
