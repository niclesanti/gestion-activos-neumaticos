package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

public interface NeumaticoRepository extends JpaRepository<Neumatico, Long>, JpaSpecificationExecutor<Neumatico> {

    boolean existsByNumeroSerie(String numeroSerie);

    /** Con la marca en la misma consulta: sin N+1 al mostrar el lote. */
    @EntityGraph(attributePaths = "marca")
    List<Neumatico> findAllByIdIn(Collection<Long> ids);

    /** Neumático por su identificador público, aun con baja lógica (el montaje lo rechaza con 409). */
    Optional<Neumatico> findByPublicId(UUID publicId);

    /** Neumático sin baja lógica con su marca en la misma consulta. */
    @EntityGraph(attributePaths = "marca")
    Optional<Neumatico> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    /** Carga la marca (to-one) en la misma consulta de la página: sin N+1. */
    @Override
    @EntityGraph(attributePaths = "marca")
    Page<Neumatico> findAll(Specification<Neumatico> spec, Pageable pageable);

}
