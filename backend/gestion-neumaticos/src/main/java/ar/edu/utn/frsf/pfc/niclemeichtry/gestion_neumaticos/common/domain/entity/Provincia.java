package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Catálogo de provincias: lo carga la migración V14. */
@Entity
@Table(name = "provincias", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provincia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;
}
