package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import jakarta.persistence.EntityManagerFactory;

/** Búsqueda por patente contra PostgreSQL real, como Lector (rol de base gn_lector, bajo RLS). */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
class BusquedaUnidadTransporteIntegrationTests {

	private static final Pageable POR_PATENTE = PageRequest.of(0, 20, Sort.by("patente"));

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private UnidadTransporteService unidadTransporteService;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/** Superusuario del contenedor, no sujeto a RLS: solo para preparar datos. */
	private JdbcTemplate owner;

	@Autowired
	void owner(PostgreSQLContainer postgres) {
		owner = new JdbcTemplate(new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@BeforeEach
	void prepararDatos() {
		owner.update("DELETE FROM unidades_transporte.slots_unidad_transporte");
		owner.update("DELETE FROM unidades_transporte.unidades_transporte");
		insertar("AB123CD", "Tractor común", false);
		insertar("AB456EF", "Semirremolque", false);
		insertar("XYZ789", "Tractor común", false);
		insertar("AC100DE", "Semirremolque", false);
		insertar("AB123ZZ", "Tractor común", true);
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, NivelAcceso.ROLE_LECTOR));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	/** Inserta como owner; la baja se simula con deleted_at / deleted_by. */
	private void insertar(String patente, String configuracion, boolean baja) {
		owner.update("""
				INSERT INTO unidades_transporte.unidades_transporte
				  (public_id, patente, configuracion_id, created_by, updated_by, deleted_at, deleted_by)
				SELECT gen_random_uuid(), ?, c.id, ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM unidades_transporte.configuraciones_unidad c WHERE c.nombre = ?""",
				patente, usuario, usuario, baja, baja, usuario, configuracion);
	}

	private List<String> patentes(Page<UnidadTransporteListadoDTO> pagina) {
		return pagina.getContent().stream().map(UnidadTransporteListadoDTO::patente).toList();
	}

	@Test
	@DisplayName("coincide en cualquier posición y sin distinguir mayúsculas")
	void parcialSinDistinguirMayusculas() {
		assertThat(patentes(unidadTransporteService.buscar("b12", POR_PATENTE))).containsExactly("AB123CD");
	}

	@Test
	@DisplayName("el término se normaliza: espacios y guiones se ignoran")
	void ignoraEspaciosYGuiones() {
		assertThat(patentes(unidadTransporteService.buscar("ab 1", POR_PATENTE))).containsExactly("AB123CD");
		assertThat(patentes(unidadTransporteService.buscar("AB-12", POR_PATENTE))).containsExactly("AB123CD");
	}

	@Test
	@DisplayName("los comodines de LIKE se buscan literales")
	void comodinesSeBuscanLiterales() {
		assertThat(unidadTransporteService.buscar("%", POR_PATENTE).getContent()).isEmpty();
		assertThat(unidadTransporteService.buscar("_", POR_PATENTE).getContent()).isEmpty();
	}

	@Test
	@DisplayName("cada elemento trae el nombre y el tipo de su configuración")
	void devuelveLaConfiguracionResumida() {
		Page<UnidadTransporteListadoDTO> pagina = unidadTransporteService.buscar(null, POR_PATENTE);

		assertThat(pagina.getContent())
				.extracting(u -> u.configuracion().nombre(), u -> u.configuracion().tipoUnidad())
				.containsExactly(
						tuple("Tractor común", TipoUnidad.TRACTOR),
						tuple("Semirremolque", TipoUnidad.SEMIRREMOLQUE),
						tuple("Semirremolque", TipoUnidad.SEMIRREMOLQUE),
						tuple("Tractor común", TipoUnidad.TRACTOR));
	}

	@Test
	@DisplayName("las unidades dadas de baja no aparecen ni se cuentan")
	void excluyeLasDadasDeBaja() {
		Page<UnidadTransporteListadoDTO> pagina = unidadTransporteService.buscar("AB123", POR_PATENTE);

		assertThat(patentes(pagina)).containsExactly("AB123CD");
		assertThat(pagina.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("sin término (o en blanco) devuelve todas las activas ordenadas por patente")
	void sinTermino_devuelveTodasLasActivas() {
		List<String> esperadas = List.of("AB123CD", "AB456EF", "AC100DE", "XYZ789");

		assertThat(patentes(unidadTransporteService.buscar(null, POR_PATENTE))).isEqualTo(esperadas);
		assertThat(patentes(unidadTransporteService.buscar("  ", POR_PATENTE))).isEqualTo(esperadas);
	}

	@Test
	@DisplayName("sin coincidencias devuelve una página vacía")
	void sinCoincidencias_devuelvePaginaVacia() {
		Page<UnidadTransporteListadoDTO> pagina = unidadTransporteService.buscar("QQQ", POR_PATENTE);

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	@DisplayName("pagina con los metadatos correctos")
	void pagina() {
		Page<UnidadTransporteListadoDTO> pagina = unidadTransporteService.buscar(null,
				PageRequest.of(1, 2, Sort.by("patente")));

		assertThat(patentes(pagina)).containsExactly("AC100DE", "XYZ789");
		assertThat(pagina.getTotalElements()).isEqualTo(4);
		assertThat(pagina.getTotalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("una página completa se carga con a lo sumo 2 sentencias: sin N+1")
	void cargaLaPaginaSinNMasUno() {
		Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estadisticas.clear();

		unidadTransporteService.buscar(null, POR_PATENTE);

		assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(2);
	}

	@Test
	@DisplayName("el Lector lee con su rol de base (gn_lector) sin permission denied")
	void lectorLeeConSuRolDeBase() {
		// Todos los casos corren como Lector; este deja explícito el criterio.
		assertThat(unidadTransporteService.buscar(null, POR_PATENTE).getTotalElements()).isEqualTo(4);
	}

}
