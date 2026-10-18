package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando una entidad referenciada por la petición (por
 * ejemplo, la marca o el proveedor de un neumático) no existe. Responde 404
 * con un mensaje apto para el usuario final.
 */
public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public EntidadNoEncontradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
