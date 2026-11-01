package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa Spring Data JPA Auditing. Vive en una configuración aparte (no en la clase
 * de arranque) para no cargarse en los {@code @WebMvcTest}. El reloj es un bean para
 * que los tests puedan fijar la hora; el usuario lo aporta el {@code AuditorAware} de
 * {@code security}.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditoriaDateTimeProvider")
public class AuditoriaJpaConfig {

    @Bean
    Clock auditoriaClock() {
        return Clock.systemUTC();
    }

    @Bean
    DateTimeProvider auditoriaDateTimeProvider(Clock auditoriaClock) {
        return () -> Optional.of(Instant.now(auditoriaClock));
    }
}
