package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/*
 * Sin @Data a propósito: su toString expondría el hash de la clave en logs, y
 * equals/hashCode sobre todos los campos (incluido el id generado) rompe la
 * identidad de una entidad JPA dentro de colecciones.
 */
@Entity
@Table(name = "usuarios", schema = "usuarios")
@Getter
@Setter
@ToString(exclude = "clave")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuarios_id_gen")
    @SequenceGenerator(name = "usuarios_id_gen", schema = "usuarios", sequenceName = "usuarios_id_seq", allocationSize = 50)
    private Long id;

    /**
     * Identificador expuesto en la API y en el JWT. UUIDv7: ordenado por tiempo,
     * con mejor localidad en el índice B-tree que un UUIDv4 aleatorio.
     */
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 30)
    private String nombreUsuario;

    @Column(name = "nombre_apellido", nullable = false, length = 100)
    private String nombreApellido;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    /** Hash Argon2id con prefijo de algoritmo ({@code {argon2}...}). Nunca texto plano. */
    @Column(name = "clave", nullable = false, length = 255)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acceso", nullable = false, length = 20)
    private NivelAcceso nivelAcceso;

    /** El login busca en minúsculas: se persiste normalizado para que coincida. */
    @PrePersist
    @PreUpdate
    void normalizar() {
        if (nombreUsuario != null) {
            nombreUsuario = nombreUsuario.strip().toLowerCase(Locale.ROOT);
        }
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}
