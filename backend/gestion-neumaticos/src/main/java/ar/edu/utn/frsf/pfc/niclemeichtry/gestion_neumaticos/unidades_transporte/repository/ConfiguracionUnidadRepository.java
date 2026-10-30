package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;

public interface ConfiguracionUnidadRepository extends JpaRepository<ConfiguracionUnidad, Long> {

    /** Con los ejes en la misma consulta: la factory y la vista previa los recorren todos. */
    @EntityGraph(attributePaths = "ejes")
    Optional<ConfiguracionUnidad> findConEjesById(Long id);

    @EntityGraph(attributePaths = "ejes")
    List<ConfiguracionUnidad> findAllByOrderByIdAsc();

}
