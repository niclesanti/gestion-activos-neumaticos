package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity;

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
@Table(name = "conductores", schema = "conductores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conductores_id_gen")
    @SequenceGenerator(name = "conductores_id_gen", schema = "conductores", sequenceName = "conductores_id_seq", allocationSize = 50)
    private Long id;

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    // Opcional: un conductor puede registrarse sin DNI.
    @Column(name = "dni", unique = true, length = 8)
    private String dni;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;
}
