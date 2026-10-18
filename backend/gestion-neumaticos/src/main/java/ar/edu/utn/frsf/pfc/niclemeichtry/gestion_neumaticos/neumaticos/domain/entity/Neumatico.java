package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "neumaticos", schema = "neumaticos")
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Neumatico {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "neumaticos_id_gen")
    @SequenceGenerator(name = "neumaticos_id_gen", schema = "neumaticos", sequenceName = "neumaticos_id_seq", allocationSize = 50)
    private Long id;

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    // No se puede modificar una vez registrado (HU-11); la base tampoco da UPDATE sobre la columna.
    @Column(name = "numero_serie", unique = true, length = 100, updatable = false)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 30)
    private EstadoNeumatico estadoActual;

    @Column(name = "en_espera", nullable = false)
    private Boolean enEspera;

    @Column(name = "kilometraje_total")
    private Float kilometrajeTotal;

    @Column(name = "kilometraje_ultima_reparacion")
    private Float kilometrajeUltimaReparacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_origen", nullable = false, length = 20)
    private TipoEjeUnidadTransporte tipoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actual", nullable = false, length = 20)
    private TipoEjeUnidadTransporte tipoActual;

    @Column(name = "cantidad_recapados")
    private Integer cantidadRecapados;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacion_operativa_vigente", nullable = false, length = 20)
    private SituacionOperativa situacionOperativaVigente;

    @Column(name = "comentario", length = 512)
    private String comentario;

    @Column(name = "slot_montado_id")
    private Long slotMontadoId;

    @Column(name = "centro_almacenamiento_id")
    private Long centroAlmacenamientoId;

    @Column(name = "servicio_reparacion_id")
    private Long servicioReparacionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private MarcaNeumatico marca;

    public void setCalculoKm(Float kmAntes, Float kmDespues) {
        setKilometraje(kmDespues - kmAntes);
    }

    // Un dato desconocido (null) sigue desconocido: no se inventa un punto de partida.
    public void setKilometraje(Float kilometraje) {
        this.kilometrajeTotal = sumar(this.kilometrajeTotal, kilometraje);
        this.kilometrajeUltimaReparacion = sumar(this.kilometrajeUltimaReparacion, kilometraje);
    }

    public void agregarNeumaticoStock(Long centroAlmacenamientoId) {
        this.centroAlmacenamientoId = centroAlmacenamientoId;
    }

    public void removerNeumaticoStock() {
        this.centroAlmacenamientoId = null;
    }

    public void enviarNeumaticoReparacion(Long servicioReparacionId) {
        this.servicioReparacionId = servicioReparacionId;
    }

    public void reingresarNeumatico() {
        this.servicioReparacionId = null;
    }

    public void nuevoRecapado() {
        if (this.cantidadRecapados != null) {
            this.cantidadRecapados += 1;
        }
    }

    public void montarEnSlot(Long slotMontadoId) {
        this.slotMontadoId = slotMontadoId;
    }

    private static Float sumar(Float actual, Float incremento) {
        return actual == null ? null : actual + incremento;
    }
}
