package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Superclase de las entidades auditadas (HU-43). Spring Data JPA Auditing completa
 * sola la creación y la última modificación a partir del usuario autenticado
 * ({@code AuditorAware}) y del reloj de {@link AuditoriaJpaConfig}; nadie las asigna
 * a mano, por eso no hay setters ni entran en los {@code @Builder}.
 *
 * <p>Los usuarios se guardan por su {@code publicId} (el {@code sub} del JWT, el
 * mismo valor que usa {@code seguridad.usuario_actual()} en la base de datos); un
 * {@code NULL} significa "sin usuario" (carga previa a la auditoría o proceso del
 * sistema). La baja es lógica: nunca se borra la fila.
 *
 * <p>Si un servicio modifica solo un hijo del agregado (por ejemplo, el domicilio),
 * tiene que tocar la raíz para que su {@code updated_*} cambie.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * Baja lógica: registra quién y cuándo. La fila queda en la base de datos.
     *
     * @throws OperacionNoPermitidaException si el registro ya estaba dado de baja
     */
    public void darDeBaja(UUID usuario, Instant fecha) {
        if (isDadoDeBaja()) {
            throw new OperacionNoPermitidaException("El registro ya fue dado de baja");
        }
        this.deletedAt = fecha;
        this.deletedBy = usuario;
    }

    public boolean isDadoDeBaja() {
        return deletedAt != null;
    }
}
