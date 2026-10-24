package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto;

import java.util.UUID;

/** Proveedor en un listado de opciones (ej. el select del alta de neumático). */
public record ProveedorListadoDTO(UUID publicId, String nombre) {

}
