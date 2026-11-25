package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.Optional;

/**
 * Puerto que implementan los módulos donde puede estar un neumático (unidades de transporte, almacenamiento
 * y reparación). {@code neumaticos} los consume sin conocerlos, así no hay ciclos entre módulos.
 */
public interface ProveedorUbicacionNeumatico {

    /** Tipo de ubicación que resuelve este proveedor. */
    TipoUbicacion tipo();

    /**
     * Resuelve la ubicación a partir del id interno que guarda el neumático (slot, centro o servicio).
     * Aunque la unidad, el centro o el servicio estén dados de baja, se devuelven: indican dónde está el neumático.
     *
     * @param idInterno id interno de la referencia.
     * @return la ubicación, o vacío si ya no existe.
     */
    Optional<UbicacionOperativaDTO> buscar(Long idInterno);

}
