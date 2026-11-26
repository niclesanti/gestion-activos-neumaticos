package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando se intenta montar un neumático en una posición que ya
 * tiene uno, sin haber aceptado el reemplazo, o cuando otro montaje concurrente
 * ocupó la posición primero. Responde 409.
 */
public class PosicionOcupadaException extends RuntimeException {

    public PosicionOcupadaException(String mensaje) {
        super(mensaje);
    }

    public PosicionOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
