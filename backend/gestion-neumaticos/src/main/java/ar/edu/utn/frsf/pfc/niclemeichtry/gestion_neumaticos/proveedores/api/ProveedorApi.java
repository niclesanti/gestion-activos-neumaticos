package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * API pública del módulo de proveedores para el resto de los módulos (por
 * ejemplo, neumáticos la usa para resolver el proveedor de una compra).
 */
public interface ProveedorApi {

    /**
     * Busca un proveedor por su identificador público.
     *
     * @return el proveedor, o vacío si no existe.
     */
    Optional<ProveedorResumenDTO> buscarPorPublicId(UUID publicId);

    /** Todos los proveedores, ordenados por nombre. */
    List<ProveedorResumenDTO> listar();

}
