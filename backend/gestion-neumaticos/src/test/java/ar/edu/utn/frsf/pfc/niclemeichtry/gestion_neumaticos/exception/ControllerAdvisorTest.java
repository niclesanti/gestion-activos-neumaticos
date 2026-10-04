package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Cada excepción se traduce a su status y a un {@link ExceptionInfo} con el
 * path del request. Se prueba sin MockMvc: hoy ningún endpoint lanza la
 * mayoría de estas excepciones, pero los futuros módulos las van a usar.
 */
class ControllerAdvisorTest {

    private static final String URI = "/api/prueba";
    private static final RuntimeException CAUSA = new RuntimeException("causa");

    private final ControllerAdvisor advisor = new ControllerAdvisor();
    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", URI));

    static Stream<Arguments> excepcionesConMensajePropio() {
        return Stream.of(
                caso("UsuarioNoEncontradoException", new UsuarioNoEncontradoException("sin usuario"),
                        HttpStatus.NOT_FOUND, ControllerAdvisor::handleUsuarioNoEncontradoException),
                caso("UsuarioNoEncontradoException con causa", new UsuarioNoEncontradoException("sin usuario", CAUSA),
                        HttpStatus.NOT_FOUND, ControllerAdvisor::handleUsuarioNoEncontradoException),
                caso("EntidadDuplicadaException", new EntidadDuplicadaException("duplicada"),
                        HttpStatus.CONFLICT, ControllerAdvisor::handleEntidadDuplicadaException),
                caso("EntidadDuplicadaException con causa", new EntidadDuplicadaException("duplicada", CAUSA),
                        HttpStatus.CONFLICT, ControllerAdvisor::handleEntidadDuplicadaException),
                caso("PermisosDenegadosException", new PermisosDenegadosException("sin permisos"),
                        HttpStatus.FORBIDDEN, ControllerAdvisor::handlePermisosDenegadosException),
                caso("PermisosDenegadosException con causa", new PermisosDenegadosException("sin permisos", CAUSA),
                        HttpStatus.FORBIDDEN, ControllerAdvisor::handlePermisosDenegadosException),
                caso("OperacionNoPermitidaException", new OperacionNoPermitidaException("no permitida"),
                        HttpStatus.UNPROCESSABLE_ENTITY, ControllerAdvisor::handleOperacionNoPermitidaException),
                caso("OperacionNoPermitidaException con causa", new OperacionNoPermitidaException("no permitida", CAUSA),
                        HttpStatus.UNPROCESSABLE_ENTITY, ControllerAdvisor::handleOperacionNoPermitidaException),
                caso("UnauthorizedException", new UnauthorizedException("no autenticado"),
                        HttpStatus.UNAUTHORIZED, ControllerAdvisor::handleUnauthorizedException),
                caso("UnauthorizedException con causa", new UnauthorizedException("no autenticado", CAUSA),
                        HttpStatus.UNAUTHORIZED, ControllerAdvisor::handleUnauthorizedException),
                caso("ForbiddenException", new ForbiddenException("prohibido"),
                        HttpStatus.FORBIDDEN, ControllerAdvisor::handleForbiddenException),
                caso("ForbiddenException con causa", new ForbiddenException("prohibido", CAUSA),
                        HttpStatus.FORBIDDEN, ControllerAdvisor::handleForbiddenException));
    }

    /** Handler del advisor para un tipo de excepción (referencia a método sin receptor). */
    @FunctionalInterface
    interface Handler<E extends Exception> {

        ResponseEntity<ExceptionInfo> manejar(ControllerAdvisor advisor, E excepcion, WebRequest request);

    }

    private static <E extends Exception> Arguments caso(String nombre, E excepcion, HttpStatus status,
            Handler<E> handler) {
        return Arguments.of(nombre, excepcion, status, handler);
    }

