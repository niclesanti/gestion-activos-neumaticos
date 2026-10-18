package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/** Compra de un neumático nuevo. El proveedor es opcional. */
@Entity
@Table(name = "eventos_compra", schema = "trazabilidad")
@PrimaryKeyJoinColumn(name = "id")
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
}
