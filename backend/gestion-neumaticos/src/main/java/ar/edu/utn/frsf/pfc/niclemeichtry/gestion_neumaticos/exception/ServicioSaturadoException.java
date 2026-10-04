package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

import java.time.Duration;

/**
 * Una operación costosa (el hash de contraseñas) alcanzó su límite de
 * concurrencia: se rechaza rápido con 503 y {@code Retry-After} en lugar de
 * encolar hilos hasta agotar memoria o conexiones.
 */
public class ServicioSaturadoException extends RuntimeException {

    public static final String MENSAJE = "El servicio está saturado. Volvé a intentar en unos segundos";

    private final Duration reintentarEn;

    public ServicioSaturadoException(Duration reintentarEn) {
        super(MENSAJE);
        this.reintentarEn = reintentarEn;
    }

    public Duration getReintentarEn() {
        return reintentarEn;
    }
}
