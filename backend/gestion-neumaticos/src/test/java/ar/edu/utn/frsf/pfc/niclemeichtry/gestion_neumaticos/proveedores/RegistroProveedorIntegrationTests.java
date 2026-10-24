package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorRequest;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service.ProveedorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta el proveedor y, por la
 * fachada de common, su domicilio y la localidad, con su rol de base y bajo
 * RLS; las restricciones de las tablas respaldan las reglas de los servicios.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroProveedorIntegrationTests {

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private ProveedorService proveedorService;

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
		return "Proveedor " + UUID.randomUUID();
	}

	@Test
	@DisplayName("el proveedor y su domicilio quedan registrados y normalizados; el nombre repetido se rechaza")
	void registraConDomicilioYRechazaDuplicado() {
		autenticar(NivelAcceso.ROLE_EDITOR);
		String nombre = nombreAleatorio();
		Long santaFe = provincia("Santa Fe");
		// Localidad única por test: así el CPA lo define este alta.
		String localidad = "Localidad " + UUID.randomUUID().toString().substring(0, 8);

		ProveedorResponseDTO registrado = proveedorService.registrarProveedor(proveedorRequest(" " + nombre + " ",
				" Ventas@Proveedor.COM ", " 0342 455-1234 ", domicilioRequest(" San Martín ", " 2550 ", "", null,
						santaFe, "  " + localidad + " ", " s3000abc ")), usuario.toString());

		assertThat(registrado.domicilio()).isEqualTo(new DomicilioResponseDTO("San Martín", "2550", null, null,
				localidad, "S3000ABC", santaFe, "Santa Fe"));
		assertThat(owner.queryForMap("""
				SELECT p.nombre, p.email, p.telefono, d.calle, l.nombre AS localidad, l.provincia_id
				FROM proveedores.proveedores p
				JOIN common.domicilios d ON d.id = p.domicilio_id
				JOIN common.localidades l ON l.id = d.localidad_id
				WHERE p.public_id = ?""", registrado.publicId()))
				.isEqualTo(Map.of("nombre", nombre, "email", "ventas@proveedor.com", "telefono", "0342 455-1234",
						"calle", "San Martín", "localidad", localidad, "provincia_id", santaFe));
		assertThatThrownBy(() -> proveedorService.registrarProveedor(
				proveedorRequest(nombre.toUpperCase(), null, null, null), usuario.toString()))
				.isInstanceOf(EntidadDuplicadaException.class);
	}

	@Test
	@DisplayName("una localidad existente se reutiliza sin distinguir mayúsculas y completa su código postal")
	void reutilizaLocalidad() {
		autenticar(NivelAcceso.ROLE_ADMINISTRADOR);
		Long cordoba = provincia("Córdoba");
		String localidad = "Villa " + UUID.randomUUID().toString().substring(0, 8);

		proveedorService.registrarProveedor(proveedorRequest(nombreAleatorio(), null, null,
				domicilioRequest("Colón", "100", null, null, cordoba, localidad, null)), usuario.toString());
		ProveedorResponseDTO segundo = proveedorService.registrarProveedor(proveedorRequest(nombreAleatorio(), null,
				null, domicilioRequest("Colón", "200", null, null, cordoba, localidad.toUpperCase(), "5000")),
				usuario.toString());
		ProveedorResponseDTO tercero = proveedorService.registrarProveedor(proveedorRequest(nombreAleatorio(), null,
				null, domicilioRequest("Colón", "300", null, null, cordoba, localidad, "5001")),
				usuario.toString());

		assertThat(owner.queryForMap(
				"SELECT count(*) AS cantidad, min(codigo_postal) AS cp FROM common.localidades WHERE lower(nombre) = ?",
				localidad.toLowerCase()))
				.isEqualTo(Map.of("cantidad", 1L, "cp", "5000"));
		// La primera escritura conserva su capitalización; un CP ya cargado no se pisa.
		assertThat(segundo.domicilio().localidad()).isEqualTo(localidad);
		assertThat(tercero.domicilio().codigoPostal()).isEqualTo("5000");
	}

	@Test
	@DisplayName("sin domicilio no se crea ninguno; una provincia inexistente revierte todo el alta")
	void sinDomicilioYProvinciaInexistente() {
		autenticar(NivelAcceso.ROLE_EDITOR);
		String nombre = nombreAleatorio();

		ProveedorResponseDTO registrado = proveedorService.registrarProveedor(
				proveedorRequest(nombre, null, null, null), usuario.toString());

		assertThat(registrado.domicilio()).isNull();
		assertThat(owner.queryForObject("SELECT domicilio_id FROM proveedores.proveedores WHERE public_id = ?",
				Long.class, registrado.publicId())).isNull();

		String otro = nombreAleatorio();
		assertThatThrownBy(() -> proveedorService.registrarProveedor(proveedorRequest(otro, null, null,
				domicilioRequest("Belgrano", "1", null, null, -1L, "Rafaela", null)), usuario.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(owner.queryForObject("SELECT count(*) FROM proveedores.proveedores WHERE nombre = ?",
				Integer.class, otro)).isZero();
	}

	@Test
	@DisplayName("el lector no puede insertar ni saltándose el controller: la base no le da INSERT")
	void lectorNoInserta() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		assertThatThrownBy(() -> proveedorService.registrarProveedor(
				proveedorRequest(nombreAleatorio(), null, null, null), usuario.toString()))
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

}
