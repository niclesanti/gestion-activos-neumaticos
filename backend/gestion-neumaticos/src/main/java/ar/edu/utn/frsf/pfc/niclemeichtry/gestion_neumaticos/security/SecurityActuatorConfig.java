package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena propia de Actuator. El puerto de gestión no pasa por el Tunnel ni se
 * publica: la protección de {@code health} y {@code prometheus} es de red, por
 * eso son públicos ahí. Cualquier otro endpoint se deniega aunque se exponga.
 * Va antes que la cadena principal, que exige JWT.
 */
@Configuration
public class SecurityActuatorConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher(EndpointRequest.toAnyEndpoint())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
						.anyRequest().denyAll());

		return http.build();
	}

}
