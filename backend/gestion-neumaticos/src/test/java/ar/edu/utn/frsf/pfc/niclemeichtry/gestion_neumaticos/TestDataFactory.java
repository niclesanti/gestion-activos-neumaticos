package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt.JwtProperties;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.limite.LimiteLoginProperties;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.MarcaNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Localidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Provincia;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.EjeConfiguracion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoRodado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoUnidad;

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

	// Neumáticos

	public static final Long MARCA_ID = 1L;
	public static final String NUMERO_SERIE = "ABC-123";
	/** Proveedor de compra de los tests de neumáticos (ver {@link #proveedorResumen()}). */
	public static final UUID PROVEEDOR_PUBLIC_ID = UUID.fromString("0199e8a0-0000-7000-8000-000000000001");

	public static MarcaNeumatico marca() {
		return MarcaNeumatico.builder().id(MARCA_ID).nombre("Fate").build();
	}

	public static ProveedorResumenDTO proveedorResumen() {
		return new ProveedorResumenDTO(1L, PROVEEDOR_PUBLIC_ID, "Neumáticos del Litoral S.A.");
	}

	/** Alta válida de un neumático nuevo, con número de serie y proveedor. */
	public static NeumaticoRequestDTO neumaticoNuevoRequest() {
		return new NeumaticoRequestDTO(NUMERO_SERIE, EstadoNeumatico.NUEVO, 0f, 0f, TipoEjeUnidadTransporte.TRACCION,
				TipoEjeUnidadTransporte.TRACCION, 0, "Compra inicial", MARCA_ID, PROVEEDOR_PUBLIC_ID);
	}

	/** Alta válida de un neumático usado (recapado), sin número de serie ni proveedor. */
	public static NeumaticoRequestDTO neumaticoUsadoRequest() {
		return new NeumaticoRequestDTO(null, EstadoNeumatico.RECAPADO_USADO, 150_000f, 20_000f,
				TipoEjeUnidadTransporte.DIRECCION, TipoEjeUnidadTransporte.ARRASTRE, 1, null, MARCA_ID, null);
	}

	/** Copia de {@code base} con otros valores, para casos de validación. */
	public static NeumaticoRequestDTO neumaticoRequest(String numeroSerie, EstadoNeumatico estado, Float kmTotal,
			Float kmUltimaReparacion, Integer recapados) {
		NeumaticoRequestDTO base = neumaticoNuevoRequest();
		return new NeumaticoRequestDTO(numeroSerie, estado, kmTotal, kmUltimaReparacion, base.tipoOrigen(),
				base.tipoActual(), recapados, base.comentario(), base.marcaId(), base.proveedorId());
	}

	/** Neumático persistido (con ids) a partir de un alta. */
	public static Neumatico neumatico(NeumaticoRequestDTO request) {
		return Neumatico.builder()
				.id(100L)
				.publicId(UUID.randomUUID())
				.numeroSerie(request.numeroSerie())
				.estadoActual(request.estadoActual())
				.enEspera(true)
				.kilometrajeTotal(request.kilometrajeTotal())
				.kilometrajeUltimaReparacion(request.kilometrajeUltimaReparacion())
				.tipoOrigen(request.tipoOrigen())
				.tipoActual(request.tipoActual())
				.cantidadRecapados(request.cantidadRecapados())
				.situacionOperativaVigente(SituacionOperativa.EN_ESPERA)
				.comentario(request.comentario())
				.marca(marca())
				.build();
	}

	public static NeumaticoResponseDTO neumaticoResponse(Neumatico neumatico) {
		return new NeumaticoResponseDTO(neumatico.getPublicId(), neumatico.getNumeroSerie(),
				neumatico.getEstadoActual(), neumatico.getEnEspera(), neumatico.getKilometrajeTotal(),
				neumatico.getKilometrajeUltimaReparacion(), neumatico.getTipoOrigen(), neumatico.getTipoActual(),
				neumatico.getCantidadRecapados(), neumatico.getSituacionOperativaVigente(), neumatico.getComentario(),
				neumatico.getMarca() == null ? null : neumatico.getMarca().getNombre(),
				null, null, null, null, null, null, null);
	}

	// Conductores

	public static final String DNI = "30123456";

	/** Alta válida de un conductor con DNI. */
	public static ConductorRequestDTO conductorRequest() {
		return new ConductorRequestDTO(DNI, "Juan", "Pérez");
	}

	public static ConductorRequestDTO conductorRequest(String dni, String nombre, String apellido) {
		return new ConductorRequestDTO(dni, nombre, apellido);
	}

	/** Conductor persistido (con ids) a partir de un alta. */
	public static Conductor conductor(ConductorRequestDTO request) {
		return Conductor.builder()
				.id(100L)
				.publicId(UUID.randomUUID())
				.dni(request.dni())
				.nombre(request.nombre())
				.apellido(request.apellido())
				.build();
	}

	public static ConductorResponseDTO conductorResponse(Conductor conductor) {
		return new ConductorResponseDTO(conductor.getPublicId(), conductor.getDni(), conductor.getNombre(),
				conductor.getApellido());
	}

	// Domicilios (common)

	/** Id de ejemplo de Santa Fe (en la base, buscarla por nombre). */
	public static final Long PROVINCIA_ID = 21L;

	public static Provincia provincia() {
		return Provincia.builder().id(PROVINCIA_ID).nombre("Santa Fe").build();
	}

	/** Alta válida de un domicilio completo en Santa Fe capital. */
	public static DomicilioRequestDTO domicilioRequest() {
		return new DomicilioRequestDTO("San Martín", "2550", "3", "B", PROVINCIA_ID, "Santa Fe", "3000");
	}

	public static DomicilioRequestDTO domicilioRequest(String calle, String altura, String piso, String departamento,
			Long provinciaId, String localidad, String codigoPostal) {
		return new DomicilioRequestDTO(calle, altura, piso, departamento, provinciaId, localidad, codigoPostal);
	}

	/** Domicilio persistido (con ids, localidad y provincia) a partir de un alta. */
	public static Domicilio domicilio(DomicilioRequestDTO request) {
		return Domicilio.builder()
				.id(200L)
				.calle(request.calle())
				.altura(request.altura())
				.piso(request.piso())
				.departamento(request.departamento())
				.localidad(Localidad.builder()
						.id(10L)
						.nombre(request.localidad())
						.codigoPostal(request.codigoPostal())
						.provincia(provincia())
						.build())
				.build();
	}

	public static DomicilioResponseDTO domicilioResponse(Domicilio domicilio) {
		Localidad localidad = domicilio.getLocalidad();
		return new DomicilioResponseDTO(domicilio.getCalle(), domicilio.getAltura(), domicilio.getPiso(),
				domicilio.getDepartamento(), localidad.getNombre(), localidad.getCodigoPostal(),
				localidad.getProvincia().getId(), localidad.getProvincia().getNombre());
	}

	// Proveedores

	/** Alta válida de un proveedor con contacto y domicilio. */
	public static ProveedorRequestDTO proveedorRequest() {
		return new ProveedorRequestDTO("Neumáticos del Litoral S.A.", "ventas@litoral.com.ar", "+54 342 455-1234",
				domicilioRequest());
	}

	public static ProveedorRequestDTO proveedorRequest(String nombre, String email, String telefono,
			DomicilioRequestDTO domicilio) {
		return new ProveedorRequestDTO(nombre, email, telefono, domicilio);
	}

	/** Proveedor persistido (con ids) a partir de un alta; el domicilio, como id de referencia. */
	public static ProveedorNeumaticos proveedor(ProveedorRequestDTO request) {
		return ProveedorNeumaticos.builder()
				.id(1L)
				.publicId(PROVEEDOR_PUBLIC_ID)
				.nombre(request.nombre())
				.email(request.email())
				.telefono(request.telefono())
				.domicilioId(request.domicilio() == null ? null : 200L)
				.build();
	}

	public static ProveedorResponseDTO proveedorResponse(ProveedorNeumaticos proveedor,
			DomicilioResponseDTO domicilio) {
		return new ProveedorResponseDTO(proveedor.getPublicId(), proveedor.getNombre(), proveedor.getEmail(),
				proveedor.getTelefono(), domicilio);
	}

	// Unidades de transporte

	public static final String PATENTE = "AB123CD";
	public static final Long CONFIGURACION_ID = 1L;
	public static final UUID UNIDAD_PUBLIC_ID = UUID.fromString("01928f3e-7b1a-7c2d-9e4f-5a6b7c8d9e0f");

	public static UnidadTransporteRequestDTO unidadTransporteRequest() {
		return new UnidadTransporteRequestDTO(PATENTE, CONFIGURACION_ID);
	}

	public static UnidadTransporteRequestDTO unidadTransporteRequest(String patente, Long configuracionId) {
		return new UnidadTransporteRequestDTO(patente, configuracionId);
	}

	/** Config 1 de V16: E1 DIRECCION SIMPLE, E2 TRACCION DUAL y un auxilio (7 slots). */
	public static ConfiguracionUnidad tractorComun() {
		return configuracion(1L, "Tractor común", TipoUnidad.TRACTOR, 1,
				eje(1, TipoEje.DIRECCION, TipoRodado.SIMPLE), eje(2, TipoEje.TRACCION, TipoRodado.DUAL));
	}

	/** Config 2 de V16: E1 DIRECCION SIMPLE, E2 y E3 TRACCION DUAL y un auxilio (11 slots). */
	public static ConfiguracionUnidad tractorPesado() {
		return configuracion(2L, "Tractor para cargas pesadas", TipoUnidad.TRACTOR, 1,
				eje(1, TipoEje.DIRECCION, TipoRodado.SIMPLE), eje(2, TipoEje.TRACCION, TipoRodado.DUAL),
				eje(3, TipoEje.TRACCION, TipoRodado.DUAL));
	}

	/** Config 3 de V16: E1 a E3 ARRASTRE DUAL y un auxilio (13 slots). */
	public static ConfiguracionUnidad semirremolque() {
		return configuracion(3L, "Semirremolque", TipoUnidad.SEMIRREMOLQUE, 1,
				eje(1, TipoEje.ARRASTRE, TipoRodado.DUAL), eje(2, TipoEje.ARRASTRE, TipoRodado.DUAL),
				eje(3, TipoEje.ARRASTRE, TipoRodado.DUAL));
	}

	/** Configuración con sus ejes enlazados en ambos sentidos. */
	public static ConfiguracionUnidad configuracion(Long id, String nombre, TipoUnidad tipoUnidad,
			int cantSlotsAuxilio, EjeConfiguracion... ejes) {
		ConfiguracionUnidad configuracion = ConfiguracionUnidad.builder()
				.id(id)
				.nombre(nombre)
				.tipoUnidad(tipoUnidad)
				.cantSlotsAuxilio(cantSlotsAuxilio)
				.ejes(new ArrayList<>(List.of(ejes)))
				.build();
		for (EjeConfiguracion eje : ejes) {
			eje.setConfiguracion(configuracion);
		}
		return configuracion;
	}

	public static EjeConfiguracion eje(int orden, TipoEje tipoEje, TipoRodado rodado) {
		return EjeConfiguracion.builder()
				.id((long) orden)
				.orden(orden)
				.tipoEje(tipoEje)
				.rodado(rodado)
				.build();
	}

	// Límite de intentos de login

	/** Límites chicos para probar: 3 intentos por IP por minuto, 2 fallos por cuenta cada 10 minutos. */
	public static LimiteLoginProperties limiteLogin() {
		return new LimiteLoginProperties(3, Duration.ofMinutes(1), 2, Duration.ofMinutes(10), 1_000);
	}

}
