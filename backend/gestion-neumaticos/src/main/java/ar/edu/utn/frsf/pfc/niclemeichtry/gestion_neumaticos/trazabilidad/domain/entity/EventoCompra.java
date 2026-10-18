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

/** Compra de un neumático nuevo. El proveedor es opcional. */
@Entity
@DiscriminatorValue("COMPRA")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventoCompra extends EventoNeumatico {
    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "nombre_proveedor", length = 150)
    private String nombreProveedor;

    @Override
    public TipoEventoNeumatico getTipo() {
        return TipoEventoNeumatico.COMPRA;
    }
}
