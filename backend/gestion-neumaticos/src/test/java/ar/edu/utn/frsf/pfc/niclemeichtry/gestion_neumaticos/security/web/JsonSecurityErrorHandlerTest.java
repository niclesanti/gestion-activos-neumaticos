package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import tools.jackson.databind.json.JsonMapper;

/**
 * El 403 de la cadena de filtros no se alcanza hoy (toda regla HTTP es
 * {@code permitAll} o {@code authenticated}): se prueba el handler directo.
 */
class JsonSecurityErrorHandlerTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final JsonSecurityErrorHandler handler = new JsonSecurityErrorHandler(jsonMapper);
	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");
	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	void noAutenticadoResponde401ConDesafioBearer() throws Exception {
		handler.commence(request, response, new InsufficientAuthenticationException("sin token"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
		assertThat(cuerpo()).satisfies(info -> {
			assertThat(info.status()).isEqualTo(401);
			assertThat(info.message()).isEqualTo(JsonSecurityErrorHandler.MENSAJE_NO_AUTENTICADO);
			assertThat(info.path()).isEqualTo("uri=/api/prueba");
		});
	}

	@Test
	void accesoDenegadoResponde403SinDesafio() throws Exception {
		handler.handle(request, response, new AccessDeniedException("denegado"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
		assertThat(cuerpo()).satisfies(info -> {
			assertThat(info.status()).isEqualTo(403);
			assertThat(info.message()).isEqualTo(JsonSecurityErrorHandler.MENSAJE_ACCESO_DENEGADO);
		});
	}

	private ExceptionInfo cuerpo() throws Exception {
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		return jsonMapper.readValue(response.getContentAsString(), ExceptionInfo.class);
	}

}
