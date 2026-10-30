package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Eje de una configuración, en su posición de adelante hacia atrás. */
@Entity
@Table(name = "ejes_configuracion", schema = "unidades_transporte")
@Getter
@Setter
@ToString(exclude = "configuracion")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjeConfiguracion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ejes_configuracion_id_gen")
    @SequenceGenerator(name = "ejes_configuracion_id_gen", schema = "unidades_transporte", sequenceName = "ejes_configuracion_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuracion_id", nullable = false)
    private ConfiguracionUnidad configuracion;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_eje", nullable = false, length = 20)
    private TipoEje tipoEje;

    @Enumerated(EnumType.STRING)
    @Column(name = "rodado", nullable = false, length = 10)
    private TipoRodado rodado;

    /** Rodado simple: una rueda por lado; dual: dos por lado. */
    public int cantidadNeumaticos() {
        return rodado == TipoRodado.DUAL ? 4 : 2;
    }
}
