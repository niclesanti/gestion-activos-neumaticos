package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.PosicionOcupadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.AsignacionNeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Montaje de neumáticos (HU-31) contra PostgreSQL real, como Editor (rol de base gn_editor, bajo RLS): la
 * posición, la unidad, el neumático y el stock del centro cambian en una transacción, y trazabilidad registra
 * los eventos de forma asíncrona como el sistema (gn_app).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AsignacionNeumaticoIntegrationTests {

	private final UUID editor = UUID.randomUUID();
	private final UUID unidad = UUID.randomUUID();
	private final UUID centro = UUID.randomUUID();
	private final UUID conductor = UUID.randomUUID();

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
	void prepararDatos() {
		owner.update("DELETE FROM trazabilidad.eventos_neumatico");
		owner.update("UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id = NULL");
		owner.update("DELETE FROM neumaticos.neumaticos");
		owner.update("DELETE FROM unidades_transporte.slots_unidad_transporte");
		owner.update("DELETE FROM unidades_transporte.unidades_transporte");
		owner.update("DELETE FROM almacenamiento.centros_almacenamiento");
		owner.update("DELETE FROM conductores.conductores");
		insertarUnidad("AB123CD", unidad, false);
		owner.update("""
				INSERT INTO almacenamiento.centros_almacenamiento
				  (public_id, nombre, cantidad_neumaticos_en_stock, domicilio_id, created_by, updated_by)
				VALUES (?, 'Depósito Central', 1, 999101, ?, ?)""", centro, editor, editor);
		owner.update("""
				INSERT INTO conductores.conductores (public_id, dni, nombre, apellido, created_by, updated_by)
				VALUES (?, '30123456', 'Juan', 'Pérez', ?, ?)""", conductor, editor, editor);
		SecurityContextHolder.getContext().setAuthentication(autenticacion(editor, NivelAcceso.ROLE_EDITOR));
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	/** Tractor común: E1 simple + E2 dual + 1 auxilio. Solo se cargan las posiciones que usan los tests. */
	private void insertarUnidad(String patente, UUID publicId, boolean baja) {
		owner.update("""
				INSERT INTO unidades_transporte.unidades_transporte
				  (public_id, patente, configuracion_id, created_by, updated_by, deleted_at, deleted_by)
				SELECT ?, ?, c.id, ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM unidades_transporte.configuraciones_unidad c WHERE c.nombre = 'Tractor común'""",
				publicId, patente, editor, editor, baja, baja, editor);
		Long unidadId = owner.queryForObject(
				"SELECT id FROM unidades_transporte.unidades_transporte WHERE public_id = ?", Long.class, publicId);
		owner.update("""
				INSERT INTO unidades_transporte.slots_unidad_transporte
				  (unidad_id, tipo_slot, nro_eje, tipo_eje, lado, posicion_rueda)
				VALUES (?, 'EJE', 1, 'DIRECCION', 'IZQUIERDO', 'SIMPLE'),
				       (?, 'EJE', 2, 'TRACCION', 'IZQUIERDO', 'EXTERIOR')""", unidadId, unidadId);
	}

	/** Neumático Fate en la situación dada; en stock, guardado en el centro de prueba. */
	private UUID insertarNeumatico(String serie, String estado, String situacion, boolean bajaLogica) {
		UUID publicId = UUID.randomUUID();
		owner.update("""
				INSERT INTO neumaticos.neumaticos
				  (public_id, numero_serie, estado_actual, en_espera, kilometraje_total, tipo_origen, tipo_actual,
				   situacion_operativa_vigente, centro_almacenamiento_id, marca_id, created_by, updated_by,
				   deleted_at, deleted_by)
				SELECT ?, ?, ?, ? = 'EN_ESPERA', 1000, 'TRACCION', 'TRACCION', ?,
				       CASE WHEN ? = 'EN_STOCK' THEN
				         (SELECT id FROM almacenamiento.centros_almacenamiento WHERE public_id = ?) END,
				       m.id, ?, ?, CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END
				FROM neumaticos.marcas_neumaticos m WHERE m.nombre = 'Fate'""",
				publicId, serie, estado, situacion, situacion, situacion, centro, editor, editor, bajaLogica,
				bajaLogica, editor);
		return publicId;
	}

	/** Monta con el owner el neumático en la posición E1-IZQ de la unidad de prueba. */
	private UUID neumaticoYaMontado(String serie) {
		UUID publicId = insertarNeumatico(serie, "NUEVO_USADO", "MONTADO", false);
		owner.update("""
				UPDATE unidades_transporte.slots_unidad_transporte SET neumatico_actual_id =
				  (SELECT id FROM neumaticos.neumaticos WHERE public_id = ?)
				WHERE nro_eje = 1""", publicId);
		owner.update("""
				UPDATE neumaticos.neumaticos SET slot_montado_id =
				  (SELECT id FROM unidades_transporte.slots_unidad_transporte WHERE nro_eje = 1)
				WHERE public_id = ?""", publicId);
		return publicId;
	}

	private Map<String, Object> neumatico(UUID publicId) {
		return owner.queryForMap("""
				SELECT n.id, n.situacion_operativa_vigente, n.en_espera, n.slot_montado_id, n.centro_almacenamiento_id,
				       n.tipo_actual, n.kilometraje_total
				FROM neumaticos.neumaticos n WHERE n.public_id = ?""", publicId);
	}

	private Long slotE1() {
		return owner.queryForObject("SELECT id FROM unidades_transporte.slots_unidad_transporte WHERE nro_eje = 1",
				Long.class);
	}

	private Integer stockDelCentro() {
		return owner.queryForObject(
				"SELECT cantidad_neumaticos_en_stock FROM almacenamiento.centros_almacenamiento WHERE public_id = ?",
				Integer.class, centro);
	}

	private List<Map<String, Object>> eventos(UUID neumaticoPublicId, int esperados) {
		Long neumaticoId = (Long) neumatico(neumaticoPublicId).get("id");
		String consulta = """
				SELECT tipo, usuario, numero_serie_neumatico, patente_unidad, posicion_slot, slot_id,
				       conductor_id IS NOT NULL AS con_conductor, nombre_conductor
				FROM trazabilidad.eventos_neumatico WHERE neumatico_id = ? ORDER BY tipo""";
		await().atMost(Duration.ofSeconds(10)).untilAsserted(
				() -> assertThat(owner.queryForList(consulta, neumaticoId)).hasSize(esperados));
		return owner.queryForList(consulta, neumaticoId);
	}

	@Test
	@DisplayName("CA2/CA6: monta el neumático en stock, lo descuenta del centro, firma la unidad y registra la "
			+ "asignación con conductor")
	void montaDesdeStock() {
		UUID entrante = insertarNeumatico("STK-001", "NUEVO", "EN_STOCK", false);

		UnidadTransporteDetalleDTO detalle = unidadTransporteService.asignarNeumatico(unidad, "E1-IZQ",
				new AsignacionNeumaticoRequestDTO(entrante, conductor, null), editor.toString());

		SlotDetalleDTO slot = detalle.slots().stream().filter(s -> s.codigo().equals("E1-IZQ")).findFirst()
				.orElseThrow();
		assertThat(slot.ocupado()).isTrue();
		assertThat(slot.neumatico().publicId()).isEqualTo(entrante);
		Map<String, Object> montado = neumatico(entrante);
		assertThat(montado.get("situacion_operativa_vigente")).isEqualTo("MONTADO");
		assertThat(montado.get("en_espera")).isEqualTo(false);
		assertThat(montado.get("slot_montado_id")).isEqualTo(slotE1());
		assertThat(montado.get("centro_almacenamiento_id")).isNull();
		assertThat(montado.get("tipo_actual")).isEqualTo("TRACCION");
		assertThat(montado.get("kilometraje_total")).isEqualTo(1000f);
		assertThat(stockDelCentro()).isZero();
		assertThat(owner.queryForObject("SELECT updated_by FROM unidades_transporte.unidades_transporte "
				+ "WHERE public_id = ?", UUID.class, unidad)).isEqualTo(editor);
		assertThat(eventos(entrante, 1)).containsExactly(Map.of(
				"tipo", "ASIGNACION",
				"usuario", editor.toString(),
				"numero_serie_neumatico", "STK-001",
				"patente_unidad", "AB123CD",
				"posicion_slot", "E1-IZQ",
				"slot_id", slotE1(),
				"con_conductor", true,
				"nombre_conductor", "Juan Pérez"));
	}

	@Test
	@DisplayName("CA7/CA3: ocupada sin aceptar responde 409 sin cambios; aceptada, el saliente queda en espera y "
			+ "se registran desasignación y asignación")
	void reemplazo() {
		UUID saliente = neumaticoYaMontado("MON-001");
		UUID entrante = insertarNeumatico("ESP-001", "NUEVO_USADO", "EN_ESPERA", false);

		assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(unidad, "E1-IZQ",
				new AsignacionNeumaticoRequestDTO(entrante, null, false), editor.toString()))
				.isInstanceOf(PosicionOcupadaException.class);
		assertThat(neumatico(saliente).get("situacion_operativa_vigente")).isEqualTo("MONTADO");
		assertThat(neumatico(entrante).get("situacion_operativa_vigente")).isEqualTo("EN_ESPERA");

		unidadTransporteService.asignarNeumatico(unidad, "E1-IZQ",
				new AsignacionNeumaticoRequestDTO(entrante, null, true), editor.toString());

		Map<String, Object> desmontado = neumatico(saliente);
		assertThat(desmontado.get("situacion_operativa_vigente")).isEqualTo("EN_ESPERA");
		assertThat(desmontado.get("en_espera")).isEqualTo(true);
		assertThat(desmontado.get("slot_montado_id")).isNull();
		assertThat(neumatico(entrante).get("slot_montado_id")).isEqualTo(slotE1());
		assertThat(owner.queryForObject("SELECT neumatico_actual_id FROM unidades_transporte.slots_unidad_transporte "
				+ "WHERE nro_eje = 1", Long.class)).isEqualTo(neumatico(entrante).get("id"));
		assertThat(eventos(saliente, 1)).singleElement().satisfies(evento -> {
			assertThat(evento.get("tipo")).isEqualTo("DESASIGNACION");
			assertThat(evento.get("patente_unidad")).isEqualTo("AB123CD");
			assertThat(evento.get("posicion_slot")).isEqualTo("E1-IZQ");
			assertThat(evento.get("usuario")).isEqualTo(editor.toString());
			assertThat(evento.get("con_conductor")).isEqualTo(false);
		});
		assertThat(eventos(entrante, 1)).singleElement()
				.satisfies(evento -> assertThat(evento.get("tipo")).isEqualTo("ASIGNACION"));
	}

	@Test
	@DisplayName("CA8: montado, en reparación, de baja (estado o situación) o con baja lógica: 409; inexistente: 404")
	void noDisponibles() {
		List<UUID> rechazados = List.of(
				neumaticoYaMontado("MON-002"),
				insertarNeumatico("REP-001", "NUEVO_USADO", "EN_REPARACION", false),
				insertarNeumatico("BAJ-001", "DE_BAJA", "EN_ESPERA", false),
				insertarNeumatico("BAJ-002", "NUEVO_USADO", "DE_BAJA", false),
				insertarNeumatico("BAJ-003", "NUEVO_USADO", "EN_STOCK", true));

		for (UUID rechazado : rechazados) {
			assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(unidad, "E2-IZQ-EXT",
					new AsignacionNeumaticoRequestDTO(rechazado, null, null), editor.toString()))
					.isInstanceOf(NeumaticoNoDisponibleException.class);
		}
		assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(unidad, "E2-IZQ-EXT",
				new AsignacionNeumaticoRequestDTO(UUID.randomUUID(), null, null), editor.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);

		assertThat(stockDelCentro()).isOne();
		assertThat(owner.queryForObject("SELECT count(*) FROM unidades_transporte.slots_unidad_transporte "
				+ "WHERE nro_eje = 2 AND neumatico_actual_id IS NOT NULL", Integer.class)).isZero();
	}

	@Test
	@DisplayName("CA9: unidad de baja, posición ajena o conductor inexistente: 404 y nada cambia (rollback)")
	void noEncontradosSinCambios() {
		UUID entrante = insertarNeumatico("STK-002", "NUEVO", "EN_STOCK", false);
		UUID dadaDeBaja = UUID.randomUUID();
		insertarUnidad("XYZ789", dadaDeBaja, true);

		assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(dadaDeBaja, "E1-IZQ",
				new AsignacionNeumaticoRequestDTO(entrante, null, null), editor.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(unidad, "E3-IZQ-EXT",
				new AsignacionNeumaticoRequestDTO(entrante, null, null), editor.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> unidadTransporteService.asignarNeumatico(unidad, "E1-IZQ",
				new AsignacionNeumaticoRequestDTO(entrante, UUID.randomUUID(), null), editor.toString()))
				.isInstanceOf(EntidadNoEncontradaException.class);

		assertThat(neumatico(entrante).get("situacion_operativa_vigente")).isEqualTo("EN_STOCK");
		assertThat(stockDelCentro()).isOne();
	}

}
