package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación que viola
 * las reglas de negocio de la aplicación o que comprometería la integridad
 * de datos ya procesados. Responde 422.
 */
public class OperacionNoPermitidaException extends RuntimeException {
    
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
    
    public OperacionNoPermitidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
