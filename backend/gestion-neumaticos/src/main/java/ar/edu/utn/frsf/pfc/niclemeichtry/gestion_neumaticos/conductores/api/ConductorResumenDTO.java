package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api;

import java.util.UUID;

/**
 * Datos mínimos de un conductor que otros módulos necesitan: el {@code id} interno (referencia formal,
 * ej. en los eventos de trazabilidad) y el nombre para los snapshots. El {@code id} nunca sale por la API REST.
 */
public record ConductorResumenDTO(Long id, UUID publicId, String nombre, String apellido) {

    /** Nombre y apellido, como se guarda en el historial. */
    public String nombreCompleto() {
        return nombre + " " + apellido;
    }

}
