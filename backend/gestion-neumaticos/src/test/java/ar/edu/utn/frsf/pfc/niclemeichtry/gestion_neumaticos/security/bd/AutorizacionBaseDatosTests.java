package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.bd;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.HASH_CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwt;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.usuarioNuevo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.LimpiezaTokensRevocadosTask;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocadoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

/**
 * Permisos y políticas RLS contra un PostgreSQL real, a través del datasource de
 * la app ({@link RolBaseDatosDataSource}): cada caso simula el usuario
 * autenticado en el {@code SecurityContext}, como lo dejaría el resource server.
 * Los datos se preparan con el owner, que no está sujeto a RLS.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AutorizacionBaseDatosTests {

	private final UUID admin = UUID.randomUUID();
	private final UUID editor = UUID.randomUUID();
	private final UUID lector = UUID.randomUUID();

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private TokenRevocadoRepository tokenRevocadoRepository;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private LimpiezaTokensRevocadosTask limpiezaTokensRevocados;

	/** Conexiones de la app: pasan por el cambio de rol. */
	@Autowired
	private JdbcTemplate app;

	/** Conexión del owner (superusuario del contenedor), para preparar datos. */
	private JdbcTemplate owner;

	@Autowired
	void owner(PostgreSQLContainer postgres) {
		owner = new JdbcTemplate(new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@BeforeEach
	void prepararDatos() {
		owner.update("DELETE FROM seguridad.tokens_revocados");
		owner.update("DELETE FROM usuarios.usuarios");
		insertarUsuario(admin, "admin", NivelAcceso.ROLE_ADMINISTRADOR);
		insertarUsuario(editor, "editor", NivelAcceso.ROLE_EDITOR);
		insertarUsuario(lector, "lector", NivelAcceso.ROLE_LECTOR);
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	private void insertarUsuario(UUID publicId, String nombre, NivelAcceso nivel) {
		owner.update("""
				INSERT INTO usuarios.usuarios (public_id, nombre_usuario, nombre_apellido, email, clave, nivel_acceso)
				VALUES (?, ?, ?, ?, ?, ?)""",
				publicId, nombre, "Usuario " + nombre, nombre + "@neumaticos.local", HASH_CLAVE, nivel.name());
	}

	private static void autenticar(UUID publicId, NivelAcceso nivel) {
		SecurityContextHolder.getContext().setAuthentication(autenticacion(publicId, nivel));
	}

	private static void permisoDenegado(Runnable operacion) {
		assertThatThrownBy(operacion::run)
				.isInstanceOf(DataAccessException.class)
				.hasStackTraceContaining("permission denied");
	}

	@Nested
	@DisplayName("Rol de la conexión")
	class RolDeConexion {

		@Test
		@DisplayName("sin sesión la conexión queda en gn_app y sin usuario")
		void sinSesion() {
			assertThat(app.queryForObject("SELECT current_user", String.class)).isEqualTo("gn_app");
			assertThat(app.queryForObject("SELECT current_setting('app.usuario_id', true)", String.class))
					.isEmpty();
		}

		@Test
		@DisplayName("con sesión asume el rol del nivel de acceso y fija el usuario")
		void conSesion() {
			autenticar(editor, NivelAcceso.ROLE_EDITOR);

			assertThat(app.queryForObject("SELECT current_user", String.class)).isEqualTo("gn_editor");
			assertThat(app.queryForObject("SELECT current_setting('app.usuario_id')", String.class))
					.isEqualTo(editor.toString());
		}

		@Test
		@DisplayName("una conexión reciclada del pool no conserva el usuario anterior")
		void conexionReciclada() {
			autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);
			app.queryForObject("SELECT 1", Integer.class);
			SecurityContextHolder.clearContext();

			assertThat(app.queryForObject("SELECT current_user", String.class)).isEqualTo("gn_app");
		}

		@Test
		@DisplayName("la app no se conecta con un rol que saltee RLS")
		void sinBypassRls() {
			assertThat(app.queryForObject(
					"SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = session_user", Boolean.class))
					.isFalse();
		}

	}

	@Nested
	@DisplayName("usuarios.usuarios")
	class Usuarios {

		@Test
		@DisplayName("sin sesión no se puede leer la tabla")
		void sinSesionNoLee() {
			permisoDenegado(() -> usuarioRepository.findAll());
		}

		@Test
		@DisplayName("sin sesión el login encuentra al usuario por nombre o email")
		void loginSinSesion() {
			assertThat(usuarioRepository.buscarParaLogin("editor")).get()
					.extracting(Usuario::getPublicId).isEqualTo(editor);
			assertThat(usuarioRepository.buscarParaLogin("lector@neumaticos.local")).get()
					.extracting(Usuario::getPublicId).isEqualTo(lector);
			assertThat(usuarioRepository.buscarParaLogin("inexistente")).isEmpty();
		}

		@Test
		@DisplayName("la búsqueda del login no está disponible para un usuario autenticado")
		void loginConSesion() {
			autenticar(lector, NivelAcceso.ROLE_LECTOR);

			permisoDenegado(() -> usuarioRepository.buscarParaLogin("admin"));
		}

		@Test
		@DisplayName("el lector solo ve su registro")
		void lectorVeSoloElPropio() {
			autenticar(lector, NivelAcceso.ROLE_LECTOR);

			assertThat(usuarioRepository.findAll()).extracting(Usuario::getPublicId).containsExactly(lector);
			assertThat(usuarioRepository.findByPublicId(admin)).isEmpty();
		}

		@Test
		@DisplayName("el editor solo ve su registro")
		void editorVeSoloElPropio() {
			autenticar(editor, NivelAcceso.ROLE_EDITOR);

			assertThat(usuarioRepository.findAll()).extracting(Usuario::getPublicId).containsExactly(editor);
		}

		@Test
		@DisplayName("lector y editor no pueden insertar, modificar ni borrar")
		void lectorYEditorNoEscriben() {
			for (NivelAcceso nivel : List.of(NivelAcceso.ROLE_LECTOR, NivelAcceso.ROLE_EDITOR)) {
				UUID propio = nivel == NivelAcceso.ROLE_LECTOR ? lector : editor;
				autenticar(propio, nivel);

				permisoDenegado(() -> app.update(
						"UPDATE usuarios.usuarios SET nombre_apellido = 'x' WHERE public_id = ?", propio));
				permisoDenegado(() -> app.update("DELETE FROM usuarios.usuarios WHERE public_id = ?", propio));
				permisoDenegado(() -> app.update("""
						INSERT INTO usuarios.usuarios (public_id, nombre_usuario, nombre_apellido, email, clave, nivel_acceso)
						VALUES (?, 'nuevo', 'Nuevo', 'nuevo@neumaticos.local', 'x', 'ROLE_LECTOR')""",
						UUID.randomUUID()));
			}
		}

		@Test
		@DisplayName("el administrador ve todos los registros")
		void adminVeTodos() {
			autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);

			assertThat(usuarioRepository.findAll()).extracting(Usuario::getPublicId)
					.containsExactlyInAnyOrder(admin, editor, lector);
		}

		@Test
		@DisplayName("el administrador inserta y actualiza nombre de usuario, nombre, email, clave y nivel")
		void adminInsertaYActualiza() {
			autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);

			Usuario nuevo = usuarioRepository.save(usuarioNuevo("nuevo", NivelAcceso.ROLE_LECTOR));

			nuevo.setNombreUsuario("renombrado");
			nuevo.setNombreApellido("Usuario Renombrado");
			nuevo.setEmail("renombrado@neumaticos.local");
			nuevo.setClave("{argon2}otro-hash");
			nuevo.setNivelAcceso(NivelAcceso.ROLE_EDITOR);
			usuarioRepository.save(nuevo);

			assertThat(usuarioRepository.findByPublicId(nuevo.getPublicId())).get()
					.satisfies(usuario -> {
						assertThat(usuario.getNombreUsuario()).isEqualTo("renombrado");
						assertThat(usuario.getNombreApellido()).isEqualTo("Usuario Renombrado");
						assertThat(usuario.getEmail()).isEqualTo("renombrado@neumaticos.local");
						assertThat(usuario.getClave()).isEqualTo("{argon2}otro-hash");
						assertThat(usuario.getNivelAcceso()).isEqualTo(NivelAcceso.ROLE_EDITOR);
					});
		}

		@Test
		@DisplayName("el administrador no puede modificar el public_id")
		void adminNoModificaPublicId() {
			autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);

			permisoDenegado(() -> app.update(
					"UPDATE usuarios.usuarios SET public_id = ? WHERE public_id = ?", UUID.randomUUID(), lector));
		}

		@Test
		@DisplayName("el administrador no puede borrar usuarios")
		void adminNoBorra() {
			autenticar(admin, NivelAcceso.ROLE_ADMINISTRADOR);

			permisoDenegado(() -> app.update("DELETE FROM usuarios.usuarios WHERE public_id = ?", lector));
			permisoDenegado(() -> usuarioRepository.deleteAll());
		}

		@Test
		@DisplayName("un rol de nivel sin usuario identificado no ve nada (política restrictiva)")
		void rolSinUsuario() {
			Integer visibles = owner.execute((ConnectionCallback<Integer>) conexion -> {
				conexion.setAutoCommit(false);
				try (Statement statement = conexion.createStatement()) {
					statement.execute("SET LOCAL ROLE gn_administrador");
					try (ResultSet resultado = statement.executeQuery("SELECT count(*) FROM usuarios.usuarios")) {
						resultado.next();
						return resultado.getInt(1);
					}
				} finally {
					conexion.rollback();
				}
			});

			assertThat(visibles).isZero();
		}

	}

	@Nested
	@DisplayName("seguridad.tokens_revocados")
	class TokensRevocados {

		@Test
		@DisplayName("el logout registra el token y la validación sin sesión lo encuentra")
		void logoutYValidacion() {
			Jwt token = jwt(lector, NivelAcceso.ROLE_LECTOR);
			autenticar(lector, NivelAcceso.ROLE_LECTOR);
			tokenService.revocar(token);
			SecurityContextHolder.clearContext();

			assertThat(tokenRevocadoRepository.existsById(UUID.fromString(token.getId()))).isTrue();
		}

		@Test
		@DisplayName("sin sesión no se pueden registrar tokens")
		void sinSesionNoInserta() {
			permisoDenegado(() -> app.update(
					"INSERT INTO seguridad.tokens_revocados (jti, expira_en) VALUES (?, now())", UUID.randomUUID()));
		}

		@Test
		@DisplayName("la purga sin sesión borra solo los tokens vencidos")
		void purgaSoloVencidos() {
			UUID vencido = UUID.randomUUID();
			UUID vigente = UUID.randomUUID();
			owner.update("INSERT INTO seguridad.tokens_revocados (jti, expira_en) VALUES (?, ?), (?, ?)",
					vencido, Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
					vigente, Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

			// Aunque se pida borrar todo, la política solo permite los vencidos.
			assertThat(app.update("DELETE FROM seguridad.tokens_revocados")).isEqualTo(1);
			limpiezaTokensRevocados.eliminarExpirados();

			assertThat(owner.queryForList("SELECT jti FROM seguridad.tokens_revocados", UUID.class))
					.containsExactly(vigente);
		}

	}

	@Test
	@DisplayName("toda tabla de la app tiene RLS habilitado (denegar por defecto)")
	void todaTablaTieneRls() {
		List<String> sinRls = owner.queryForList("""
				SELECT n.nspname || '.' || c.relname
				FROM pg_class c
				JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE c.relkind IN ('r', 'p')
				  AND NOT c.relrowsecurity
				  AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
				  -- Infraestructura sin datos de negocio: historial de Flyway y
				  -- registro de eventos de Modulith (ver V6).
				  AND (n.nspname, c.relname) NOT IN (('public', 'flyway_schema_history'), ('public', 'event_publication'))
				""", String.class);

		assertThat(sinRls).as("tablas sin ROW LEVEL SECURITY").isEmpty();
	}

}
