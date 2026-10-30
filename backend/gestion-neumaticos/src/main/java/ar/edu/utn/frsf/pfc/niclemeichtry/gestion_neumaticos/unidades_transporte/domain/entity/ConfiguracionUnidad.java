package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuración de ejes de una unidad (patrón Type Object): describe en orden
 * los ejes y la cantidad de auxilios. Es un catálogo que solo cargan las
 * migraciones; la app no lo modifica.
 */
@Entity
@Table(name = "configuraciones_unidad", schema = "unidades_transporte")
@Getter
@Setter
@ToString(exclude = "ejes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionUnidad {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "configuraciones_unidad_id_gen")
    @SequenceGenerator(name = "configuraciones_unidad_id_gen", schema = "unidades_transporte", sequenceName = "configuraciones_unidad_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_unidad", nullable = false, length = 20)
    private TipoUnidad tipoUnidad;

    @Column(name = "cant_slots_auxilio", nullable = false)
    private Integer cantSlotsAuxilio;

    @OneToMany(mappedBy = "configuracion")
    @OrderBy("orden ASC")
    @Builder.Default
    private List<EjeConfiguracion> ejes = new ArrayList<>();

    public int obtenerTotalEjes() {
        return ejes.size();
    }

    /** Slots de eje (según el rodado de cada eje) más los de auxilio. */
    public int obtenerTotalSlots() {
        return ejes.stream().mapToInt(EjeConfiguracion::cantidadNeumaticos).sum() + cantSlotsAuxilio;
    }
}
