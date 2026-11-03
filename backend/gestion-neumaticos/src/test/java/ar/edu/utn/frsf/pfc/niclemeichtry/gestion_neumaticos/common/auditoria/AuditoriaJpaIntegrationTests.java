package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.HASH_CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository.ProveedorRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service.ProveedorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

/**
 * Auditoría de punta a punta contra PostgreSQL (HU-43): Spring Data JPA Auditing
 * firma con el usuario autenticado, la base de datos lo respalda con RLS y la consulta
 * del administrador devuelve las fichas con el nombre de quien firmó.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuditoriaJpaIntegrationTests {

    private final UUID editor = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProveedorService proveedorService;

    /** Superusuario del contenedor, no sujeto a RLS: solo para preparar y verificar datos. */
    private JdbcTemplate owner;

    @Autowired
    void owner(PostgreSQLContainer postgres) {
        owner = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
    }

    @BeforeEach
    void crearUsuarios() {
        insertarUsuario(editor, "aud-ed-" + sufijo, "Victoria Editora", NivelAcceso.ROLE_EDITOR);
        insertarUsuario(admin, "aud-ad-" + sufijo, "Santiago Administrador", NivelAcceso.ROLE_ADMINISTRADOR);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void insertarUsuario(UUID publicId, String nombreUsuario, String nombreApellido, NivelAcceso nivel) {
        owner.update("""
                INSERT INTO usuarios.usuarios (public_id, nombre_usuario, nombre_apellido, email, clave, nivel_acceso)
                VALUES (?, ?, ?, ?, ?, ?)""",
                publicId, nombreUsuario, nombreApellido, nombreUsuario + "@neumaticos.local", HASH_CLAVE, nivel.name());
    }

    private void autenticar(UUID usuario, NivelAcceso nivel) {
        SecurityContextHolder.getContext().setAuthentication(autenticacion(usuario, nivel));
    }

    private ProveedorNeumaticos altaComoEditor(String nombre) {
        autenticar(editor, NivelAcceso.ROLE_EDITOR);
        return proveedorRepository.saveAndFlush(ProveedorNeumaticos.builder().nombre(nombre).build());
    }

    private Map<String, Object> fila(UUID publicId) {
        return owner.queryForMap("""
                SELECT created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
                FROM proveedores.proveedores WHERE public_id = ?""", publicId);
    }

    private static Instant instante(Object timestamp) {
        return ((Timestamp) timestamp).toInstant();
    }

    @Test
    @DisplayName("el alta registra el usuario autenticado y la fecha, sin intervención manual")
    void altaRegistraCreacion() {
        Instant antes = Instant.now().minusSeconds(1);

        ProveedorNeumaticos creado = altaComoEditor("Aud-alta-" + sufijo);

        assertThat(creado.getCreatedBy()).isEqualTo(editor);
        Map<String, Object> fila = fila(creado.getPublicId());
        assertThat(fila.get("created_by")).isEqualTo(editor);
        assertThat(fila.get("updated_by")).isEqualTo(editor);
        assertThat(instante(fila.get("created_at"))).isAfter(antes).isBeforeOrEqualTo(Instant.now());
        assertThat(instante(fila.get("updated_at"))).isEqualTo(instante(fila.get("created_at")));
        assertThat(fila.get("deleted_at")).isNull();
        assertThat(fila.get("deleted_by")).isNull();
    }

    @Test
    @DisplayName("la modificación actualiza usuario y fecha de última modificación, y conserva los de creación")
    void modificacionActualizaUltimaModificacion() {
        ProveedorNeumaticos creado = altaComoEditor("Aud-mod-" + sufijo);
        Instant alta = instante(fila(creado.getPublicId()).get("created_at"));
        autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);

        ProveedorNeumaticos cargado = proveedorRepository.findByPublicId(creado.getPublicId()).orElseThrow();
        cargado.setTelefono("455123");
        proveedorRepository.saveAndFlush(cargado);

        Map<String, Object> fila = fila(creado.getPublicId());
        assertThat(fila.get("created_by")).isEqualTo(editor);
        assertThat(instante(fila.get("created_at"))).isEqualTo(alta);
        assertThat(fila.get("updated_by")).isEqualTo(admin);
        assertThat(instante(fila.get("updated_at"))).isAfter(alta);
    }

    @Test
    @DisplayName("la baja lógica registra usuario y fecha, no borra la fila y no se repite")
    void bajaLogica() {
        ProveedorNeumaticos creado = altaComoEditor("Aud-baja-" + sufijo);
        autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);
        ProveedorNeumaticos cargado = proveedorRepository.findByPublicId(creado.getPublicId()).orElseThrow();
        Instant momento = Instant.now();

        cargado.darDeBaja(admin, momento);
        proveedorRepository.saveAndFlush(cargado);

        Map<String, Object> fila = fila(creado.getPublicId());
        assertThat(fila.get("deleted_by")).isEqualTo(admin);
        assertThat(instante(fila.get("deleted_at"))).isCloseTo(momento, org.assertj.core.api.Assertions.within(
                1, java.time.temporal.ChronoUnit.MILLIS));
        assertThat(fila.get("updated_by")).isEqualTo(admin);
        assertThat(owner.queryForObject("SELECT count(*) FROM proveedores.proveedores WHERE public_id = ?",
                Integer.class, creado.getPublicId())).isOne();

        ProveedorNeumaticos dadoDeBaja = proveedorRepository.findByPublicId(creado.getPublicId()).orElseThrow();
        assertThatThrownBy(() -> dadoDeBaja.darDeBaja(admin, Instant.now()))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("la consulta del administrador devuelve las fichas con el nombre de quien firmó y respeta los filtros")
    void consultaDelAdministrador() {
        ProveedorNeumaticos activo = altaComoEditor("Aud-consulta-activo-" + sufijo);
        ProveedorNeumaticos conBaja = altaComoEditor("Aud-consulta-baja-" + sufijo);
        autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);
        ProveedorNeumaticos cargado = proveedorRepository.findByPublicId(conBaja.getPublicId()).orElseThrow();
        cargado.darDeBaja(admin, Instant.now());
        proveedorRepository.saveAndFlush(cargado);
        PageRequest porModificacion = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String q = "aud-consulta-";

        Page<RegistroAuditoriaDTO> todos = proveedorService.listarAuditoria(
                new FiltroAuditoria(q + "%", null, null, null), porModificacion);
        Page<RegistroAuditoriaDTO> encontrados = proveedorService.listarAuditoria(
                new FiltroAuditoria(q, FiltroAuditoria.EstadoFiltro.TODOS, null, null), porModificacion);
        Page<RegistroAuditoriaDTO> soloBajas = proveedorService.listarAuditoria(
                new FiltroAuditoria(q, FiltroAuditoria.EstadoFiltro.BAJAS, null, null), porModificacion);
        Page<RegistroAuditoriaDTO> soloActivos = proveedorService.listarAuditoria(
                new FiltroAuditoria(q, FiltroAuditoria.EstadoFiltro.ACTIVOS, null, null), porModificacion);

        // Los comodines de LIKE se buscan literales.
        assertThat(todos.getContent()).isEmpty();
        assertThat(encontrados.getContent()).extracting(RegistroAuditoriaDTO::publicId)
                .containsExactly(conBaja.getPublicId(), activo.getPublicId());
        RegistroAuditoriaDTO ficha = encontrados.getContent().get(1);
        assertThat(ficha.descripcion()).isEqualTo("Aud-consulta-activo-" + sufijo);
        assertThat(ficha.estado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(ficha.creadoPor().publicId()).isEqualTo(editor);
        assertThat(ficha.creadoPor().nombreUsuario()).isEqualTo("aud-ed-" + sufijo);
        assertThat(ficha.creadoPor().nombreApellido()).isEqualTo("Victoria Editora");
        assertThat(ficha.dadoDeBajaPor()).isNull();
        RegistroAuditoriaDTO baja = encontrados.getContent().get(0);
        assertThat(baja.estado()).isEqualTo(EstadoRegistro.BAJA);
        assertThat(baja.dadoDeBajaPor().nombreApellido()).isEqualTo("Santiago Administrador");
        assertThat(baja.modificadoPor().publicId()).isEqualTo(admin);

        assertThat(soloBajas.getContent()).extracting(RegistroAuditoriaDTO::publicId)
                .containsExactly(conBaja.getPublicId());
        assertThat(soloActivos.getContent()).extracting(RegistroAuditoriaDTO::publicId)
                .containsExactly(activo.getPublicId());
    }

    @Test
    @DisplayName("el rango de fechas acota por última modificación (inclusive, en UTC)")
    void filtroPorFechas() {
        ProveedorNeumaticos creado = altaComoEditor("Aud-fechas-" + sufijo);
        LocalDate dia = instante(fila(creado.getPublicId()).get("updated_at")).atZone(ZoneOffset.UTC).toLocalDate();
        autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);
        PageRequest pagina = PageRequest.of(0, 20, Sort.by("createdAt"));
        String q = "aud-fechas-" + sufijo;

        assertThat(cantidad(new FiltroAuditoria(q, null, dia, dia), pagina)).isOne();
        assertThat(cantidad(new FiltroAuditoria(q, null, dia.minusDays(1), dia.plusDays(1)), pagina)).isOne();
        assertThat(cantidad(new FiltroAuditoria(q, null, null, dia.minusDays(1)), pagina)).isZero();
        assertThat(cantidad(new FiltroAuditoria(q, null, dia.plusDays(1), null), pagina)).isZero();
    }

    private long cantidad(FiltroAuditoria filtro, PageRequest pagina) {
        return proveedorService.listarAuditoria(filtro, pagina).getTotalElements();
    }
}
