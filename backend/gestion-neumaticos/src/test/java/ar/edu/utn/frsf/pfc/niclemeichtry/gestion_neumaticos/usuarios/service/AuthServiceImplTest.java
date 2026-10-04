package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.CredencialesInvalidasException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenEmitido;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String HASH_DUMMY = "{argon2}dummy";
    private static final String HASH_USUARIO = "{argon2}hash-del-usuario";
    private static final String CLAVE = "Admin.1234";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private UsuarioMapper usuarioMapper;

    private AuthServiceImpl authService;
    private Usuario usuario;
    private UsuarioSesionDTO usuarioSesion;

    @BeforeEach
    void setUp() {
        // El constructor precalcula el hash dummy con el encoder.
        when(passwordEncoder.encode(anyString())).thenReturn(HASH_DUMMY);
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, tokenService, usuarioMapper);

        usuario = Usuario.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .nombreUsuario("administrador")
                .nombreApellido("Ana Administradora")
                .email("administrador@neumaticos.local")
                .clave(HASH_USUARIO)
                .nivelAcceso(NivelAcceso.ROLE_ADMINISTRADOR)
                .build();
        usuarioSesion = new UsuarioSesionDTO(usuario.getPublicId(), usuario.getNombreUsuario(),
                usuario.getNombreApellido(), usuario.getEmail(), usuario.getNivelAcceso());
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("con nombre de usuario y clave correctos emite el token y devuelve el usuario")
        void loginPorNombreUsuario() {
            Instant expiraEn = Instant.parse("2026-10-01T20:00:00Z");
            when(usuarioRepository.buscarParaLogin("administrador"))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_USUARIO)).thenReturn(true);
            when(tokenService.generar(usuario.getPublicId(), "ROLE_ADMINISTRADOR"))
                    .thenReturn(new TokenEmitido("jwt", expiraEn));
            when(usuarioMapper.toSesionDTO(usuario)).thenReturn(usuarioSesion);

            LoginResponseDTO respuesta = authService.login(new LoginRequestDTO("administrador", CLAVE));

            assertThat(respuesta.token()).isEqualTo("jwt");
            assertThat(respuesta.tipo()).isEqualTo("Bearer");
            assertThat(respuesta.expiraEn()).isEqualTo(expiraEn);
            assertThat(respuesta.usuario()).isEqualTo(usuarioSesion);
        }

        @Test
        @DisplayName("acepta el email como identificador")
        void loginPorEmail() {
            String email = "administrador@neumaticos.local";
            when(usuarioRepository.buscarParaLogin(email)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_USUARIO)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));
            when(usuarioMapper.toSesionDTO(usuario)).thenReturn(usuarioSesion);

            LoginResponseDTO respuesta = authService.login(new LoginRequestDTO(email, CLAVE));

            assertThat(respuesta.usuario().email()).isEqualTo(email);
        }

        @Test
        @DisplayName("normaliza el identificador a minúsculas y sin espacios antes de buscar")
        void normalizaIdentificador() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_USUARIO)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));

            authService.login(new LoginRequestDTO("  Administrador@Neumaticos.LOCAL ", CLAVE));

            verify(usuarioRepository).buscarParaLogin("administrador@neumaticos.local");
        }

        @Test
        @DisplayName("no recorta la contraseña: los espacios son parte de ella")
        void noRecortaLaClave() {
            String claveConEspacios = " " + CLAVE + " ";
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(claveConEspacios, HASH_USUARIO)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequestDTO("administrador", claveConEspacios)))
                    .isInstanceOf(CredencialesInvalidasException.class);
            verify(passwordEncoder).matches(claveConEspacios, HASH_USUARIO);
        }

        @Test
        @DisplayName("usuario inexistente: error genérico y compara contra el hash dummy (tiempo constante)")
        void usuarioInexistente() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequestDTO("noexiste", CLAVE)))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage(CredencialesInvalidasException.MENSAJE);

            verify(passwordEncoder).matches(CLAVE, HASH_DUMMY);
            verify(tokenService, never()).generar(any(), anyString());
        }

        @Test
        @DisplayName("clave incorrecta: mismo error genérico que el usuario inexistente")
        void claveIncorrecta() {
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("otra", HASH_USUARIO)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequestDTO("administrador", "otra")))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage(CredencialesInvalidasException.MENSAJE);

            verify(tokenService, never()).generar(any(), anyString());
            verify(usuarioMapper, never()).toSesionDTO(any());
        }

        @Test
        @DisplayName("el token lleva el nivel de acceso del usuario")
        void tokenConNivelDeAcceso() {
            usuario.setNivelAcceso(NivelAcceso.ROLE_EDITOR);
            when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(CLAVE, HASH_USUARIO)).thenReturn(true);
            when(tokenService.generar(any(), anyString())).thenReturn(new TokenEmitido("jwt", Instant.now()));

            authService.login(new LoginRequestDTO("administrador", CLAVE));

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

    private static Jwt jwt(UUID subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject.toString())
                .jti(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

}
