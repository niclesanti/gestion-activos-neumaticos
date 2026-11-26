package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos;

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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import jakarta.persistence.EntityManagerFactory;

/** Búsqueda de neumáticos contra PostgreSQL real, como Lector (rol de base gn_lector, bajo RLS). */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
class BusquedaNeumaticoIntegrationTests {

	private static final Pageable POR_SERIE = PageRequest.of(0, 20, Sort.by("numeroSerie"));

	private final UUID usuario = UUID.randomUUID();

	@Autowired
	private NeumaticoService neumaticoService;

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
		owner.update("DELETE FROM neumaticos.neumaticos");
		// Activos
		insertar("AB-123", "Fate", "NUEVO", "EN_STOCK", true, false);
		insertar("ab-456", "Pirelli", "HOMBROS", "MONTADO", false, false);
		insertar("XYZ_9%", "Fate", "NUEVO", "EN_ESPERA", true, false);
		insertar(null, "Bridgestone", "NUEVO", "EN_REPARACION", false, false);
		// Activo en espera por el booleano pero con otra situación vigente
		insertar("AB-777", "Fate", "HOMBROS", "MONTADO", true, false);
		// Inactivos: baja lógica, estado DE_BAJA y situación DE_BAJA
		insertar("AB-900", "Fate", "NUEVO", "EN_STOCK", false, true);
		insertar("AB-901", "Fate", "DE_BAJA", "EN_STOCK", false, false);
		insertar("AB-902", "Fate", "NUEVO", "DE_BAJA", false, false);
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, NivelAcceso.ROLE_LECTOR));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	/** Inserta como owner; la baja lógica se simula con deleted_at / deleted_by. */
	private void insertar(String serie, String marca, String estado, String situacion, boolean enEspera,
			boolean bajaLogica) {
		owner.update("""
				INSERT INTO neumaticos.neumaticos
				  (public_id, numero_serie, estado_actual, en_espera, kilometraje_total, kilometraje_ultima_reparacion,
				   tipo_origen, tipo_actual, cantidad_recapados, situacion_operativa_vigente, marca_id,
				   created_by, updated_by, deleted_at, deleted_by)
				SELECT gen_random_uuid(), ?, ?, ?, 100, 50, 'TRACCION', 'TRACCION',
				       CASE WHEN ? = 'HOMBROS' THEN 0 ELSE 1 END, ?, m.id,
				       ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM neumaticos.marcas_neumaticos m WHERE m.nombre = ?""",
				serie == null ? null : serie.toUpperCase(), estado, enEspera, estado, situacion, usuario, usuario,
				bajaLogica, bajaLogica, usuario, marca);
	}

	private Long marcaId(String nombre) {
		return owner.queryForObject("SELECT id FROM neumaticos.marcas_neumaticos WHERE nombre = ?", Long.class,
				nombre);
	}

	private List<String> series(Page<NeumaticoListadoDTO> pagina) {
		return pagina.getContent().stream().map(NeumaticoListadoDTO::numeroSerie).toList();
	}

	private Page<NeumaticoListadoDTO> buscar(String serie, Long marcaId, EstadoNeumatico estado,
			SituacionOperativa situacion) {
		return neumaticoService.buscar(serie, marcaId, estado, situacion == null ? null : List.of(situacion), null,
				POR_SERIE);
	}

	@Test
	@DisplayName("CA1: el número de serie coincide en cualquier posición y sin distinguir mayúsculas")
	void parcialSinDistinguirMayusculas() {
		assertThat(series(buscar("b-12", null, null, null))).containsExactly("AB-123");
		assertThat(series(buscar("  Ab-4 ", null, null, null))).containsExactly("AB-456");
	}

	@Test
	@DisplayName("los comodines de LIKE se buscan literales")
	void comodinesSeBuscanLiterales() {
		assertThat(buscar("%", null, null, null).getContent()).hasSize(1);
		assertThat(series(buscar("_9%", null, null, null))).containsExactly("XYZ_9%");
		assertThat(buscar("A_-1", null, null, null).getContent()).isEmpty();
	}

	@Test
	@DisplayName("un filtro por número de serie nunca devuelve neumáticos sin serie")
	void conSerieNoDevuelveSinSerie() {
		assertThat(series(buscar("A", null, null, null))).doesNotContainNull();
	}

	@Test
	@DisplayName("CA2: serie, marca y estado se combinan con AND")
	void combinaFiltros() {
		assertThat(series(buscar("AB", marcaId("Fate"), EstadoNeumatico.NUEVO, null))).containsExactly("AB-123");
		assertThat(series(buscar("AB", marcaId("Fate"), EstadoNeumatico.HOMBROS, null))).containsExactly("AB-777");
		assertThat(buscar("AB", marcaId("Pirelli"), EstadoNeumatico.NUEVO, null).getContent()).isEmpty();
	}

	@Test
	@DisplayName("CA3: la situación operativa filtra por un solo valor y se combina con los demás filtros")
	void filtraPorSituacion() {
		assertThat(series(buscar(null, null, null, SituacionOperativa.EN_STOCK))).containsExactly("AB-123");
		assertThat(series(buscar(null, null, null, SituacionOperativa.MONTADO))).containsExactly("AB-456", "AB-777");
		assertThat(series(buscar(null, marcaId("Fate"), null, SituacionOperativa.MONTADO))).containsExactly("AB-777");
		assertThat(buscar(null, null, null, SituacionOperativa.EN_REPARACION).getContent())
				.extracting(NeumaticoListadoDTO::marca).containsExactly("Bridgestone");
	}

	@Test
	@DisplayName("CA7: en_espera = true con otra situación vigente no aparece al filtrar EN_ESPERA")
	void enEsperaSeFiltraPorSituacionVigente() {
		assertThat(series(buscar(null, null, null, SituacionOperativa.EN_ESPERA))).containsExactly("XYZ_9%");
	}

	@Test
	@DisplayName("filtrar por DE_BAJA (estado o situación) o por una marca inexistente da una página vacía")
	void deBajaYMarcaInexistente_vacio() {
		assertThat(buscar(null, null, EstadoNeumatico.DE_BAJA, null).getContent()).isEmpty();
		assertThat(buscar(null, null, null, SituacionOperativa.DE_BAJA).getContent()).isEmpty();
		assertThat(buscar(null, -1L, null, null).getContent()).isEmpty();
	}

	@Test
	@DisplayName("CA6: con baja lógica, estado DE_BAJA o situación DE_BAJA no aparecen ni se cuentan")
	void excluyeLosInactivos() {
		Page<NeumaticoListadoDTO> pagina = buscar("AB-9", null, null, null);

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	@DisplayName("CA4: sin filtros devuelve todos los activos, con serie ordenados y los sin serie al final")
	void sinFiltros_devuelveTodosLosActivos() {
		Page<NeumaticoListadoDTO> pagina = buscar(null, null, null, null);

		assertThat(series(pagina)).containsExactly("AB-123", "AB-456", "AB-777", "XYZ_9%", null);
		assertThat(pagina.getTotalElements()).isEqualTo(5);
		assertThat(series(buscar("  ", null, null, null))).hasSize(5);
	}

	@Test
	@DisplayName("cada fila trae marca, estado, situación y kilometraje")
	void devuelveLosDatosDeLaTabla() {
		assertThat(buscar("AB-456", null, null, null).getContent())
				.extracting(NeumaticoListadoDTO::marca, NeumaticoListadoDTO::estadoActual,
						NeumaticoListadoDTO::situacionOperativaVigente, NeumaticoListadoDTO::kilometrajeTotal)
				.containsExactly(tuple("Pirelli", EstadoNeumatico.HOMBROS, SituacionOperativa.MONTADO, 100f));
	}

	@Test
	@DisplayName("CA8: pagina con los metadatos correctos y orden estable con varios sin serie")
	void pagina() {
		insertar(null, "Fate", "NUEVO", "EN_STOCK", false, false);

		Page<NeumaticoListadoDTO> primera = neumaticoService.buscar(null, null, null, null, null,
				PageRequest.of(0, 4, Sort.by("numeroSerie")));
		Page<NeumaticoListadoDTO> segunda = neumaticoService.buscar(null, null, null, null, null,
				PageRequest.of(1, 4, Sort.by("numeroSerie")));

		assertThat(primera.getContent()).hasSize(4);
		assertThat(segunda.getContent()).hasSize(2);
		assertThat(primera.getTotalElements()).isEqualTo(6);
		assertThat(primera.getTotalPages()).isEqualTo(2);
		assertThat(segunda.getContent()).extracting(NeumaticoListadoDTO::publicId)
				.doesNotContainAnyElementsOf(primera.getContent().stream().map(NeumaticoListadoDTO::publicId).toList());
	}

	@Test
	@DisplayName("una página completa se carga con a lo sumo 2 sentencias: sin N+1")
	void cargaLaPaginaSinNMasUno() {
		Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estadisticas.clear();

		buscar(null, null, null, null);

		assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(2);
	}

	@Test
	@DisplayName("HU-31: situaciones repetibles (OR), filtro por centro y nombre del centro en cada fila")
	void disponiblesYCentro() {
		UUID centro = UUID.randomUUID();
		owner.update("DELETE FROM almacenamiento.centros_almacenamiento");
		owner.update("""
				INSERT INTO almacenamiento.centros_almacenamiento
				  (public_id, nombre, cantidad_neumaticos_en_stock, domicilio_id, created_by, updated_by)
				VALUES (?, 'Depósito Central', 1, 999001, ?, ?)""", centro, usuario, usuario);
		owner.update("""
				UPDATE neumaticos.neumaticos SET centro_almacenamiento_id =
				  (SELECT id FROM almacenamiento.centros_almacenamiento WHERE public_id = ?)
				WHERE numero_serie = 'AB-123'""", centro);

		Page<NeumaticoListadoDTO> disponibles = neumaticoService.buscar(null, null, null,
				List.of(SituacionOperativa.EN_STOCK, SituacionOperativa.EN_ESPERA), null, POR_SERIE);
		Page<NeumaticoListadoDTO> delCentro = neumaticoService.buscar(null, null, null,
				List.of(SituacionOperativa.EN_STOCK), centro, POR_SERIE);

		assertThat(disponibles.getContent()).extracting(NeumaticoListadoDTO::numeroSerie,
				NeumaticoListadoDTO::centroAlmacenamiento)
				.containsExactly(tuple("AB-123", "Depósito Central"), tuple("XYZ_9%", null));
		assertThat(series(delCentro)).containsExactly("AB-123");
		assertThat(neumaticoService.buscar(null, null, null, null, UUID.randomUUID(), POR_SERIE).getContent())
				.isEmpty();
	}

	@Test
	@DisplayName("CA9: el Lector lee con su rol de base (gn_lector) sin permission denied")
	void lectorLeeConSuRolDeBase() {
		assertThat(buscar(null, null, null, null).getTotalElements()).isEqualTo(5);
	}

}
