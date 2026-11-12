package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte;

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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import jakarta.persistence.EntityManagerFactory;

/** Ficha de una unidad contra PostgreSQL real, como Lector (rol de base gn_lector, bajo RLS). */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
class DetalleUnidadTransporteIntegrationTests {

	private final UUID usuario = UUID.randomUUID();
	private final UUID publicId = UUID.randomUUID();

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
		owner.update("UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id = NULL");
		owner.update("DELETE FROM neumaticos.neumaticos");
		owner.update("DELETE FROM unidades_transporte.slots_unidad_transporte");
		owner.update("DELETE FROM unidades_transporte.unidades_transporte");
		insertarUnidad("AB123CD", publicId, false);
		SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, NivelAcceso.ROLE_LECTOR));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	/** Tractor común: E1 simple + E2 dual + 1 auxilio, siete slots. */
	private void insertarUnidad(String patente, UUID unidadPublicId, boolean baja) {
		owner.update("""
				INSERT INTO unidades_transporte.unidades_transporte
				  (public_id, patente, configuracion_id, created_by, updated_by, deleted_at, deleted_by)
				SELECT ?, ?, c.id, ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM unidades_transporte.configuraciones_unidad c WHERE c.nombre = 'Tractor común'""",
				unidadPublicId, patente, usuario, usuario, baja, baja, usuario);
		Long unidadId = owner.queryForObject(
				"SELECT id FROM unidades_transporte.unidades_transporte WHERE public_id = ?", Long.class,
				unidadPublicId);
		slot(unidadId, "EJE", 1, "DIRECCION", "IZQUIERDO", "SIMPLE", null);
		slot(unidadId, "EJE", 1, "DIRECCION", "DERECHO", "SIMPLE", null);
		slot(unidadId, "EJE", 2, "TRACCION", "IZQUIERDO", "EXTERIOR", null);
		slot(unidadId, "EJE", 2, "TRACCION", "IZQUIERDO", "INTERIOR", null);
		slot(unidadId, "EJE", 2, "TRACCION", "DERECHO", "INTERIOR", null);
		slot(unidadId, "EJE", 2, "TRACCION", "DERECHO", "EXTERIOR", null);
		slot(unidadId, "AUXILIO", null, null, null, null, 1);
	}

	private void slot(Long unidadId, String tipoSlot, Integer nroEje, String tipoEje, String lado, String posicion,
			Integer nroAuxilio) {
		owner.update("""
				INSERT INTO unidades_transporte.slots_unidad_transporte
				  (unidad_id, tipo_slot, nro_eje, tipo_eje, lado, posicion_rueda, nro_auxilio)
				VALUES (?, ?, ?, ?, ?, ?, ?)""", unidadId, tipoSlot, nroEje, tipoEje, lado, posicion, nroAuxilio);
	}

	/** Neumático con marca Fate, montado en el slot E1-IZQ de la unidad. */
	private UUID montarNeumatico(String numeroSerie, Float kilometraje) {
		UUID neumaticoPublicId = UUID.randomUUID();
		owner.update("""
				INSERT INTO neumaticos.neumaticos
				  (public_id, numero_serie, estado_actual, kilometraje_total, tipo_origen, tipo_actual,
				   situacion_operativa_vigente, marca_id, created_by, updated_by)
				SELECT ?, ?, 'NUEVO_USADO', ?, 'DIRECCION', 'DIRECCION', 'EN_STOCK', m.id, ?, ?
				FROM neumaticos.marcas_neumaticos m WHERE m.nombre = 'Fate'""",
				neumaticoPublicId, numeroSerie, kilometraje, usuario, usuario);
		owner.update("""
				UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id =
				  (SELECT id FROM neumaticos.neumaticos WHERE public_id = ?)
				WHERE nro_eje = 1 AND lado = 'IZQUIERDO'""", neumaticoPublicId);
		return neumaticoPublicId;
	}

	@Test
	@DisplayName("sin neumáticos montados todas las posiciones vienen vacías")
	void todasVacias() {
		UnidadTransporteDetalleDTO detalle = unidadTransporteService.obtenerDetalle(publicId);

		assertThat(detalle.patente()).isEqualTo("AB123CD");
		assertThat(detalle.configuracion().nombre()).isEqualTo("Tractor común");
		assertThat(detalle.configuracion().ejes()).hasSize(2);
		assertThat(detalle.slots()).hasSize(7).allMatch(slot -> !slot.ocupado() && slot.neumatico() == null);
	}

	@Test
	@DisplayName("el lector ve el neumático montado con serie, marca, estado y kilometraje")
	void conNeumaticoMontado() {
		UUID neumaticoPublicId = montarNeumatico("SER-001", 2500f);

		UnidadTransporteDetalleDTO detalle = unidadTransporteService.obtenerDetalle(publicId);

		SlotDetalleDTO ocupado = detalle.slots().stream().filter(SlotDetalleDTO::ocupado).findFirst().orElseThrow();
		assertThat(detalle.slots()).filteredOn(SlotDetalleDTO::ocupado).hasSize(1);
		assertThat(ocupado.codigo()).isEqualTo("E1-IZQ");
		assertThat(ocupado.neumatico().publicId()).isEqualTo(neumaticoPublicId);
		assertThat(ocupado.neumatico().numeroSerie()).isEqualTo("SER-001");
		assertThat(ocupado.neumatico().marca()).isEqualTo("Fate");
		assertThat(ocupado.neumatico().estado()).isEqualTo("NUEVO_USADO");
		assertThat(ocupado.neumatico().kilometrajeTotal()).isEqualTo(2500f);
	}

	@Test
	@DisplayName("un neumático sin serie ni kilometraje se informa con esos datos nulos")
	void datosNoRegistrados() {
		montarNeumatico(null, null);

		SlotDetalleDTO ocupado = unidadTransporteService.obtenerDetalle(publicId).slots().stream()
				.filter(SlotDetalleDTO::ocupado).findFirst().orElseThrow();

		assertThat(ocupado.neumatico().numeroSerie()).isNull();
		assertThat(ocupado.neumatico().kilometrajeTotal()).isNull();
	}

	@Test
	@DisplayName("la ficha se arma con una cantidad fija de sentencias: sin N+1 por slot")
	void sinNMasUno() {
		montarNeumatico("SER-001", 10f);
		Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		estadisticas.clear();

		unidadTransporteService.obtenerDetalle(publicId);

		// unidad+configuración+slots, ejes y neumáticos con marca (más lo que ya pidan el rol y el usuario).
		assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(6);
	}

	@Test
	@DisplayName("una unidad dada de baja o inexistente responde 404")
	void bajaOInexistente() {
		UUID dadaDeBaja = UUID.randomUUID();
		insertarUnidad("XYZ789", dadaDeBaja, true);

		assertThatThrownBy(() -> unidadTransporteService.obtenerDetalle(dadaDeBaja))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> unidadTransporteService.obtenerDetalle(UUID.randomUUID()))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

}
