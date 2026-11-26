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

/** Desmontaje de un neumático de una posición de una unidad (hoy, al reemplazarlo: HU-31). */
@Entity
@DiscriminatorValue("DESASIGNACION")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventoDesasignacion extends EventoNeumatico {
    @Column(name = "unidad_id")
    private Long unidadId;

    @Column(name = "patente_unidad", length = 10)
    private String patenteUnidad;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "posicion_slot", length = 20)
    private String posicionSlot;

    @Override
    public TipoEventoNeumatico getTipo() {
        return TipoEventoNeumatico.DESASIGNACION;
    }
}
