package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.jwt.AutoridadesVigentesConverter;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;

/**
 * API stateless autenticada con JWT (Bearer). El token lo emite
 * {@link TokenService} en el login y lo valida el resource server de Spring
 * Security en cada request; el rol se toma de la base en cada request
 * ({@link AutoridadesVigentesConverter}), no del token.
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

	private static final String[] DOCUMENTACION = { "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**" };

	/**
	 * La API solo devuelve JSON: no carga recursos ni se puede embeber. Swagger UI
	 * (deshabilitado en prod) necesita scripts y estilos propios, así que queda afuera.
	 */
	static final String CSP_API = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

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
			JsonSecurityErrorHandler errorHandler,
			NivelAccesoVigente nivelAccesoVigente) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
						.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
								new NegatedRequestMatcher(documentacion()),
								new StaticHeadersWriter("Content-Security-Policy", CSP_API))))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(DOCUMENTACION).permitAll()
						.requestMatchers(LOGIN).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver())
						.jwt(jwt -> jwt.jwtAuthenticationConverter(new AutoridadesVigentesConverter(nivelAccesoVigente)))
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler));

		return http.build();
	}

	private static RequestMatcher documentacion() {
		return new OrRequestMatcher(Arrays.stream(DOCUMENTACION)
				.map(patron -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(patron))
				.toList());
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

}
