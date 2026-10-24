package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api;

import java.util.Optional;

/**
 * Fachada del módulo common para el resto de los módulos: es la única forma de
 * crear o leer un domicilio. El módulo dueño (ej. proveedores) guarda solo el
 * id que devuelve el alta, sin FK entre schemas; por eso el alta debe
 * llamarse dentro de la transacción del dueño, para que se revierta con ella.
 */
public interface DomicilioApi {

    /**
     * Registra un domicilio. La localidad se busca por nombre dentro de la
     * provincia (sin distinguir mayúsculas) y se crea si no existe. Los textos
     * se guardan recortados; los opcionales en blanco, como nulos.
     *
     * @param domicilioRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario             identificador público del usuario que hace el alta.
     * @return el id interno del domicilio, para guardarlo como referencia.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la provincia no existe.
     */
    Long registrarDomicilio(DomicilioRequestDTO domicilioRequestDTO, String usuario);

    /**
     * Busca un domicilio por su id interno.
     *
     * @return el domicilio con su localidad y provincia, o vacío si no existe.
     */
    Optional<DomicilioResponseDTO> buscarDomicilio(Long id);

}
