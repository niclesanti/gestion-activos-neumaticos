package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando se intenta crear una entidad que viola una regla
 * de unicidad (por ejemplo, un neumático con un número de serie ya
 * registrado). Responde 409.
 */
public class EntidadDuplicadaException extends RuntimeException {
    
    public EntidadDuplicadaException(String mensaje) {
        super(mensaje);
    }
    
    public EntidadDuplicadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
