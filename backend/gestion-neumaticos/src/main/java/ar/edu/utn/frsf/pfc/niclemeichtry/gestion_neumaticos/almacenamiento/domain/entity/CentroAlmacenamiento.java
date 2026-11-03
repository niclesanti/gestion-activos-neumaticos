package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditableEntity;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "centros_almacenamiento", schema = "almacenamiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentroAlmacenamiento extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "centros_almacenamiento_id_gen")
    @SequenceGenerator(name = "centros_almacenamiento_id_gen", schema = "almacenamiento", sequenceName = "centros_almacenamiento_id_seq", allocationSize = 50)
    private Long id;

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Neumáticos guardados hoy en este centro.
    @Column(name = "cantidad_neumaticos_en_stock", nullable = false)
    @Builder.Default
    private Integer cantidadNeumaticosEnStock = 0;

    // Referencia por id al domicilio del módulo common (sin asociación JPA ni FK
    // entre módulos): se crea y se lee por la fachada DomicilioApi. Obligatorio.
    @Column(name = "domicilio_id", nullable = false, unique = true)
    private Long domicilioId;

    /** Registra que ingresó un neumático al stock del centro. */
    public void agregarNeumaticoStock() {
        cantidadNeumaticosEnStock++;
    }

    /** Registra que salió un neumático del centro; sin stock no hay qué remover. */
    public void removerNeumaticoStock() {
        if (cantidadNeumaticosEnStock <= 0) {
            throw new OperacionNoPermitidaException(
                    "El centro de almacenamiento no tiene neumáticos en stock para remover");
        }
        cantidadNeumaticosEnStock--;
    }
}
