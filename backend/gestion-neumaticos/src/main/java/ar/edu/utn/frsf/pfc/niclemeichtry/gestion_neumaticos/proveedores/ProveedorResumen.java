package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores;

import java.util.UUID;

/**
 * Datos mínimos de un proveedor que otros módulos necesitan: el {@code id}
 * interno (referencia formal, ej. en los eventos de trazabilidad) y el nombre
 * para los snapshots. El {@code id} nunca sale por la API REST.
 */
public record ProveedorResumen(Long id, UUID publicId, String nombre) {

}
