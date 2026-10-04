package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;

/**
 * Convención para restringir endpoints: {@code @PreAuthorize("hasRole('X')")}
 * con la jerarquía ADMINISTRADOR &gt; EDITOR &gt; LECTOR de {@link SecurityConfig}.
 * Usa un controller de prueba porque todavía no hay endpoints de negocio.
 */
@WebMvcTest(AutorizacionPorRolTest.EndpointsDePrueba.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class,
		AutorizacionPorRolTest.EndpointsDePrueba.class })
class AutorizacionPorRolTest {

	@RestController
	static class EndpointsDePrueba {

		@GetMapping("/prueba/lector")
		@PreAuthorize("hasRole('LECTOR')")
		String lector() {
			return "ok";
		}

		@GetMapping("/prueba/editor")
		@PreAuthorize("hasRole('EDITOR')")
		String editor() {
			return "ok";
		}

		@GetMapping("/prueba/administrador")
		@PreAuthorize("hasRole('ADMINISTRADOR')")
		String administrador() {
			return "ok";
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@ParameterizedTest(name = "{0} en /prueba/{1} → {2}")
	@CsvSource({
			"ROLE_LECTOR,        lector,        200",
			"ROLE_LECTOR,        editor,        403",
			"ROLE_LECTOR,        administrador, 403",
			"ROLE_EDITOR,        lector,        200",
			"ROLE_EDITOR,        editor,        200",
			"ROLE_EDITOR,        administrador, 403",
			"ROLE_ADMINISTRADOR, lector,        200",
			"ROLE_ADMINISTRADOR, editor,        200",
			"ROLE_ADMINISTRADOR, administrador, 200",
	})
	void jerarquiaDeRoles(String nivel, String endpoint, int esperado) throws Exception {
		mockMvc.perform(get("/prueba/" + endpoint).with(jwt().authorities(new SimpleGrantedAuthority(nivel))))
				.andExpect(status().is(esperado));
	}

	@Test
	void rolInsuficienteRespondeExceptionInfo() throws Exception {
		mockMvc.perform(get("/prueba/administrador").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EDITOR"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

}
