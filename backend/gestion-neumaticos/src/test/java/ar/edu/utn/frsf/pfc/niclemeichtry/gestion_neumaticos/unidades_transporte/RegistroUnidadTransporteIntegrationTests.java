package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadTransporteRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotUnidadTransporteDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta la unidad y sus slots con
 * su rol de base y bajo RLS, a partir de las configuraciones que carga V16.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroUnidadTransporteIntegrationTests {

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private UnidadTransporteService unidadTransporteService;

	/** Superusuario del contenedor, no sujeto a RLS: solo para preparar y verificar datos. */
	private JdbcTemplate owner;

	@Autowired
	void owner(PostgreSQLContainer postgres) {
		owner = new JdbcTemplate(new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@BeforeEach
	void limpiar() {
		owner.update("DELETE FROM unidades_transporte.slots_unidad_transporte");
		owner.update("DELETE FROM unidades_transporte.unidades_transporte");
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	private void autenticar(NivelAcceso nivel) {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, nivel));
	}

	private Long configuracion(String nombre) {
		return owner.queryForObject("SELECT id FROM unidades_transporte.configuraciones_unidad WHERE nombre = ?",
				Long.class, nombre);
	}

	@Test
	@DisplayName("la migración carga las tres configuraciones con sus ejes: 7, 11 y 13 slots")
	void configuracionesPrecargadas() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		List<ConfiguracionUnidadDTO> configuraciones = unidadTransporteService.listarConfiguraciones();

		assertThat(configuraciones).extracting(ConfiguracionUnidadDTO::nombre)
				.containsExactly("Tractor común", "Tractor para cargas pesadas", "Semirremolque");
		assertThat(configuraciones).extracting(ConfiguracionUnidadDTO::totalEjes).containsExactly(2, 3, 3);
		assertThat(configuraciones).extracting(ConfiguracionUnidadDTO::totalSlots).containsExactly(7, 11, 13);
	}

	@Test
	@DisplayName("la unidad y todos sus slots quedan registrados, libres y con la patente normalizada")
	void registraConSlots() {
		autenticar(NivelAcceso.ROLE_EDITOR);

		UnidadTransporteResponseDTO registrada = unidadTransporteService.registrarUnidadTransporte(
				unidadTransporteRequest(" ab-123 cd ", configuracion("Tractor para cargas pesadas")),
				usuario.toString());

		assertThat(registrada.patente()).isEqualTo("AB123CD");
		assertThat(registrada.slots()).extracting(SlotUnidadTransporteDTO::codigo).containsExactly(
				"E1-IZQ", "E1-DER",
				"E2-IZQ-EXT", "E2-IZQ-INT", "E2-DER-INT", "E2-DER-EXT",
				"E3-IZQ-EXT", "E3-IZQ-INT", "E3-DER-INT", "E3-DER-EXT",
				"AUX-1");
		assertThat(owner.queryForMap("""
				SELECT u.patente, count(s.id) AS slots, count(s.neumatico_actual_id) AS ocupados,
				       count(*) FILTER (WHERE s.tipo_slot = 'AUXILIO') AS auxilios
				FROM unidades_transporte.unidades_transporte u
				JOIN unidades_transporte.slots_unidad_transporte s ON s.unidad_id = u.id
				WHERE u.public_id = ?
				GROUP BY u.patente""", registrada.publicId()))
				.isEqualTo(Map.of("patente", "AB123CD", "slots", 11L, "ocupados", 0L, "auxilios", 1L));
	}

	@Test
	@DisplayName("la misma patente escrita de otra forma se rechaza; una configuración inexistente no crea nada")
	void rechazaDuplicadaYConfiguracionInexistente() {
		autenticar(NivelAcceso.ROLE_ADMINISTRADOR);
		unidadTransporteService.registrarUnidadTransporte(
				unidadTransporteRequest("AB123CD", configuracion("Semirremolque")), usuario.toString());

		assertThatThrownBy(() -> unidadTransporteService.registrarUnidadTransporte(
				unidadTransporteRequest("ab 123 cd", configuracion("Tractor común")), usuario.toString()))
				.isInstanceOf(EntidadDuplicadaException.class);
		assertThatThrownBy(() -> unidadTransporteService.registrarUnidadTransporte(
				unidadTransporteRequest("AC456DE", -1L), usuario.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(owner.queryForObject("SELECT count(*) FROM unidades_transporte.unidades_transporte",
				Integer.class)).isOne();
		assertThat(owner.queryForObject("SELECT count(*) FROM unidades_transporte.slots_unidad_transporte",
				Integer.class)).isEqualTo(13);
	}

	@Test
	@DisplayName("el lector no puede insertar ni saltándose el controller: la base no le da INSERT")
	void lectorNoInserta() {
		autenticar(NivelAcceso.ROLE_LECTOR);

		assertThatThrownBy(() -> unidadTransporteService.registrarUnidadTransporte(
				unidadTransporteRequest("AB123CD", configuracion("Tractor común")), usuario.toString()))
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

}
