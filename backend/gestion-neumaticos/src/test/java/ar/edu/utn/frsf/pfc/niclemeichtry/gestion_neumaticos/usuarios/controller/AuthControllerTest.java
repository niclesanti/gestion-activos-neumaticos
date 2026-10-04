package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.TOKEN;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.loginRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.loginResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.usuarioSesionAdministrador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.CredencialesInvalidasException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.DemasiadosIntentosException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ServicioSaturadoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service.AuthService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Corre con la cadena de filtros de seguridad real ({@link SecurityConfig}): así
 * se prueba también qué endpoints son públicos y cómo se responde un 401.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
@AutoConfigureRestDocs
class AuthControllerTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String ME = "/api/auth/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private NivelAccesoVigente nivelAccesoVigente;

    private final UsuarioSesionDTO usuario = usuarioSesionAdministrador();

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("200 con token y usuario ante credenciales válidas (endpoint público)")
        void loginExitoso() throws Exception {
            LoginRequestDTO request = loginRequest();
            when(authService.login(eq(request), anyString())).thenReturn(loginResponse(usuario));

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.usuario.nombreUsuario").value("administrador"))
                    .andExpect(jsonPath("$.usuario.nivelAcceso").value("ROLE_ADMINISTRADOR"))
                    .andExpect(jsonPath("$.usuario.id").doesNotExist())
                    .andDo(document("auth-login",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("identifier").description("Correo electrónico o nombre de usuario"),
                                    fieldWithPath("password").description("Contraseña")),
                            responseFields(
                                    fieldWithPath("token").description("JWT para el header Authorization"),
                                    fieldWithPath("tipo").description("Tipo de token (Bearer)"),
                                    fieldWithPath("expiraEn").description("Expiración del token (UTC)"),
                                    fieldWithPath("usuario.publicId").description("Identificador público"),
                                    fieldWithPath("usuario.nombreUsuario").description("Nombre de usuario"),
                                    fieldWithPath("usuario.nombreApellido").description("Nombre y apellido"),
                                    fieldWithPath("usuario.email").description("Correo electrónico"),
                                    fieldWithPath("usuario.nivelAcceso").description("Nivel de acceso"))));
        }

        @Test
        @DisplayName("ignora un token en el header: el login siempre corre sin usuario")
        void ignoraTokenEnElHeader() throws Exception {
            LoginRequestDTO request = loginRequest();
            when(authService.login(eq(request), anyString())).thenReturn(loginResponse(usuario));

            mockMvc.perform(post(LOGIN).header(HttpHeaders.AUTHORIZATION, "Bearer token.viejo.revocado")
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk());

            verifyNoInteractions(jwtDecoder);
        }

        @Test
        @DisplayName("401 con mensaje genérico ante credenciales inválidas")
        void credencialesInvalidas() throws Exception {
            when(authService.login(any(), anyString())).thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                            .content(json(loginRequest("noexiste", "cualquiera"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                    .andExpect(jsonPath("$.status").value(401));
        }

        static Stream<Arguments> requestsInvalidos() {
            return Stream.of(
                    Arguments.of("identificador vacío", "", "clave", "identifier"),
                    Arguments.of("identificador en blanco", "   ", "clave", "identifier"),
                    Arguments.of("identificador nulo", null, "clave", "identifier"),
                    Arguments.of("identificador con espacios internos", "juan perez", "clave", "identifier"),
                    Arguments.of("email mal formado", "juan@", "clave", "identifier"),
                    Arguments.of("usuario demasiado corto", "ab", "clave", "identifier"),
                    Arguments.of("identificador de más de 254", "a".repeat(250) + "@x.com", "clave", "identifier"),
                    Arguments.of("contraseña vacía", "administrador", "", "password"),
                    Arguments.of("contraseña nula", "administrador", null, "password"),
                    Arguments.of("contraseña de más de 128", "administrador", "x".repeat(129), "password"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("requestsInvalidos")
        @DisplayName("400 sin consultar el servicio ante campos faltantes o inválidos")
        void validaElRequest(String caso, String identifier, String password, String campo) throws Exception {
            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                            .content(json(loginRequest(identifier, password))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(campo + ":")));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("una contraseña de solo espacios es válida (no se recorta)")
        void claveDeEspaciosEsValida() throws Exception {
            when(authService.login(any(), anyString())).thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                            .content(json(loginRequest("administrador", "   "))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("pasa al servicio la IP del cliente")
        void pasaLaIpDelCliente() throws Exception {
            LoginRequestDTO request = loginRequest();
            when(authService.login(eq(request), anyString())).thenReturn(loginResponse(usuario));

            mockMvc.perform(post(LOGIN).with(req -> {
                        req.setRemoteAddr("203.0.113.99");
                        return req;
                    }).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk());

            verify(authService).login(request, "203.0.113.99");
        }

        @Test
        @DisplayName("429 con Retry-After al superar el límite de intentos")
        void demasiadosIntentos() throws Exception {
            when(authService.login(any(), anyString()))
                    .thenThrow(new DemasiadosIntentosException(Duration.ofSeconds(90)));

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(loginRequest())))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string(HttpHeaders.RETRY_AFTER, "90"))
                    .andExpect(jsonPath("$.message").value(DemasiadosIntentosException.MENSAJE))
                    .andExpect(jsonPath("$.status").value(429));
        }

        @Test
        @DisplayName("503 con Retry-After si el hash de contraseñas está saturado")
        void servicioSaturado() throws Exception {
            when(authService.login(any(), anyString()))
                    .thenThrow(new ServicioSaturadoException(Duration.ofMillis(1500)));

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(loginRequest())))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                    .andExpect(jsonPath("$.status").value(503));
        }

        @Test
        @DisplayName("415 (no 500) ante un Content-Type que no es JSON")
        void contentTypeNoSoportado() throws Exception {
            mockMvc.perform(post(LOGIN).contentType(MediaType.TEXT_PLAIN).content("identifier=admin"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.message").value("Tipo de contenido no soportado"))
                    .andExpect(jsonPath("$.status").value(415));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("responde con las cabeceras de seguridad")
        void cabecerasDeSeguridad() throws Exception {
            when(authService.login(any(), anyString())).thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(loginRequest())))
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.containsString("default-src 'none'")))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                            org.hamcrest.Matchers.containsString("no-store")));
        }

        @Test
        @DisplayName("HSTS solo sobre HTTPS")
        void hstsSobreHttps() throws Exception {
            when(authService.login(any(), anyString())).thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(post(LOGIN).secure(true).contentType(MediaType.APPLICATION_JSON)
                            .content(json(loginRequest())))
                    .andExpect(header().string("Strict-Transport-Security",
                            org.hamcrest.Matchers.containsString("max-age=31536000")));
        }

        @Test
        @DisplayName("400 ante un cuerpo mal formado")
        void cuerpoMalFormado() throws Exception {
            mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{no-es-json"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("204 y revoca el token de la sesión")
        void logoutExitoso() throws Exception {
            mockMvc.perform(post(LOGOUT).with(jwt().jwt(j -> j.subject(usuario.publicId().toString()))))
                    .andExpect(status().isNoContent());

            verify(authService).logout(any(Jwt.class));
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(LOGOUT))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.status").value(401));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("401 con un token revocado, inválido o expirado")
        void tokenRechazado() throws Exception {
            when(jwtDecoder.decode("token-revocado")).thenThrow(new BadJwtException("El token fue revocado"));

            mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Bearer token-revocado"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("No autenticado o sesión inválida"));

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("200 con el usuario del token válido")
        void usuarioActual() throws Exception {
            Jwt token = TestDataFactory.jwt(usuario.publicId(), NivelAcceso.ROLE_ADMINISTRADOR);
            when(jwtDecoder.decode("token-valido")).thenReturn(token);
            when(nivelAccesoVigente.buscar(usuario.publicId())).thenReturn(Optional.of("ROLE_ADMINISTRADOR"));
            when(authService.usuarioActual(any(Jwt.class))).thenReturn(usuario);

            mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer token-valido"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicId").value(usuario.publicId().toString()))
                    .andExpect(jsonPath("$.nivelAcceso").value("ROLE_ADMINISTRADOR"));

            // El controller recibe el token que validó el resource server.
            ArgumentCaptor<Jwt> recibido = ArgumentCaptor.forClass(Jwt.class);
            verify(authService).usuarioActual(recibido.capture());
            assertThat(recibido.getValue().getSubject()).isEqualTo(usuario.publicId().toString());
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 con un token válido si el usuario ya no está habilitado en la base")
        void usuarioDadoDeBajaConTokenVigente() throws Exception {
            Jwt token = TestDataFactory.jwt(usuario.publicId(), NivelAcceso.ROLE_ADMINISTRADOR);
            when(jwtDecoder.decode("token-valido")).thenReturn(token);
            when(nivelAccesoVigente.buscar(usuario.publicId())).thenReturn(Optional.empty());

            mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer token-valido"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("405 (no 500) con un método no soportado")
        void metodoNoSoportado() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(ME).with(jwt()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "GET"))
                    .andExpect(jsonPath("$.status").value(405));
        }

        @Test
        @DisplayName("401 si el usuario del token ya no existe")
        void usuarioEliminado() throws Exception {
            when(authService.usuarioActual(any())).thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(get(ME).with(jwt()))
                    .andExpect(status().isUnauthorized());
        }
    }

}
