package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.auditoria;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

class AuditorActualTest {

    private final AuditorActual auditor = new AuditorActual();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("el auditor es el publicId (sub) del JWT autenticado")
    void devuelveElSubDelToken() {
        UUID publicId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(autenticacion(publicId, NivelAcceso.ROLE_EDITOR));

        assertThat(auditor.getCurrentAuditor()).contains(publicId);
    }

    @Test
    @DisplayName("sin autenticación (proceso del sistema) no hay auditor")
    void sinAutenticacion() {
        SecurityContextHolder.clearContext();

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("una autenticación que no es un JWT no firma")
    void autenticacionQueNoEsJwt() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alguien", "x", List.of()));

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("un sub que no es un UUID no firma")
    void subInvalido() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("no-es-un-uuid").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("un token sin sub no firma")
    void subAusente() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").claim("x", "y").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }
}
