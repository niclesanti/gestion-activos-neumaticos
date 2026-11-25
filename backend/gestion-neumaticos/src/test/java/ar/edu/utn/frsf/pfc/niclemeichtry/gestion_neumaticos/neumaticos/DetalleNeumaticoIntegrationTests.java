package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import jakarta.persistence.EntityManagerFactory;

/**
 * Ficha de un neumático contra PostgreSQL real, como Lector (rol de base gn_lector, bajo RLS): la ubicación se
 * resuelve leyendo unidades, centros y servicios de otros esquemas.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
class DetalleNeumaticoIntegrationTests {

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
		owner.update("UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id = NULL");
		owner.update("DELETE FROM neumaticos.neumaticos");
		owner.update("DELETE FROM unidades_transporte.slots_unidad_transporte");
		owner.update("DELETE FROM unidades_transporte.unidades_transporte");
		owner.update("DELETE FROM almacenamiento.centros_almacenamiento");
		owner.update("DELETE FROM reparacion.servicios_reparacion");
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, NivelAcceso.ROLE_LECTOR));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	/** Unidad con un único slot E1-IZQ; devuelve el id interno del slot. */
	private Long insertarSlot(String patente, boolean unidadDeBaja) {
		owner.update("""
				INSERT INTO unidades_transporte.unidades_transporte
				  (public_id, patente, configuracion_id, created_by, updated_by, deleted_at, deleted_by)
				SELECT ?, ?, c.id, ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM unidades_transporte.configuraciones_unidad c WHERE c.nombre = 'Tractor común'""",
				UUID.randomUUID(), patente, usuario, usuario, unidadDeBaja, unidadDeBaja, usuario);
		owner.update("""
				INSERT INTO unidades_transporte.slots_unidad_transporte
				  (unidad_id, tipo_slot, nro_eje, tipo_eje, lado, posicion_rueda)
				SELECT id, 'EJE', 1, 'DIRECCION', 'IZQUIERDO', 'SIMPLE'
				FROM unidades_transporte.unidades_transporte WHERE patente = ?""", patente);
		return owner.queryForObject("""
				SELECT s.id FROM unidades_transporte.slots_unidad_transporte s
				JOIN unidades_transporte.unidades_transporte u ON u.id = s.unidad_id WHERE u.patente = ?""",
				Long.class, patente);
	}

	private Long insertarCentro(String nombre) {
		owner.update("""
				INSERT INTO almacenamiento.centros_almacenamiento (public_id, nombre, domicilio_id, created_by, updated_by)
				VALUES (?, ?, ?, ?, ?)""", UUID.randomUUID(), nombre, 1000L + nombre.length(), usuario, usuario);
		return owner.queryForObject("SELECT id FROM almacenamiento.centros_almacenamiento WHERE nombre = ?",
				Long.class, nombre);
	}

	private Long insertarServicio(String nombre) {
		owner.update("""
				INSERT INTO reparacion.servicios_reparacion (public_id, nombre, created_by, updated_by)
				VALUES (?, ?, ?, ?)""", UUID.randomUUID(), nombre, usuario, usuario);
		return owner.queryForObject("SELECT id FROM reparacion.servicios_reparacion WHERE nombre = ?",
				Long.class, nombre);
	}

	/** Neumático con marca Fate en la situación indicada y las referencias de ubicación dadas. */
	private UUID insertarNeumatico(String serie, String estado, SituacionOperativa situacion, Long slotId,
			Long centroId, Long servicioId, boolean bajaLogica) {
		UUID publicId = UUID.randomUUID();
		owner.update("""
				INSERT INTO neumaticos.neumaticos
				  (public_id, numero_serie, estado_actual, kilometraje_total, kilometraje_ultima_reparacion,
				   tipo_origen, tipo_actual, cantidad_recapados, comentario, situacion_operativa_vigente,
				   slot_montado_id, centro_almacenamiento_id, servicio_reparacion_id, marca_id,
				   created_by, updated_by, deleted_at, deleted_by)
				SELECT ?, ?, ?, 2500, 500, 'DIRECCION', 'TRACCION', 1, 'Sin novedades', ?, ?, ?, ?, m.id, ?, ?,
				       CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM neumaticos.marcas_neumaticos m WHERE m.nombre = 'Fate'""",
				publicId, serie, estado, situacion.name(), slotId, centroId, servicioId, usuario, usuario,
				bajaLogica, bajaLogica, usuario);
		return publicId;
	}

	@Test
	@DisplayName("montado: el lector ve patente y posición, con los datos técnicos")
	void montado() {
		Long slotId = insertarSlot("AB123CD", false);
		UUID publicId = insertarNeumatico("SER-001", "NUEVO_USADO", SituacionOperativa.MONTADO, slotId, null, null,
				false);

		NeumaticoDetalleDTO detalle = neumaticoService.obtenerDetalle(publicId);

		assertThat(detalle.publicId()).isEqualTo(publicId);
		assertThat(detalle.numeroSerie()).isEqualTo("SER-001");
		assertThat(detalle.marca()).isEqualTo("Fate");
		assertThat(detalle.kilometrajeTotal()).isEqualTo(2500f);
		assertThat(detalle.kilometrajeUltimaReparacion()).isEqualTo(500f);
		assertThat(detalle.cantidadRecapados()).isEqualTo(1);
		assertThat(detalle.comentario()).isEqualTo("Sin novedades");
		assertThat(detalle.ubicacion()).isEqualTo(UbicacionOperativaDTO.deUnidad("AB123CD", "E1-IZQ"));
	}

	@Test
	@DisplayName("montado en una unidad dada de baja: igual se nombra la unidad")
	void montadoEnUnidadDeBaja() {
		Long slotId = insertarSlot("XYZ789", true);
		UUID publicId = insertarNeumatico("SER-002", "NUEVO", SituacionOperativa.MONTADO, slotId, null, null, false);

		assertThat(neumaticoService.obtenerDetalle(publicId).ubicacion())
				.isEqualTo(UbicacionOperativaDTO.deUnidad("XYZ789", "E1-IZQ"));
	}

	@Test
	@DisplayName("en stock: el lector ve el nombre del centro de almacenamiento")
	void enStock() {
		Long centroId = insertarCentro("Depósito Central");
		UUID publicId = insertarNeumatico("SER-003", "NUEVO", SituacionOperativa.EN_STOCK, null, centroId, null, false);

		assertThat(neumaticoService.obtenerDetalle(publicId).ubicacion())
				.isEqualTo(UbicacionOperativaDTO.deCentro("Depósito Central"));
	}

	@Test
	@DisplayName("en reparación: el lector ve el nombre del servicio de reparación")
	void enReparacion() {
		Long servicioId = insertarServicio("Gomería Sur");
		UUID publicId = insertarNeumatico("SER-004", "HOMBROS", SituacionOperativa.EN_REPARACION, null, null,
				servicioId, false);

		assertThat(neumaticoService.obtenerDetalle(publicId).ubicacion())
				.isEqualTo(UbicacionOperativaDTO.deServicio("Gomería Sur"));
	}

	@Test
	@DisplayName("en espera: sin ubicación aunque queden referencias")
	void enEspera() {
		Long centroId = insertarCentro("Depósito Viejo");
		UUID publicId = insertarNeumatico("SER-005", "NUEVO", SituacionOperativa.EN_ESPERA, null, centroId, null, false);

		NeumaticoDetalleDTO detalle = neumaticoService.obtenerDetalle(publicId);

		assertThat(detalle.situacionOperativaVigente()).isEqualTo(SituacionOperativa.EN_ESPERA);
		assertThat(detalle.ubicacion()).isEqualTo(UbicacionOperativaDTO.vacia());
	}

	@Test
	@DisplayName("de baja (estado o situación): se devuelve, sin ubicación")
	void deBaja() {
		Long centroId = insertarCentro("Depósito Baja");
		UUID porEstado = insertarNeumatico("SER-006", "DE_BAJA", SituacionOperativa.EN_STOCK, null, centroId, null,
				false);
		UUID porSituacion = insertarNeumatico("SER-007", "NUEVO", SituacionOperativa.DE_BAJA, null, centroId, null,
				false);

		assertThat(neumaticoService.obtenerDetalle(porEstado).ubicacion()).isEqualTo(UbicacionOperativaDTO.vacia());
		assertThat(neumaticoService.obtenerDetalle(porSituacion).ubicacion()).isEqualTo(UbicacionOperativaDTO.vacia());
	}

	@Test
	@DisplayName("si la referencia ya no existe la ubicación queda vacía")
	void referenciaInexistente() {
		UUID publicId = insertarNeumatico("SER-008", "NUEVO", SituacionOperativa.EN_STOCK, null, 999_999L, null, false);

		assertThat(neumaticoService.obtenerDetalle(publicId).ubicacion()).isEqualTo(UbicacionOperativaDTO.vacia());
	}

	@Test
	@DisplayName("sin serie, kilometraje ni recapados vienen nulos")
	void datosNoRegistrados() {
		UUID publicId = insertarNeumatico(null, "NUEVO", SituacionOperativa.EN_ESPERA, null, null, null, false);
		owner.update("""
				UPDATE neumaticos.neumaticos SET kilometraje_total = NULL, cantidad_recapados = NULL, comentario = NULL
				WHERE public_id = ?""", publicId);

		NeumaticoDetalleDTO detalle = neumaticoService.obtenerDetalle(publicId);

		assertThat(detalle.numeroSerie()).isNull();
		assertThat(detalle.kilometrajeTotal()).isNull();
		assertThat(detalle.cantidadRecapados()).isNull();
		assertThat(detalle.comentario()).isNull();
	}

	@Test
	@DisplayName("con baja lógica o inexistente responde 404")
	void bajaLogicaOInexistente() {
		UUID conBaja = insertarNeumatico("SER-009", "NUEVO", SituacionOperativa.EN_ESPERA, null, null, null, true);

		assertThatThrownBy(() -> neumaticoService.obtenerDetalle(conBaja))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> neumaticoService.obtenerDetalle(UUID.randomUUID()))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("la ficha se arma con una cantidad fija de sentencias: neumático con marca y una de ubicación")
	void sinNMasUno() {
		Long slotId = insertarSlot("AB123CD", false);
		UUID publicId = insertarNeumatico("SER-010", "NUEVO", SituacionOperativa.MONTADO, slotId, null, null, false);
		Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estadisticas.clear();

		neumaticoService.obtenerDetalle(publicId);

		// neumático+marca y slot+unidad (más lo que ya pidan el rol y el usuario).
		assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(4);
	}

}
