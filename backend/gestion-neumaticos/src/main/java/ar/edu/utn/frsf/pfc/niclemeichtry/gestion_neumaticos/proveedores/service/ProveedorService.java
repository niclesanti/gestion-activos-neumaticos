package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import java.util.List;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;

public interface ProveedorService {

    /**
     * Registra un proveedor y, si se indica, su domicilio (en la misma
     * transacción). Los textos se guardan sin espacios en los extremos; el email,
     * en minúsculas; un contacto vacío equivale a no tenerlo.
     *
     * @param proveedorRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario             identificador público del usuario que hace el alta.
     * @return el proveedor registrado.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException
     *         si ya existe un proveedor con el mismo nombre (sin distinguir mayúsculas).
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la provincia del domicilio no existe.
     */
    ProveedorResponseDTO registrarProveedor(ProveedorRequestDTO proveedorRequestDTO, String usuario);

    /** Todos los proveedores, ordenados por nombre. */
    List<ProveedorListadoDTO> listarProveedores();

}
