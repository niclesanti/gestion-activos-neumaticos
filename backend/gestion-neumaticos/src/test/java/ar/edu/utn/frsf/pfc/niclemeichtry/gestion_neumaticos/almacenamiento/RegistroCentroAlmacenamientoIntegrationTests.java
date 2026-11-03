package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service.CentroAlmacenamientoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta el centro y, por la
 * fachada de common, su domicilio y la localidad, con su rol de base y bajo
 * RLS; las restricciones de la tabla respaldan las reglas del servicio.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroCentroAlmacenamientoIntegrationTests {

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private CentroAlmacenamientoService centroAlmacenamientoService;

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

	private Long provincia(String nombre) {
		return owner.queryForObject("SELECT id FROM common.provincias WHERE nombre = ?", Long.class, nombre);
	}

	/** Nombre distinto en cada llamada, para no chocar con otros tests. */
	private static String nombreAleatorio() {
		return "Depósito " + UUID.randomUUID();
	}

	@Test
	@DisplayName("el centro y su domicilio quedan registrados y normalizados, sin neumáticos en stock")
	void registraConDomicilio() {
		autenticar(NivelAcceso.ROLE_EDITOR);
		String nombre = nombreAleatorio();
		Long santaFe = provincia("Santa Fe");
		// Localidad única por test: así el CPA lo define este alta.
		String localidad = "Localidad " + UUID.randomUUID().toString().substring(0, 8);

		CentroAlmacenamientoResponseDTO registrado = centroAlmacenamientoService.registrarCentroAlmacenamiento(
				centroAlmacenamientoRequest(" " + nombre + " ",
						domicilioRequest(" San Martín ", " 2550 ", "", null, santaFe, "  " + localidad + " ",
								" s3000abc ")),
				usuario.toString());

		assertThat(registrado.cantidadNeumaticosEnStock()).isZero();
		assertThat(registrado.domicilio()).isEqualTo(new DomicilioResponseDTO("San Martín", "2550", null, null,
				localidad, "S3000ABC", santaFe, "Santa Fe"));
		assertThat(owner.queryForMap("""
				SELECT c.nombre, c.cantidad_neumaticos_en_stock AS cantidad,
				       d.calle, l.nombre AS localidad, l.provincia_id
				FROM almacenamiento.centros_almacenamiento c
				JOIN common.domicilios d ON d.id = c.domicilio_id
				JOIN common.localidades l ON l.id = d.localidad_id
				WHERE c.public_id = ?""", registrado.publicId()))
				.isEqualTo(Map.of("nombre", nombre, "cantidad", 0, "calle", "San Martín", "localidad", localidad,
						"provincia_id", santaFe));
	}

	@Test
	@DisplayName("una provincia inexistente revierte todo el alta")
	void provinciaInexistente() {
		autenticar(NivelAcceso.ROLE_ADMINISTRADOR);
		String nombre = nombreAleatorio();

		assertThatThrownBy(() -> centroAlmacenamientoService.registrarCentroAlmacenamiento(
				centroAlmacenamientoRequest(nombre,
						domicilioRequest("Belgrano", "1", null, null, -1L, "Rafaela", null)),
				usuario.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(owner.queryForObject(
				"SELECT count(*) FROM almacenamiento.centros_almacenamiento WHERE nombre = ?",
				Integer.class, nombre)).isZero();
	}

	@Test
	@DisplayName("el lector no puede insertar ni saltándose el controller: la base no le da INSERT")
	void lectorNoInserta() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		assertThatThrownBy(() -> centroAlmacenamientoService.registrarCentroAlmacenamiento(
				centroAlmacenamientoRequest(nombreAleatorio(), domicilioRequest()), usuario.toString()))
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

}
