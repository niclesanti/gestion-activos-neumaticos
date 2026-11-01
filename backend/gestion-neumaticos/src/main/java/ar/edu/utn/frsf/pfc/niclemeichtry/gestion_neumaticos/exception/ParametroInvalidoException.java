package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/** Parámetro de consulta inválido (por ejemplo, un orden no permitido). Responde 400. */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
