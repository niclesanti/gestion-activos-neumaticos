package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtBuilder;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenEmitido;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocadoRepository;

/**
 * Usa el encoder y el decoder reales de {@link JwtConfig}: verifica que lo que
 * se firma en el login es exactamente lo que acepta (o rechaza) el resource server.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private TokenRevocadoRepository tokenRevocadoRepository;

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = jwtProperties();
    private final SecretKey key = config.jwtSecretKey(properties);

    /** Decoder real del resource server, con el validador de revocados sobre el mock. */
    private JwtDecoder decoder() {
        return config.jwtDecoder(key, properties, new TokenNoRevocadoValidator(tokenRevocadoRepository));
    }

    private TokenServiceImpl tokenService(Clock clock) {
        return new TokenServiceImpl(config.jwtEncoder(key), properties, tokenRevocadoRepository, clock);
    }

    @Test
    void generaUnTokenFirmadoConLosClaimsDeLaSesion() {
        UUID publicId = UUID.randomUUID();
        when(tokenRevocadoRepository.existsById(any())).thenReturn(false);

        TokenEmitido emitido = tokenService(Clock.systemUTC()).generar(publicId, "ROLE_EDITOR");
        Jwt jwt = decoder().decode(emitido.token());

        assertThat(jwt.getSubject()).isEqualTo(publicId.toString());
        assertThat(jwt.getClaimAsString(TokenService.CLAIM_NIVEL_ACCESO)).isEqualTo("ROLE_EDITOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("gestion-neumaticos");
        assertThat(UUID.fromString(jwt.getId())).isNotNull();
        assertThat(jwt.getExpiresAt()).isEqualTo(emitido.expiraEn().truncatedTo(ChronoUnit.SECONDS));
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void cadaTokenTieneUnJtiDistinto() {
        when(tokenRevocadoRepository.existsById(any())).thenReturn(false);
        TokenServiceImpl service = tokenService(Clock.systemUTC());
        UUID publicId = UUID.randomUUID();

        String jti1 = decoder().decode(service.generar(publicId, "ROLE_EDITOR").token()).getId();
        String jti2 = decoder().decode(service.generar(publicId, "ROLE_EDITOR").token()).getId();

        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void rechazaUnTokenRevocado() {
        TokenEmitido emitido = tokenService(Clock.systemUTC()).generar(UUID.randomUUID(), "ROLE_ADMINISTRADOR");
        when(tokenRevocadoRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> decoder().decode(emitido.token()))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("revocado");
    }

    @Test
    void rechazaUnTokenExpirado() {
        Clock haceUnDia = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        TokenEmitido emitido = tokenService(haceUnDia).generar(UUID.randomUUID(), "ROLE_ADMINISTRADOR");

        assertThatThrownBy(() -> decoder().decode(emitido.token()))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        JwtProperties otras = jwtProperties("otro-secreto-distinto-de-al-menos-32-bytes");
        TokenServiceImpl ajeno = new TokenServiceImpl(config.jwtEncoder(config.jwtSecretKey(otras)), otras,
                tokenRevocadoRepository, Clock.systemUTC());
        String token = ajeno.generar(UUID.randomUUID(), "ROLE_ADMINISTRADOR").token();

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(org.springframework.security.oauth2.jwt.BadJwtException.class);
    }

    @Test
    void revocarGuardaElJtiHastaSuExpiracion() {
        UUID jti = UUID.randomUUID();
        Instant expiraEn = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        Jwt jwt = jwtBuilder().jti(jti.toString()).subject(UUID.randomUUID().toString()).expiresAt(expiraEn).build();

        tokenService(Clock.systemUTC()).revocar(jwt);

        ArgumentCaptor<TokenRevocado> captor = ArgumentCaptor.forClass(TokenRevocado.class);
        verify(tokenRevocadoRepository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo(jti);
        assertThat(captor.getValue().getExpiraEn()).isEqualTo(expiraEn);
    }

    @Test
    void fallaAlArrancarSinSecretoOConSecretoCorto() {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(null, Duration.ofHours(1), "x")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("corto", Duration.ofHours(1), "x")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aceptaUnSecretoDeExactamente32Bytes() {
        assertThat(config.jwtSecretKey(jwtProperties("a".repeat(JwtConfig.LONGITUD_MINIMA_SECRETO))).getEncoded())
                .hasSize(JwtConfig.LONGITUD_MINIMA_SECRETO);
        assertThatThrownBy(() -> config.jwtSecretKey(jwtProperties("a".repeat(JwtConfig.LONGITUD_MINIMA_SECRETO - 1))))
                .isInstanceOf(IllegalStateException.class);
    }

}
