package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lleva el usuario autenticado hasta PostgreSQL. En cada conexión que se toma
 * del pool fija:
 * <ul>
 * <li>{@code role}: el rol de base del nivel de acceso ({@code SET ROLE}), que
 * define los GRANT disponibles.</li>
 * <li>{@code app.usuario_id}: el {@code publicId} del JWT, que las políticas RLS
 * leen con {@code seguridad.usuario_actual()}.</li>
 * </ul>
 * Sin usuario autenticado (login, validación del token, tareas programadas) se
 * vuelve al rol de conexión {@code gn_app}, que casi no tiene permisos. Como
 * cada checkout sobrescribe los dos valores, una conexión reciclada nunca
 * arrastra la identidad del usuario anterior.
 *
 * <p>Los valores se fijan a nivel de sesión fuera de toda transacción (el pool
 * entrega las conexiones en autocommit), así que un rollback posterior no los
 * revierte.
 */
class RolBaseDatosDataSource extends DelegatingDataSource {

	/** Lista blanca: un nivel desconocido nunca llega a la base como rol. */
	private static final Map<String, String> ROLES = Map.of(
			"ROLE_ADMINISTRADOR", "gn_administrador",
			"ROLE_EDITOR", "gn_editor",
			"ROLE_LECTOR", "gn_lector");

	/** {@code SET ROLE NONE}: vuelve al rol de la conexión. */
	private static final String SIN_ROL = "none";

	private static final String FIJAR_SESION =
			"SELECT set_config('role', ?, false), set_config('app.usuario_id', ?, false)";

	RolBaseDatosDataSource(DataSource destino) {
		super(destino);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return fijarSesion(super.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return fijarSesion(super.getConnection(username, password));
	}

	private static Connection fijarSesion(Connection conexion) throws SQLException {
		SesionBaseDatos sesion = sesionActual();
		try (PreparedStatement statement = conexion.prepareStatement(FIJAR_SESION)) {
			statement.setString(1, sesion.rol());
			statement.setString(2, sesion.usuarioId());
			statement.execute();
		} catch (SQLException ex) {
			conexion.close();
			throw ex;
		}
		return conexion;
	}

	private static SesionBaseDatos sesionActual() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			return SesionBaseDatos.ANONIMA;
		}
		Jwt jwt = token.getToken();
		// El nivel sale de las authorities, que el resource server ya cargó desde
		// la base (AutoridadesVigentesConverter), y no del claim del token.
		List<String> roles = token.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(ROLES::get)
				.filter(Objects::nonNull)
				.toList();
		if (roles.size() != 1 || jwt.getSubject() == null) {
			return SesionBaseDatos.ANONIMA;
		}
		try {
			return new SesionBaseDatos(roles.getFirst(), UUID.fromString(jwt.getSubject()).toString());
		} catch (IllegalArgumentException ex) {
			return SesionBaseDatos.ANONIMA;
		}
	}

	private record SesionBaseDatos(String rol, String usuarioId) {

		static final SesionBaseDatos ANONIMA = new SesionBaseDatos(SIN_ROL, "");

	}

}