    @ParameterizedTest(name = "{0} → {2}")
    @MethodSource("excepcionesConMensajePropio")
    @DisplayName("responde el status de la excepción con su mensaje y el path del request")
    <E extends Exception> void traduceLaExcepcion(String nombre, E excepcion, HttpStatus status,
            Handler<E> handler) {
        ResponseEntity<ExceptionInfo> respuesta = handler.manejar(advisor, excepcion, request);

        assertRespuesta(respuesta, status, excepcion.getMessage());
    }

    @Test
    @DisplayName("credenciales inválidas: 401 con el mensaje genérico")
    void credencialesInvalidas() {
        assertRespuesta(advisor.handleCredencialesInvalidasException(new CredencialesInvalidasException(), request),
                HttpStatus.UNAUTHORIZED, CredencialesInvalidasException.MENSAJE);
    }

    @Test
    @DisplayName("cuerpo ilegible: 400 con un mensaje fijo, sin el detalle del parser")
    void cuerpoIlegible() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("detalle interno del parser",
                new MockHttpInputMessage(new byte[0]));

        assertRespuesta(advisor.handleHttpMessageNotReadableException(ex, request),
                HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o está mal formado");
    }

    @Test
    @DisplayName("excepciones de la plataforma: mensaje genérico, nunca el de la excepción")
    void excepcionesDeLaPlataformaNoExponenSuMensaje() {
        assertRespuesta(advisor.handleIllegalArgumentException(
                new IllegalArgumentException("Invalid UUID string: 1'; --"), request),
                HttpStatus.BAD_REQUEST, ControllerAdvisor.MENSAJE_SOLICITUD_INVALIDA);
        assertRespuesta(advisor.handleEntityNotFoundException(
                new EntityNotFoundException("Unable to find ...Usuario with id 7"), request),
                HttpStatus.NOT_FOUND, ControllerAdvisor.MENSAJE_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("límite de intentos: 429 con Retry-After en segundos, redondeado hacia arriba")
    void demasiadosIntentos() {
        ResponseEntity<ExceptionInfo> respuesta = advisor.handleDemasiadosIntentosException(
                new DemasiadosIntentosException(Duration.ofMillis(61_200)), request);

        assertRespuesta(respuesta, HttpStatus.TOO_MANY_REQUESTS, DemasiadosIntentosException.MENSAJE);
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("62");
    }

    @Test
    @DisplayName("servicio saturado: 503 con Retry-After de al menos 1 segundo")
    void servicioSaturado() {
        ResponseEntity<ExceptionInfo> respuesta = advisor.handleServicioSaturadoException(
                new ServicioSaturadoException(Duration.ZERO), request);

        assertRespuesta(respuesta, HttpStatus.SERVICE_UNAVAILABLE, ServicioSaturadoException.MENSAJE);
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    }

    static Stream<Arguments> erroresDelFramework() {
        return Stream.of(
                Arguments.of(new HttpMediaTypeNotSupportedException("text/plain"), HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Tipo de contenido no soportado"),
                Arguments.of(new HttpRequestMethodNotSupportedException("DELETE"), HttpStatus.METHOD_NOT_ALLOWED,
                        "Método HTTP no permitido para este recurso"),
                Arguments.of(new NoResourceFoundException(HttpMethod.GET, "/api/nada", "api/nada"),
                        HttpStatus.NOT_FOUND, ControllerAdvisor.MENSAJE_NO_ENCONTRADO),
                Arguments.of(new HttpMediaTypeNotAcceptableException("xml"), HttpStatus.NOT_ACCEPTABLE,
                        "No se puede generar una respuesta en el formato pedido"),
                Arguments.of(new MissingServletRequestParameterException("id", "String"), HttpStatus.BAD_REQUEST,
                        ControllerAdvisor.MENSAJE_SOLICITUD_INVALIDA),
                Arguments.of(new AsyncRequestTimeoutException(), HttpStatus.SERVICE_UNAVAILABLE,
                        ControllerAdvisor.MENSAJE_ERROR_INTERNO));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("erroresDelFramework")
    @DisplayName("errores de Spring MVC: conservan su status (no 500) con un mensaje propio")
    void erroresDelFrameworkConservanSuStatus(Exception ex, HttpStatus status, String mensaje) {
        ResponseEntity<ExceptionInfo> respuesta = advisor.handleGeneralException(ex, request);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getStatusCode()).isEqualTo(status);
        assertThat(respuesta.getBody()).isInstanceOfSatisfying(ExceptionInfo.class, info -> {
            assertThat(info.status()).isEqualTo(status.value());
            assertThat(info.message()).isEqualTo(mensaje);
        });
    }

    @Test
    @DisplayName("un status 4xx sin mensaje propio usa el de solicitud inválida")
    void otrosStatus() {
        assertThat(ControllerAdvisor.mensajePara(HttpStatus.PAYLOAD_TOO_LARGE)).isEqualTo("La petición es demasiado grande");
        assertThat(ControllerAdvisor.mensajePara(HttpStatus.CONFLICT)).isEqualTo(ControllerAdvisor.MENSAJE_SOLICITUD_INVALIDA);
    }

    @Test
    @DisplayName("rol insuficiente (@PreAuthorize): 403")
    void accesoDenegado() {
        assertRespuesta(advisor.handleAccessDeniedException(new AccessDeniedException("Access Denied"), request),
                HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta operación");
    }

    @Test
    @DisplayName("error inesperado: 500 sin exponer el mensaje original")
    void errorInesperado() {
        ResponseEntity<ExceptionInfo> respuesta = advisor.handleGeneralException(
                new Exception("password=secreta en la URL de la base"), request);

        assertRespuesta(respuesta, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        assertThat(respuesta.getBody().message()).doesNotContain("secreta");
    }

    @Test
    @DisplayName("validación: une los errores de cada campo como 'campo: mensaje'")
    void validacionConErroresDeCampo() throws NoSuchMethodException {
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(new Formulario(), "request");
        errores.rejectValue("identifier", "NotBlank", "es obligatorio");
        errores.rejectValue("password", "Size", "es demasiado larga");

        assertRespuesta(advisor.handleValidationException(validacion(errores), request),
                HttpStatus.BAD_REQUEST, "identifier: es obligatorio, password: es demasiado larga");
    }

    @Test
    @DisplayName("validación sin errores de campo: mensaje por defecto")
    void validacionSinErroresDeCampo() throws NoSuchMethodException {
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(new Formulario(), "request");
        errores.reject("global", "error global");

        assertRespuesta(advisor.handleValidationException(validacion(errores), request),
                HttpStatus.BAD_REQUEST, "Validation error");
    }

    private static MethodArgumentNotValidException validacion(BeanPropertyBindingResult errores)
            throws NoSuchMethodException {
        MethodParameter parametro = new MethodParameter(
                ControllerAdvisorTest.class.getDeclaredMethod("recibir", Formulario.class), 0);
        return new MethodArgumentNotValidException(parametro, errores);
    }

    private static void assertRespuesta(ResponseEntity<?> respuesta, HttpStatus status, String mensaje) {
        assertThat(respuesta.getStatusCode()).isEqualTo(status);
        assertThat(respuesta.getBody()).isInstanceOfSatisfying(ExceptionInfo.class, info -> {
            assertThat(info.status()).isEqualTo(status.value());
            assertThat(info.message()).isEqualTo(mensaje);
            assertThat(info.path()).isEqualTo("uri=" + URI);
            assertThat(info.timestamp()).isNotBlank();
        });
    }

    /** Destino ficticio para construir un {@link MethodArgumentNotValidException}. */
    @SuppressWarnings("unused")
    private void recibir(Formulario formulario) {
    }

    /** Bean con los campos que se rechazan en los tests de validación. */
    static class Formulario {

        private String identifier;
        private String password;

        public String getIdentifier() {
            return identifier;
        }

        public String getPassword() {
            return password;
        }

    }

}
