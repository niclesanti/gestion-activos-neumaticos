package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Localidad de una provincia. Se crea al registrar un domicilio con una
 * localidad nueva, con un upsert nativo ({@code LocalidadRepository}): el id lo
 * da el DEFAULT de la columna, nunca Hibernate.
 */
@Entity
@Table(name = "localidades", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Localidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Opcional.
    @Column(name = "codigo_postal", length = 8)
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;
}
