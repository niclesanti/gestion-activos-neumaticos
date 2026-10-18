package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
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
 *
 * <p>Todos los subtipos comparten la tabla {@code eventos_neumatico}
 * (SINGLE_TABLE, ver V11): el historial completo de un neumático se lee sin
 * joins. La columna {@code tipo} es el discriminador y la escribe Hibernate a
 * partir del {@code @DiscriminatorValue} de cada subtipo.
 */
@Entity
@Table(name = "eventos_neumatico", schema = "trazabilidad")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 30)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class EventoNeumatico {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eventos_neumatico_id_gen")
    @SequenceGenerator(name = "eventos_neumatico_id_gen", schema = "trazabilidad", sequenceName = "eventos_neumatico_id_seq", allocationSize = 50)
    private Long id;

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

    /** El tipo de evento; coincide con el valor del discriminador del subtipo. */
    public abstract TipoEventoNeumatico getTipo();
}
