package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Agregado raíz del módulo: la unidad y sus slots, que se crean con ella
 * (ver {@code UnidadTransporteFactory}) y se guardan en cascada.
 */
@Entity
@Table(name = "unidades_transporte", schema = "unidades_transporte")
@Getter
@Setter
@ToString(exclude = { "configuracion", "slots" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadTransporte {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unidades_transporte_id_gen")
    @SequenceGenerator(name = "unidades_transporte_id_gen", schema = "unidades_transporte", sequenceName = "unidades_transporte_id_seq", allocationSize = 50)
    private Long id;

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    // Normalizada (ver Patentes); la base no da UPDATE sobre la tabla.
    @Column(name = "patente", nullable = false, unique = true, length = 10, updatable = false)
    private String patente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuracion_id", nullable = false, updatable = false)
    private ConfiguracionUnidad configuracion;

    // El slot es el dueño de la relación: así unidad_id va en su INSERT (la base
    // no permite el UPDATE que haría un @OneToMany unidireccional).
    @OneToMany(mappedBy = "unidad", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<SlotUnidadTransporte> slots = new ArrayList<>();

    /** Agrega el slot y le asigna esta unidad. */
    public void addSlot(SlotUnidadTransporte slot) {
        slot.setUnidad(this);
        slots.add(slot);
    }

    public List<SlotUnidadTransporte> getSlotsDeEje(int nroEje) {
        return slots.stream()
                .filter(slot -> slot.getTipoSlot() == TipoSlot.EJE && slot.getNroEje() == nroEje)
                .toList();
    }

    public List<SlotUnidadTransporte> getSlotsAuxilio() {
        return slots.stream()
                .filter(slot -> slot.getTipoSlot() == TipoSlot.AUXILIO)
                .toList();
    }
}
