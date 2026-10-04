package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los errores de autenticación/autorización ocurren en los filtros de Spring
 * Security, antes de llegar al {@code ControllerAdvisor}: se responden acá con el
 * mismo {@link ExceptionInfo} para que el cliente reciba un único formato.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String MENSAJE_NO_AUTENTICADO = "No autenticado o sesión inválida";
	static final String MENSAJE_ACCESO_DENEGADO = "Acceso denegado";

	private final JsonMapper jsonMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		escribir(request, response, HttpStatus.UNAUTHORIZED, MENSAJE_NO_AUTENTICADO);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		escribir(request, response, HttpStatus.FORBIDDEN, MENSAJE_ACCESO_DENEGADO);
	}

	private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
			String mensaje) throws IOException {
		ExceptionInfo info = new ExceptionInfo(
				mensaje,
				"uri=" + request.getRequestURI(),
				String.valueOf(System.currentTimeMillis()),
				status.value());
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), info);
	}

}
