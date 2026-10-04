package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.DemasiadosIntentosException;

/**
 * API pública del módulo de seguridad para frenar la fuerza bruta y el
 * credential stuffing contra el login. Limita por IP y por identificador
 * (exista o no la cuenta, así el bloqueo no revela qué cuentas existen).
 */
public interface LimitadorIntentosLogin {

	/**
	 * Consume un intento de la IP y otro del identificador, antes de verificar
	 * la contraseña. Consumir por adelantado evita que requests concurrentes
	 * superen el límite mientras las anteriores todavía se están verificando.
	 *
	 * @throws DemasiadosIntentosException si alguno superó su límite
	 */
	void verificar(String ip, String identificador);

	/**
	 * Un login correcto olvida los intentos del identificador: el límite por
	 * cuenta termina contando solo los fallos.
	 */
	void registrarExito(String identificador);

}
