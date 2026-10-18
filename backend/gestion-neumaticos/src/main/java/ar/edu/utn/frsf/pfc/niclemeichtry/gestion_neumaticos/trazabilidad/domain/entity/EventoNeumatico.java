package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Hecho del historial de un neumático. Trazabilidad es un observador: solo
 * persiste eventos ya consumados en otros módulos, con sus referencias como
 * id + snapshot (los datos de exhibición "congelados" al momento del hecho).
 * Cada subtipo vive en su propia tabla (JOINED).
 */
@Entity
@Table(name = "eventos_neumatico", schema = "trazabilidad")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class EventoNeumatico {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eventos_neumatico_id_gen")
    @SequenceGenerator(name = "eventos_neumatico_id_gen", schema = "trazabilidad", sequenceName = "eventos_neumatico_id_seq", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoEventoNeumatico tipo;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "comentario", length = 512)
    private String comentario;

    @Column(name = "neumatico_id", nullable = false)
    private Long neumaticoId;

    @Column(name = "numero_serie_neumatico", length = 100)
    private String numeroSerieNeumatico;
}
