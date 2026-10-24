package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "proveedores", schema = "proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorNeumaticos {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proveedores_id_gen")
    @SequenceGenerator(name = "proveedores_id_gen", schema = "proveedores", sequenceName = "proveedores_id_seq", allocationSize = 50)
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

    // Referencia por id al domicilio del módulo common (sin asociación JPA ni FK
    // entre módulos): se crea y se lee por la fachada DomicilioApi.
    @Column(name = "domicilio_id", unique = true)
    private Long domicilioId;
}
