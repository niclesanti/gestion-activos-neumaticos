package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import jakarta.persistence.LockModeType;

public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Long>, JpaSpecificationExecutor<UnidadTransporte> {

    boolean existsByPatente(String patente);

    /**
     * Unidad activa con configuración y slots en la misma consulta. Los ejes de la configuración quedan
     * fuera: dos colecciones List en un mismo fetch no se pueden cargar juntas.
     */
    @EntityGraph(attributePaths = { "configuracion", "slots" })
    Optional<UnidadTransporte> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    /**
     * Unidad activa con bloqueo de escritura sobre su fila: serializa los desmontajes concurrentes. Sin
     * entity graph, porque PostgreSQL no admite FOR UPDATE sobre el lado nullable de un outer join; los
     * slots se cargan luego, dentro de la misma transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UnidadTransporte u where u.publicId = :publicId and u.deletedAt is null")
    Optional<UnidadTransporte> findBloqueadaByPublicIdAndDeletedAtIsNull(@Param("publicId") UUID publicId);

    /** Carga la configuración (to-one) en la misma consulta de la página: sin N+1. */
    @Override
    @EntityGraph(attributePaths = "configuracion")
    Page<UnidadTransporte> findAll(Specification<UnidadTransporte> spec, Pageable pageable);

}
