package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ejecuta una acción como el sistema, sin usuario autenticado: las conexiones
 * que se tomen adentro quedan como {@code gn_app} (ver
 * {@code bd.RolBaseDatosDataSource}).
 *
 * <p>Lo usan los listeners asíncronos de eventos que escriben datos propios del
 * sistema (ej. la trazabilidad). No alcanza con que corran en otro hilo: Spring
 * Modulith (observability) decora el executor con un
 * {@code ContextPropagatingTaskDecorator}, que también copia el
 * {@code SecurityContext} del request que publicó el evento. La conexión se toma
 * al iniciar la transacción, así que la acción tiene que abrir la suya adentro
 * (ej. un método de repositorio) y no heredar una ya abierta.
 */
public final class ComoSistema {

	private ComoSistema() {
	}

	/** Corre la acción sin usuario y después restaura el contexto que había. */
	public static void ejecutar(Runnable accion) {
		SecurityContext anterior = SecurityContextHolder.getContext();
		SecurityContextHolder.clearContext();
		try {
			accion.run();
		} finally {
			SecurityContextHolder.setContext(anterior);
		}
	}

}
