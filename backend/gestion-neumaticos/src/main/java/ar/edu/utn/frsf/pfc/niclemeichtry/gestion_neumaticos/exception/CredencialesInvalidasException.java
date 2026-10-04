package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception;

/**
 * Excepción lanzada cuando el inicio de sesión falla, sea porque el usuario no
 * existe o porque la contraseña es incorrecta. El mensaje es siempre el mismo a
 * propósito: distinguir ambos casos permitiría enumerar cuentas existentes.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public static final String MENSAJE = "Credenciales inválidas";

    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}
