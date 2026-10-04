package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

import java.time.Duration;

/**
 * Se superó el límite de intentos (por IP o por cuenta). Responde 429 con
 * {@code Retry-After}. El mensaje es el mismo exista o no la cuenta, para no
 * permitir enumerarlas.
 */
public class DemasiadosIntentosException extends RuntimeException {

    public static final String MENSAJE = "Demasiados intentos. Esperá unos minutos y volvé a intentar";

    private final Duration reintentarEn;

    public DemasiadosIntentosException(Duration reintentarEn) {
        super(MENSAJE);
        this.reintentarEn = reintentarEn;
    }

    public Duration getReintentarEn() {
        return reintentarEn;
    }
}
