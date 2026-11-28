package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando se intenta desmontar una posición que no tiene neumático
 * (o que otro desmontaje concurrente liberó primero). Responde 409.
 */
public class PosicionLibreException extends RuntimeException {

    public PosicionLibreException(String mensaje) {
        super(mensaje);
    }

    public PosicionLibreException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
