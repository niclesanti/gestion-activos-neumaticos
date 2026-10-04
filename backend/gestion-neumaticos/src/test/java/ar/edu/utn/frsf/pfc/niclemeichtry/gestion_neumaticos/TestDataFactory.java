package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt.JwtProperties;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.limite.LimiteLoginProperties;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;

/**
 * Datos de prueba compartidos por todos los tests: cada método devuelve una
 * instancia nueva, así un test puede modificarla sin afectar a otro.
 */
public final class TestDataFactory {

	public static final String CLAVE = "Admin.1234";
	public static final String HASH_CLAVE = "{argon2}hash-de-prueba";
	/** Secreto JWT en base64, como se configura `JWT_SECRET` (43 bytes decodificados). */
	public static final String SECRETO_JWT = secretoBase64("un-secreto-de-prueba-de-al-menos-32-bytes!!");
	public static final String ISSUER = "gestion-neumaticos";
	public static final String AUDIENCE = "gestion-neumaticos-api";
	public static final Duration EXPIRACION_TOKEN = Duration.ofHours(2);
	public static final String TOKEN = "eyJ.token.firma";
	/** IP de cliente de ejemplo (rango de documentación, RFC 5737). */
	public static final String IP = "203.0.113.10";
	public static final Instant EXPIRACION = Instant.parse("2026-10-01T20:00:00Z");

	private TestDataFactory() {
	}

	// Usuarios

	/** Usuario con nombre, email y nombre completo derivados del nivel (ej. {@code editor@neumaticos.local}). */
	public static Usuario usuario(NivelAcceso nivel) {
		String nombre = nombreUsuario(nivel);
		return Usuario.builder()
				.id(1L)
				.publicId(UUID.randomUUID())
				.nombreUsuario(nombre)
				.nombreApellido(nombreApellido(nivel))
				.email(nombre + "@neumaticos.local")
				.clave(HASH_CLAVE)
				.nivelAcceso(nivel)
				.build();
	}

	/** Usuario sin {@code id} ni {@code publicId}, listo para persistir (los genera la base/Hibernate). */
	public static Usuario usuarioNuevo(String nombreUsuario, NivelAcceso nivel) {
		return Usuario.builder()
				.nombreUsuario(nombreUsuario)
				.nombreApellido("Usuario " + nombreUsuario)
				.email(nombreUsuario + "@neumaticos.local")
				.clave(HASH_CLAVE)
				.nivelAcceso(nivel)
				.build();
	}

	public static Usuario administrador() {
		return usuario(NivelAcceso.ROLE_ADMINISTRADOR);
	}

	public static Usuario editor() {
		return usuario(NivelAcceso.ROLE_EDITOR);
	}

	public static Usuario lector() {
		return usuario(NivelAcceso.ROLE_LECTOR);
	}

	public static String nombreUsuario(NivelAcceso nivel) {
		return switch (nivel) {
			case ROLE_ADMINISTRADOR -> "administrador";
			case ROLE_EDITOR -> "editor";
			case ROLE_LECTOR -> "lector";
		};
	}

	private static String nombreApellido(NivelAcceso nivel) {
		return switch (nivel) {
			case ROLE_ADMINISTRADOR -> "Ana Administradora";
			case ROLE_EDITOR -> "Eduardo Editor";
			case ROLE_LECTOR -> "Lucía Lectora";
		};
	}

	// DTOs

	public static UsuarioSesionDTO usuarioSesion(Usuario usuario) {
		return new UsuarioSesionDTO(usuario.getPublicId(), usuario.getNombreUsuario(), usuario.getNombreApellido(),
				usuario.getEmail(), usuario.getNivelAcceso());
	}

	public static UsuarioSesionDTO usuarioSesionAdministrador() {
		return usuarioSesion(administrador());
	}

	public static LoginRequestDTO loginRequest() {
		return loginRequest("administrador", CLAVE);
	}

	public static LoginRequestDTO loginRequest(String identifier, String password) {
		return new LoginRequestDTO(identifier, password);
	}

	public static LoginResponseDTO loginResponse(UsuarioSesionDTO usuario) {
		return new LoginResponseDTO(TOKEN, LoginResponseDTO.TIPO_BEARER, EXPIRACION, usuario);
	}

	// JWT

	/** Base de un JWT vigente por una hora, con {@code jti} propio: completar con sub, claims, etc. */
	public static Jwt.Builder jwtBuilder() {
		Instant ahora = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.jti(UUID.randomUUID().toString())
				.issuedAt(ahora)
				.expiresAt(ahora.plus(1, ChronoUnit.HOURS));
	}

	public static Jwt jwt(UUID publicId) {
		return jwtBuilder().subject(publicId.toString()).build();
	}

	/** JWT como lo emite el login: con el nivel de acceso en el claim {@code nivelAcceso}. */
	public static Jwt jwt(UUID publicId, NivelAcceso nivel) {
		return jwtBuilder()
				.subject(publicId.toString())
				.claim(TokenService.CLAIM_NIVEL_ACCESO, nivel.name())
				.build();
	}

	/** JWT con el {@code jti} indicado, o sin {@code jti} si es {@code null}. */
	public static Jwt jwtConJti(String jti) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject("sub");
		if (jti != null) {
			builder.jti(jti);
		}
		return builder.build();
	}

	/** Autenticación como la deja el resource server en el {@code SecurityContext}. */
	public static JwtAuthenticationToken autenticacion(UUID publicId, NivelAcceso nivel) {
		return new JwtAuthenticationToken(jwt(publicId, nivel), List.of(new SimpleGrantedAuthority(nivel.name())));
	}

	public static JwtProperties jwtProperties() {
		return jwtProperties(SECRETO_JWT);
	}

	public static JwtProperties jwtProperties(String secreto) {
		return new JwtProperties(secreto, EXPIRACION_TOKEN, ISSUER, AUDIENCE);
	}

	/** Codifica en base64 los bytes UTF-8 del texto, como hay que pasar el secreto JWT. */
	public static String secretoBase64(String texto) {
		return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	// Límite de intentos de login

	/** Límites chicos para probar: 3 intentos por IP por minuto, 2 fallos por cuenta cada 10 minutos. */
	public static LimiteLoginProperties limiteLogin() {
		return new LimiteLoginProperties(3, Duration.ofMinutes(1), 2, Duration.ofMinutes(10), 1_000);
	}

}
