package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenEmitido;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.TokenService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion.TokenRevocadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final TokenRevocadoRepository tokenRevocadoRepository;
	private final Clock clock;

	@Override
	public TokenEmitido generar(UUID publicId, String nivelAcceso) {
		Instant ahora = clock.instant();
		Instant expiraEn = ahora.plus(properties.expiracion());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.id(UUID.randomUUID().toString())
				.issuer(properties.issuer())
				.subject(publicId.toString())
				.issuedAt(ahora)
				.expiresAt(expiraEn)
				.claim(CLAIM_NIVEL_ACCESO, nivelAcceso)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenEmitido(token, expiraEn);
	}

	@Override
	@Transactional
	public void revocar(Jwt jwt) {
		tokenRevocadoRepository.save(new TokenRevocado(UUID.fromString(jwt.getId()), jwt.getExpiresAt()));
		log.debug("Token revocado para el usuario {}", jwt.getSubject());
	}

}
