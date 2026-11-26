package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando se intenta montar un neumático que no está disponible:
 * no está en stock ni en espera (montado, en reparación) o está dado de baja.
 * Es un conflicto con el estado actual del recurso: responde 409.
 */
public class NeumaticoNoDisponibleException extends RuntimeException {

    public NeumaticoNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public NeumaticoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
