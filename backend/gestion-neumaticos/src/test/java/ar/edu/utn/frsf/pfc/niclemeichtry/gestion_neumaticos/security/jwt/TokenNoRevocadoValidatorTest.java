package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocadoRepository;

@ExtendWith(MockitoExtension.class)
class TokenNoRevocadoValidatorTest {

    @Mock
    private TokenRevocadoRepository tokenRevocadoRepository;

    @InjectMocks
    private TokenNoRevocadoValidator validator;

    @Test
    void aceptaUnTokenNoRevocado() {
        UUID jti = UUID.randomUUID();
        when(tokenRevocadoRepository.existsById(jti)).thenReturn(false);

        assertThat(validator.validate(jwtConJti(jti.toString())).hasErrors()).isFalse();
    }

    @Test
    void rechazaUnTokenRevocado() {
        UUID jti = UUID.randomUUID();
        when(tokenRevocadoRepository.existsById(jti)).thenReturn(true);

        assertThat(validator.validate(jwtConJti(jti.toString())).hasErrors()).isTrue();
    }

    @Test
    void rechazaUnTokenSinJti() {
        assertThat(validator.validate(jwtConJti(null)).hasErrors()).isTrue();
        verifyNoInteractions(tokenRevocadoRepository);
    }

    @Test
    void rechazaUnJtiQueNoEsUuid() {
        assertThat(validator.validate(jwtConJti("no-es-un-uuid")).hasErrors()).isTrue();
        verifyNoInteractions(tokenRevocadoRepository);
    }

    private static Jwt jwtConJti(String jti) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject("sub");
        if (jti != null) {
            builder.jti(jti);
        }
        return builder.build();
    }

}
