package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.auditoria;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Usuario que firma las columnas {@code created_by}/{@code updated_by}: el
 * {@code publicId} del JWT autenticado, el mismo valor con que
 * {@code RolBaseDatosDataSource} identifica la sesión en la base de datos. Sin
 * usuario autenticado (login, procesos del sistema por {@code ComoSistema}) no hay
 * auditor y la columna queda en {@code NULL}.
 */
@Component
class AuditorActual implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            try {
                return Optional.of(UUID.fromString(token.getToken().getSubject()));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
