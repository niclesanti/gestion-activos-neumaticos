package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "servicios_reparacion", schema = "reparacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioReparacion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "servicios_reparacion_id_gen")
    @SequenceGenerator(name = "servicios_reparacion_id_gen", schema = "reparacion", sequenceName = "servicios_reparacion_id_seq", allocationSize = 50)
    private Long id;

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Contacto opcional.
    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "telefono", length = 30)
    private String telefono;

    // Neumáticos derivados hoy a este servicio.
    @Column(name = "cantidad_neumaticos_en_reparacion", nullable = false)
    @Builder.Default
    private Integer cantidadNeumaticosEnReparacion = 0;

    // Referencia por id al domicilio del módulo common (sin asociación JPA ni FK
    // entre módulos): se crea y se lee por la fachada DomicilioApi.
    @Column(name = "domicilio_id", unique = true)
    private Long domicilioId;

    /** Registra que se derivó un neumático a este servicio. */
    public void enviarNeumaticoReparacion() {
        cantidadNeumaticosEnReparacion++;
    }

    /** Registra que un neumático volvió del servicio; sin neumáticos derivados no hay qué reingresar. */
    public void reingresarNeumatico() {
        if (cantidadNeumaticosEnReparacion <= 0) {
            throw new OperacionNoPermitidaException(
                    "El servicio de reparación no tiene neumáticos en reparación para reingresar");
        }
        cantidadNeumaticosEnReparacion--;
    }
}
