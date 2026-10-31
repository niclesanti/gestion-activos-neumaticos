package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionRequest;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service.ServicioReparacionService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta el servicio y, por la
 * fachada de common, su domicilio y la localidad, con su rol de base y bajo
 * RLS; las restricciones de la tabla respaldan las reglas del servicio.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroServicioReparacionIntegrationTests {

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private ServicioReparacionService servicioReparacionService;

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
		return "Gomería " + UUID.randomUUID();
	}

	@Test
	@DisplayName("el servicio y su domicilio quedan registrados y normalizados, sin neumáticos en reparación; el nombre repetido se rechaza")
	void registraConDomicilioYRechazaDuplicado() {
		autenticar(NivelAcceso.ROLE_EDITOR);
		String nombre = nombreAleatorio();
		Long santaFe = provincia("Santa Fe");
		// Localidad única por test: así el CPA lo define este alta.
		String localidad = "Localidad " + UUID.randomUUID().toString().substring(0, 8);

		ServicioReparacionResponseDTO registrado = servicioReparacionService.registrarServicioReparacion(
				servicioReparacionRequest(" " + nombre + " ", " Contacto@Gomeria.COM ", " 0342 455-1234 ",
						domicilioRequest(" San Martín ", " 2550 ", "", null, santaFe, "  " + localidad + " ",
								" s3000abc ")),
				usuario.toString());

		assertThat(registrado.cantidadNeumaticosEnReparacion()).isZero();
		assertThat(registrado.domicilio()).isEqualTo(new DomicilioResponseDTO("San Martín", "2550", null, null,
				localidad, "S3000ABC", santaFe, "Santa Fe"));
		assertThat(owner.queryForMap("""
				SELECT s.nombre, s.email, s.telefono, s.cantidad_neumaticos_en_reparacion AS cantidad,
				       d.calle, l.nombre AS localidad, l.provincia_id
				FROM reparacion.servicios_reparacion s
				JOIN common.domicilios d ON d.id = s.domicilio_id
				JOIN common.localidades l ON l.id = d.localidad_id
				WHERE s.public_id = ?""", registrado.publicId()))
				.isEqualTo(Map.of("nombre", nombre, "email", "contacto@gomeria.com", "telefono", "0342 455-1234",
						"cantidad", 0, "calle", "San Martín", "localidad", localidad, "provincia_id", santaFe));
		assertThatThrownBy(() -> servicioReparacionService.registrarServicioReparacion(
				servicioReparacionRequest(nombre.toUpperCase(), null, null, null), usuario.toString()))
				.isInstanceOf(EntidadDuplicadaException.class);
	}

	@Test
	@DisplayName("sin domicilio no se crea ninguno; una provincia inexistente revierte todo el alta")
	void sinDomicilioYProvinciaInexistente() {
		autenticar(NivelAcceso.ROLE_ADMINISTRADOR);
		String nombre = nombreAleatorio();

		ServicioReparacionResponseDTO registrado = servicioReparacionService.registrarServicioReparacion(
				servicioReparacionRequest(nombre, null, null, null), usuario.toString());

		assertThat(registrado.domicilio()).isNull();
		assertThat(owner.queryForObject(
				"SELECT domicilio_id FROM reparacion.servicios_reparacion WHERE public_id = ?",
				Long.class, registrado.publicId())).isNull();

		String otro = nombreAleatorio();
		assertThatThrownBy(() -> servicioReparacionService.registrarServicioReparacion(
				servicioReparacionRequest(otro, null, null,
						domicilioRequest("Belgrano", "1", null, null, -1L, "Rafaela", null)),
				usuario.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(owner.queryForObject("SELECT count(*) FROM reparacion.servicios_reparacion WHERE nombre = ?",
				Integer.class, otro)).isZero();
	}

	@Test
	@DisplayName("el lector no puede insertar ni saltándose el controller: la base no le da INSERT")
	void lectorNoInserta() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		assertThatThrownBy(() -> servicioReparacionService.registrarServicioReparacion(
				servicioReparacionRequest(nombreAleatorio(), null, null, null), usuario.toString()))
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

}
