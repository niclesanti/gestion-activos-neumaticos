package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;

/**
 * API stateless autenticada con JWT (Bearer). El token lo emite
 * {@link TokenService} en el login y lo valida el resource server de Spring
 * Security en cada request.
 *
 * <p>La autorización por rol se declara en cada endpoint con
 * {@code @PreAuthorize("hasRole('EDITOR')")}: gracias a {@link #roleHierarchy()}
 * eso habilita también al administrador.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	public static final String LOGIN_PATH = "/api/auth/login";

	private static final RequestMatcher LOGIN = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, LOGIN_PATH);

	/**
	 * ADMINISTRADOR &gt; EDITOR &gt; LECTOR: cada nivel incluye los permisos del
	 * siguiente. La usan tanto {@code @PreAuthorize} como
	 * {@code authorizeHttpRequests}. Es {@code static} para que la seguridad de
	 * métodos la tome sin inicializar antes esta configuración.
	 */
	@Bean
	static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
				.role("ADMINISTRADOR").implies("EDITOR")
				.role("EDITOR").implies("LECTOR")
				.build();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource,
			JsonSecurityErrorHandler errorHandler) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**")
						.permitAll()
						.requestMatchers(LOGIN).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver())
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler));

		return http.build();
	}

	/**
	 * El login ignora un token que llegue en el header: siempre corre sin
	 * usuario, así que en la base solo puede usar la búsqueda de credenciales
	 * (ver {@code usuarios.buscar_para_login}), nunca los permisos de un rol.
	 */
	private BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver porDefecto = new DefaultBearerTokenResolver();
		return request -> LOGIN.matches(request) ? null : porDefecto.resolve(request);
	}

	/**
	 * El nivel de acceso viaja en el claim {@code nivelAcceso} y ya tiene el
	 * prefijo {@code ROLE_}, así que se usa tal cual como authority.
	 */
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(TokenService.CLAIM_NIVEL_ACCESO);
		authorities.setAuthorityPrefix("");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

}
