package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/** Montaje de un neumático en una posición de una unidad (HU-31). El conductor es opcional. */
@Entity
@DiscriminatorValue("ASIGNACION")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventoAsignacion extends EventoNeumatico {
    @Column(name = "unidad_id")
    private Long unidadId;

    @Column(name = "patente_unidad", length = 10)
    private String patenteUnidad;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "posicion_slot", length = 20)
    private String posicionSlot;

    @Column(name = "conductor_id")
    private Long conductorId;

    @Column(name = "nombre_conductor", length = 201)
    private String nombreConductor;

    @Override
    public TipoEventoNeumatico getTipo() {
        return TipoEventoNeumatico.ASIGNACION;
    }
}
