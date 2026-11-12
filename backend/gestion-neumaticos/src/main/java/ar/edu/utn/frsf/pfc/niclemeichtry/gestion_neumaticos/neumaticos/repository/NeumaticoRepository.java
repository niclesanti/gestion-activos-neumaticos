package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

public interface NeumaticoRepository extends JpaRepository<Neumatico, Long>, JpaSpecificationExecutor<Neumatico> {

    boolean existsByNumeroSerie(String numeroSerie);

    /** Con la marca en la misma consulta: sin N+1 al mostrar el lote. */
    @EntityGraph(attributePaths = "marca")
    List<Neumatico> findAllByIdIn(Collection<Long> ids);

}
