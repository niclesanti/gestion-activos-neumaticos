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

/**
 * Posición de neumático de una unidad. Un slot de eje se identifica por
 * (nroEje, lado, posicionRueda); uno de auxilio, solo por nroAuxilio (la base
 * exige esa consistencia con un CHECK).
 */
@Entity
@Table(name = "slots_unidad_transporte", schema = "unidades_transporte")
@Getter
@Setter
@ToString(exclude = "unidad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotUnidadTransporte {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slots_unidad_transporte_id_gen")
    @SequenceGenerator(name = "slots_unidad_transporte_id_gen", schema = "unidades_transporte", sequenceName = "slots_unidad_transporte_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false, updatable = false)
    private UnidadTransporte unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_slot", nullable = false, length = 10)
    private TipoSlot tipoSlot;

    @Column(name = "nro_eje")
    private Integer nroEje;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_eje", length = 20)
    private TipoEje tipoEje;

    @Enumerated(EnumType.STRING)
    @Column(name = "lado", length = 10)
    private Lado lado;

    @Enumerated(EnumType.STRING)
    @Column(name = "posicion_rueda", length = 10)
    private PosicionRueda posicionRueda;

    @Column(name = "nro_auxilio")
    private Integer nroAuxilio;

    // Referencia por id al módulo neumaticos (sin asociación JPA ni FK).
    @Column(name = "neumatico_actual_id")
    private Long neumaticoActualId;

    /**
     * Código legible derivado de la posición (no se persiste): "E1-IZQ",
     * "E2-IZQ-EXT", "E2-DER-INT", "AUX-1".
     */
    public String getCodigo() {
        if (tipoSlot == TipoSlot.AUXILIO) {
            return "AUX-" + nroAuxilio;
        }
        String codigo = "E" + nroEje + "-" + (lado == Lado.IZQUIERDO ? "IZQ" : "DER");
        return switch (posicionRueda) {
            case SIMPLE -> codigo;
            case EXTERIOR -> codigo + "-EXT";
            case INTERIOR -> codigo + "-INT";
        };
    }

    public boolean estaOcupado() {
        return neumaticoActualId != null;
    }
}
