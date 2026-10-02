package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.CredencialesInvalidasException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenEmitido;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UsuarioMapper usuarioMapper;

    /**
     * Hash de una contraseña que nadie conoce. Si el usuario no existe se lo
     * compara igual, para que la respuesta tarde lo mismo que con un usuario
     * real y no se pueda deducir por tiempos qué cuentas existen.
     */
    private final String hashDummy;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            TokenService tokenService, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.usuarioMapper = usuarioMapper;
        this.hashDummy = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        String identificador = request.identifier().strip().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuario = usuarioRepository.findByNombreUsuarioOrEmail(identificador, identificador);

        if (usuario.isEmpty()) {
            passwordEncoder.matches(request.password(), hashDummy);
            log.info("Inicio de sesión rechazado: credenciales inválidas");
            throw new CredencialesInvalidasException();
        }
        if (!passwordEncoder.matches(request.password(), usuario.get().getClave())) {
            log.info("Inicio de sesión rechazado: credenciales inválidas");
            throw new CredencialesInvalidasException();
        }

        Usuario autenticado = usuario.get();
        TokenEmitido token = tokenService.generar(autenticado.getPublicId(), autenticado.getNivelAcceso().name());
        log.info("Inicio de sesión exitoso del usuario {}", autenticado.getPublicId());

        return new LoginResponseDTO(token.token(), LoginResponseDTO.TIPO_BEARER, token.expiraEn(),
                usuarioMapper.toSesionDTO(autenticado));
    }

    @Override
    @Transactional
    public void logout(Jwt jwt) {
        tokenService.revocar(jwt);
        log.info("Cierre de sesión del usuario {}", jwt.getSubject());
    }

    @Override
    public UsuarioSesionDTO usuarioActual(Jwt jwt) {
        // Un token válido de un usuario dado de baja deja de servir.
        return usuarioRepository.findByPublicId(UUID.fromString(jwt.getSubject()))
                .map(usuarioMapper::toSesionDTO)
                .orElseThrow(CredencialesInvalidasException::new);
    }

}
