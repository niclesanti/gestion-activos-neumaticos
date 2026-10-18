package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity;

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
@Table(name = "marcas_neumaticos", schema = "neumaticos")
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcaNeumatico {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marcas_neumaticos_id_gen")
    @SequenceGenerator(name = "marcas_neumaticos_id_gen", schema = "neumaticos", sequenceName = "marcas_neumaticos_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;
}
