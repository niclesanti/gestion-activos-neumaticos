package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.EXPIRACION;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.HASH_CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.IP;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.administrador;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwt;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.loginRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.usuarioSesion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.CredencialesInvalidasException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.DemasiadosIntentosException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.LimitadorIntentosLogin;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenEmitido;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String HASH_DUMMY = "{argon2}dummy";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private LimitadorIntentosLogin limitador;

    private final Usuario usuario = administrador();
    private final UsuarioSesionDTO usuarioSesion = usuarioSesion(usuario);

    private AuthServiceImpl authService;

    /**
     * No arma datos (vienen de {@link TestDataFactory}): el constructor precalcula
     * el hash dummy con el encoder, así que el stub tiene que existir antes.
     */
    @BeforeEach
    void crearServicio() {
        when(passwordEncoder.encode(anyString())).thenReturn(HASH_DUMMY);
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, tokenService, usuarioMapper, limitador);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("con nombre de usuario y clave correctos emite el token y devuelve el usuario")
        void loginPorNombreUsuario() {
            when(usuarioRepository.buscarParaLogin("administrador"))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_CLAVE)).thenReturn(true);
            when(tokenService.generar(usuario.getPublicId(), "ROLE_ADMINISTRADOR"))
                    .thenReturn(new TokenEmitido("jwt", EXPIRACION));
            when(usuarioMapper.toSesionDTO(usuario)).thenReturn(usuarioSesion);

            LoginResponseDTO respuesta = authService.login(loginRequest(), IP);

            assertThat(respuesta.token()).isEqualTo("jwt");
            assertThat(respuesta.tipo()).isEqualTo("Bearer");
            assertThat(respuesta.expiraEn()).isEqualTo(EXPIRACION);
            assertThat(respuesta.usuario()).isEqualTo(usuarioSesion);
        }

        @Test
        @DisplayName("acepta el email como identificador")
        void loginPorEmail() {
            String email = usuario.getEmail();
            when(usuarioRepository.buscarParaLogin(email)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_CLAVE)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));
            when(usuarioMapper.toSesionDTO(usuario)).thenReturn(usuarioSesion);

            LoginResponseDTO respuesta = authService.login(loginRequest(email, CLAVE), IP);

            assertThat(respuesta.usuario().email()).isEqualTo(email);
        }

        @Test
        @DisplayName("normaliza el identificador a minúsculas y sin espacios antes de buscar")
        void normalizaIdentificador() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_CLAVE)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));

            authService.login(loginRequest("  Administrador@Neumaticos.LOCAL ", CLAVE), IP);

            verify(usuarioRepository).buscarParaLogin("administrador@neumaticos.local");
        }

        @Test
        @DisplayName("no recorta la contraseña: los espacios son parte de ella")
        void noRecortaLaClave() {
            String claveConEspacios = " " + CLAVE + " ";
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(claveConEspacios, HASH_CLAVE)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest("administrador", claveConEspacios), IP))
                    .isInstanceOf(CredencialesInvalidasException.class);
            verify(passwordEncoder).matches(claveConEspacios, HASH_CLAVE);
        }

        @Test
        @DisplayName("usuario inexistente: error genérico y compara contra el hash dummy (tiempo constante)")
        void usuarioInexistente() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest("noexiste", CLAVE), IP))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage(CredencialesInvalidasException.MENSAJE);

            verify(passwordEncoder).matches(CLAVE, HASH_DUMMY);
            verify(tokenService, never()).generar(any(), anyString());
        }

        @Test
        @DisplayName("clave incorrecta: mismo error genérico que el usuario inexistente")
        void claveIncorrecta() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("otra", HASH_CLAVE)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest("administrador", "otra"), IP))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage(CredencialesInvalidasException.MENSAJE);

            verify(tokenService, never()).generar(any(), anyString());
            verify(usuarioMapper, never()).toSesionDTO(any());
        }

        @Test
        @DisplayName("bloqueado por el límite de intentos: no busca al usuario ni calcula el hash")
        void bloqueadoPorLimiteDeIntentos() {
            doThrow(new DemasiadosIntentosException(Duration.ofMinutes(3)))
                    .when(limitador).verificar(IP, "administrador");

            assertThatThrownBy(() -> authService.login(loginRequest(), IP))
                    .isInstanceOf(DemasiadosIntentosException.class);

            verifyNoInteractions(usuarioRepository, tokenService);
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("el intento se cuenta para el identificador normalizado, exista o no la cuenta")
        void registraElFallo() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest(" NoExiste ", CLAVE), IP))
                    .isInstanceOf(CredencialesInvalidasException.class);

            verify(limitador).verificar(IP, "noexiste");
            verify(limitador, never()).registrarExito(anyString());
        }

        @Test
        @DisplayName("un login correcto devuelve los intentos de la cuenta")
        void registraElExito() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_CLAVE)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));

            authService.login(loginRequest(), IP);

            verify(limitador).registrarExito("administrador");
        }

        @Test
        @DisplayName("el token lleva el nivel de acceso del usuario")
        void tokenConNivelDeAcceso() {
            usuario.setNivelAcceso(NivelAcceso.ROLE_EDITOR);
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_CLAVE)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));

            authService.login(loginRequest(), IP);

            ArgumentCaptor<String> nivel = ArgumentCaptor.forClass(String.class);
            verify(tokenService).generar(eq(usuario.getPublicId()), nivel.capture());
            assertThat(nivel.getValue()).isEqualTo("ROLE_EDITOR");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revoca el token de la sesión")
        void revocaElToken() {
            Jwt jwt = jwt(usuario.getPublicId());

            authService.logout(jwt);

            verify(tokenService).revocar(jwt);
        }
    }

    @Nested
    @DisplayName("usuarioActual")
    class UsuarioActual {

        @Test
        @DisplayName("devuelve el usuario dueño del token")
        void devuelveElUsuario() {
            when(usuarioRepository.findByPublicId(usuario.getPublicId())).thenReturn(Optional.of(usuario));
            when(usuarioMapper.toSesionDTO(usuario)).thenReturn(usuarioSesion);

            assertThat(authService.usuarioActual(jwt(usuario.getPublicId()))).isEqualTo(usuarioSesion);
        }

        @Test
        @DisplayName("si el usuario ya no existe, la sesión deja de ser válida")
        void usuarioEliminado() {
            UUID publicId = UUID.randomUUID();
            when(usuarioRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.usuarioActual(jwt(publicId)))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
    }

}
