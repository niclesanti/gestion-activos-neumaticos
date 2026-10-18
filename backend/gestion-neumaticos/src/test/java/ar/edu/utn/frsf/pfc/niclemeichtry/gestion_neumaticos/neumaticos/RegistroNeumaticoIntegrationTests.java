package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Alta completa contra PostgreSQL: el editor inserta con su rol de base y el
 * listener de trazabilidad, asíncrono y sin usuario (gn_app), registra la
 * compra después del commit, todo bajo RLS.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroNeumaticoIntegrationTests {

	private final UUID editor = UUID.randomUUID();

	@Autowired
	private NeumaticoService neumaticoService;

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

	private Long marcaFate() {
		return owner.queryForObject("SELECT id FROM neumaticos.marcas_neumaticos WHERE nombre = 'Fate'", Long.class);
	}

	private NeumaticoRequestDTO alta(String numeroSerie, EstadoNeumatico estado, Float km, Integer recapados) {
		return new NeumaticoRequestDTO(numeroSerie, estado, km, km, TipoEjeUnidadTransporte.TRACCION,
				TipoEjeUnidadTransporte.ARRASTRE, recapados, "alta de prueba", marcaFate(),
				estado == EstadoNeumatico.NUEVO ? PROVEEDOR_PUBLIC_ID : null);
	}

	@Test
	@DisplayName("un neumático nuevo queda registrado y trazabilidad guarda la compra de forma asíncrona")
	void nuevoGeneraEventoDeCompra() {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(editor, NivelAcceso.ROLE_EDITOR));
		String serie = "it-" + UUID.randomUUID();

		NeumaticoResponseDTO registrado = neumaticoService.registrarNeumatico(
				alta(serie, EstadoNeumatico.NUEVO, 0f, 0), editor.toString());

		assertThat(registrado.numeroSerie()).isEqualTo(serie.toUpperCase());
		Long neumaticoId = owner.queryForObject("SELECT id FROM neumaticos.neumaticos WHERE public_id = ?",
				Long.class, registrado.publicId());

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(owner.queryForList("""
				SELECT tipo, usuario, numero_serie_neumatico, proveedor_id, nombre_proveedor
				FROM trazabilidad.eventos_neumatico
				WHERE neumatico_id = ?""", neumaticoId))
				.containsExactly(Map.of(
						"tipo", "COMPRA",
						"usuario", editor.toString(),
						"numero_serie_neumatico", serie.toUpperCase(),
						"proveedor_id", 1L,
						"nombre_proveedor", "Neumáticos del Litoral S.A.")));
		// El listener terminó bien: la publicación quedó completada.
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(owner.queryForObject(
				"SELECT count(*) FROM public.event_publication WHERE completion_date IS NULL AND event_type LIKE ?",
				Integer.class, "%NeumaticoComprado"))
				.isZero());
	}

	@Test
	@DisplayName("un neumático usado no genera evento de compra; un número de serie repetido se rechaza")
	void usadoSinEventoYDuplicado() {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(editor, NivelAcceso.ROLE_EDITOR));
		String serie = "it-" + UUID.randomUUID();

		NeumaticoResponseDTO registrado = neumaticoService.registrarNeumatico(
				alta(serie, EstadoNeumatico.RECAPADO, 50_000f, 1), editor.toString());

		Long neumaticoId = owner.queryForObject("SELECT id FROM neumaticos.neumaticos WHERE public_id = ?",
				Long.class, registrado.publicId());
		assertThat(owner.queryForObject("SELECT count(*) FROM trazabilidad.eventos_neumatico WHERE neumatico_id = ?",
				Integer.class, neumaticoId)).isZero();
		assertThatThrownBy(() -> neumaticoService.registrarNeumatico(
				alta(serie.toUpperCase(), EstadoNeumatico.HOMBROS, 10f, 0), editor.toString()))
				.isInstanceOf(EntidadDuplicadaException.class);
	}

	@Test
	@DisplayName("sin kilometrajes ni recapados conocidos se guardan NULL")
	void datosDeUsoDesconocidos() {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(editor, NivelAcceso.ROLE_EDITOR));

		NeumaticoResponseDTO registrado = neumaticoService.registrarNeumatico(
				alta(null, EstadoNeumatico.HOMBROS, null, null), editor.toString());

		assertThat(registrado.kilometrajeTotal()).isNull();
		assertThat(owner.queryForMap("""
				SELECT kilometraje_total, kilometraje_ultima_reparacion, cantidad_recapados
				FROM neumaticos.neumaticos WHERE public_id = ?""", registrado.publicId()).values())
				.hasSize(3)
				.containsOnlyNulls();
	}

}
