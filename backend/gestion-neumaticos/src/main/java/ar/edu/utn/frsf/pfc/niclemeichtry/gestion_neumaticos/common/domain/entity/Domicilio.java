package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domicilio de una entidad de otro módulo, que guarda solo su id y lo maneja
 * por la fachada {@link ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi}.
 */
@Entity
@Table(name = "domicilios", schema = "common")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domicilios_id_gen")
    @SequenceGenerator(name = "domicilios_id_gen", schema = "common", sequenceName = "domicilios_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "calle", nullable = false, length = 100)
    private String calle;

    @Column(name = "altura", nullable = false, length = 10)
    private String altura;

    @Column(name = "piso", length = 10)
    private String piso;

    @Column(name = "departamento", length = 10)
    private String departamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "localidad_id", nullable = false)
    private Localidad localidad;
}
