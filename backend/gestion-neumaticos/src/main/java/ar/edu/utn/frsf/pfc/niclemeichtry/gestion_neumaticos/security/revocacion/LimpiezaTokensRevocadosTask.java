package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion;

import java.time.Clock;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Un token expirado ya lo rechaza la validación de {@code exp}: guardarlo en la
 * lista de revocados deja de tener sentido, así que se purga una vez por día.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class LimpiezaTokensRevocadosTask {

	private final TokenRevocadoRepository tokenRevocadoRepository;
	private final Clock clock;

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void eliminarExpirados() {
		int eliminados = tokenRevocadoRepository.eliminarExpiradosAntesDe(clock.instant());
		log.debug("Tokens revocados expirados eliminados: {}", eliminados);
	}

}
