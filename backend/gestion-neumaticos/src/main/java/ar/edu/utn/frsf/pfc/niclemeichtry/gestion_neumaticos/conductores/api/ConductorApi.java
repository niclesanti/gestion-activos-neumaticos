package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api;

import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de conductores para el resto de los módulos. */
public interface ConductorApi {

    /**
     * Busca un conductor activo (sin baja lógica) por su identificador público.
     *
     * @return el conductor, o vacío si no existe o está dado de baja.
     */
    Optional<ConductorResumenDTO> buscarActivoPorPublicId(UUID publicId);

}
