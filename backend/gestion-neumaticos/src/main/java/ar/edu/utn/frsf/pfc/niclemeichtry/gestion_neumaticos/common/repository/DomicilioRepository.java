package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;

public interface DomicilioRepository extends JpaRepository<Domicilio, Long> {

    /** El domicilio con su localidad y provincia, en una sola consulta. */
    @EntityGraph(attributePaths = { "localidad", "localidad.provincia" })
    Optional<Domicilio> findWithLocalidadById(Long id);

}
