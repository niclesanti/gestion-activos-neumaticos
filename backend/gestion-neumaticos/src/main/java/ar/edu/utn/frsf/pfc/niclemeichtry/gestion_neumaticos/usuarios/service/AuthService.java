package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import org.springframework.security.oauth2.jwt.Jwt;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.LoginResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;

public interface AuthService {

    /**
     * Valida las credenciales y emite un token de sesión.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.CredencialesInvalidasException
     *         si el usuario no existe o la contraseña no coincide (mismo error en ambos casos).
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /** Invalida el token de la sesión actual. */
    void logout(Jwt jwt);

    /** Usuario dueño del token de la sesión actual. */
    UsuarioSesionDTO usuarioActual(Jwt jwt);

}
