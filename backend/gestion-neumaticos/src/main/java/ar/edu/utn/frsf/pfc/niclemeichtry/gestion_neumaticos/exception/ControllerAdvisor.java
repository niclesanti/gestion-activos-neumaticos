package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.persistence.EntityNotFoundException;

/**
 * Traduce las excepciones a {@link ExceptionInfo}. Los errores propios de Spring
 * MVC (415, 405, 404, 400…) implementan {@link ErrorResponse}: conservan su
 * status y sus headers en lugar de caer en el 500 genérico, y se loguean en
 * WARN y sin stack, porque los provoca el cliente.
 *
 * <p>Nunca se devuelve al cliente el mensaje de una excepción de la plataforma
 * (JDK, Spring, JPA): solo el de las excepciones propias, escritas para él.
 */
@RestControllerAdvice
@Slf4j
public class ControllerAdvisor {

    static final String MENSAJE_SOLICITUD_INVALIDA = "La solicitud es inválida";
    static final String MENSAJE_CUERPO_INVALIDO = "El cuerpo de la petición es inválido o está mal formado";
    static final String MENSAJE_NO_ENCONTRADO = "El recurso solicitado no existe";
    static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionInfo> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Argumento inválido: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return respuesta(HttpStatus.BAD_REQUEST, MENSAJE_SOLICITUD_INVALIDA, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionInfo> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entidad no encontrada: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return respuesta(HttpStatus.NOT_FOUND, MENSAJE_NO_ENCONTRADO, request);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ExceptionInfo> handleUsuarioNoEncontradoException(UsuarioNoEncontradoException ex, WebRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EntidadDuplicadaException.class)
    public ResponseEntity<ExceptionInfo> handleEntidadDuplicadaException(EntidadDuplicadaException ex, WebRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionInfo> handleGeneralException(Exception ex, WebRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.warn("Request rechazado ({}): {} - Request: {}", status.value(), ex.getClass().getSimpleName(),
                    request.getDescription(false));
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .body(info(status, mensajePara(status), request));
        }
        log.error("Error inesperado: {} - Request: {}", ex.getMessage(), request.getDescription(false), ex);
        // El detalle queda en el log: devolverlo al cliente filtraría información interna.
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO, request);
    }

    @ExceptionHandler(PermisosDenegadosException.class)
    public ResponseEntity<ExceptionInfo> handlePermisosDenegadosException(PermisosDenegadosException ex, WebRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ExceptionInfo> handleOperacionNoPermitidaException(OperacionNoPermitidaException ex, WebRequest request) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ExceptionInfo> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        log.warn("Intento de acceso no autorizado: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ExceptionInfo> handleCredencialesInvalidasException(CredencialesInvalidasException ex, WebRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, CredencialesInvalidasException.MENSAJE, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ExceptionInfo> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.warn("Acceso denegado por permisos insuficientes: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /**
     * Rol insuficiente según {@code @PreAuthorize}. Sin este handler la excepción
     * caería en el genérico y respondería 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionInfo> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Acceso denegado por rol insuficiente - Request: {}", request.getDescription(false));
        return respuesta(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta operación", request);
    }

    /** Límite de intentos de login superado (fuerza bruta / credential stuffing). */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ExceptionInfo> handleDemasiadosIntentosException(DemasiadosIntentosException ex, WebRequest request) {
        return conReintento(respuesta(HttpStatus.TOO_MANY_REQUESTS, DemasiadosIntentosException.MENSAJE, request),
                ex.getReintentarEn());
    }

    /** El hash de contraseñas está al tope de su concurrencia (bulkhead). */
    @ExceptionHandler(ServicioSaturadoException.class)
    public ResponseEntity<ExceptionInfo> handleServicioSaturadoException(ServicioSaturadoException ex, WebRequest request) {
        log.warn("Servicio saturado: se rechaza el request - Request: {}", request.getDescription(false));
        return conReintento(respuesta(HttpStatus.SERVICE_UNAVAILABLE, ServicioSaturadoException.MENSAJE, request),
                ex.getReintentarEn());
    }

    /** Errores de validación de Bean Validation: "campo: mensaje" por cada campo rechazado. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionInfo> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((message1, message2) -> message1 + ", " + message2)
                .orElse("Validation error");
        return respuesta(HttpStatus.BAD_REQUEST, errorMessage, request);
    }

    /** JSON mal formado: mensaje fijo, sin el detalle del parser. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionInfo> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
            WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, MENSAJE_CUERPO_INVALIDO, request);
    }

    /** Mensaje propio para el status de un error de Spring MVC (nunca el de la excepción). */
    static String mensajePara(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> MENSAJE_SOLICITUD_INVALIDA;
            case 404 -> MENSAJE_NO_ENCONTRADO;
            case 405 -> "Método HTTP no permitido para este recurso";
            case 406 -> "No se puede generar una respuesta en el formato pedido";
            case 413 -> "La petición es demasiado grande";
            case 415 -> "Tipo de contenido no soportado";
            default -> status.is5xxServerError() ? MENSAJE_ERROR_INTERNO : MENSAJE_SOLICITUD_INVALIDA;
        };
    }

    private static ResponseEntity<ExceptionInfo> respuesta(HttpStatus status, String mensaje, WebRequest request) {
        return new ResponseEntity<>(info(status, mensaje, request), status);
    }

    private static ExceptionInfo info(HttpStatusCode status, String mensaje, WebRequest request) {
        return new ExceptionInfo(
                mensaje,
                request.getDescription(false),
                String.valueOf(System.currentTimeMillis()),
                status.value());
    }

    /** {@code Retry-After} en segundos enteros, redondeado hacia arriba y como mínimo 1. */
    private static ResponseEntity<ExceptionInfo> conReintento(ResponseEntity<ExceptionInfo> respuesta,
            Duration reintentarEn) {
        long segundos = Math.max(1, (reintentarEn.toMillis() + 999) / 1000);
        return ResponseEntity.status(respuesta.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(segundos))
                .body(respuesta.getBody());
    }

}
