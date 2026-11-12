package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Long>, JpaSpecificationExecutor<UnidadTransporte> {

    boolean existsByPatente(String patente);

    /**
     * Unidad activa con configuración y slots en la misma consulta. Los ejes de la configuración quedan
     * fuera: dos colecciones List en un mismo fetch no se pueden cargar juntas.
     */
    @EntityGraph(attributePaths = { "configuracion", "slots" })
    Optional<UnidadTransporte> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    /** Carga la configuración (to-one) en la misma consulta de la página: sin N+1. */
    @Override
    @EntityGraph(attributePaths = "configuracion")
    Page<UnidadTransporte> findAll(Specification<UnidadTransporte> spec, Pageable pageable);

}
