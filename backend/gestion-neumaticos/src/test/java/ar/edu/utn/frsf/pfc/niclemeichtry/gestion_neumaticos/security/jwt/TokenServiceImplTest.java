package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtBuilder;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.jwtProperties;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.secretoBase64;
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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory;
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
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(properties.expiracion());
        assertThat(jwt.getAudience()).containsExactly(TestDataFactory.AUDIENCE);
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
        JwtProperties otras = jwtProperties(secretoBase64("otro-secreto-distinto-de-al-menos-32-bytes"));
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
    void rechazaUnTokenParaOtroDestinatario() {
        JwtProperties otraAudiencia = new JwtProperties(properties.secret(), properties.expiracion(),
                properties.issuer(), "otra-api");
        TokenServiceImpl ajeno = new TokenServiceImpl(config.jwtEncoder(key), otraAudiencia,
                tokenRevocadoRepository, Clock.systemUTC());
        String token = ajeno.generar(UUID.randomUUID(), "ROLE_ADMINISTRADOR").token();
        when(tokenRevocadoRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("aud");
    }

    @Test
    void fallaAlArrancarSinSecretoConSecretoCortoONoBase64() {
        assertThatThrownBy(() -> config.jwtSecretKey(jwtProperties(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
        assertThatThrownBy(() -> config.jwtSecretKey(jwtProperties(secretoBase64("corto"))))
                .isInstanceOf(IllegalStateException.class);
        // Una frase en texto plano ya no se acepta, aunque mida más de 32 caracteres.
        assertThatThrownBy(() -> config.jwtSecretKey(jwtProperties("una frase humana con espacios y mas de 32 caracteres")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openssl rand -base64 32");
    }

    @Test
    void aceptaUnSecretoDeExactamente32BytesDecodificados() {
        String exacto = secretoBase64("a".repeat(JwtConfig.LONGITUD_MINIMA_SECRETO));
        String corto = secretoBase64("a".repeat(JwtConfig.LONGITUD_MINIMA_SECRETO - 1));

        assertThat(config.jwtSecretKey(jwtProperties(exacto)).getEncoded())
                .hasSize(JwtConfig.LONGITUD_MINIMA_SECRETO);
        assertThatThrownBy(() -> config.jwtSecretKey(jwtProperties(corto)))
                .isInstanceOf(IllegalStateException.class);
    }

}
