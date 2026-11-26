package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;

public interface ConductorRepository extends JpaRepository<Conductor, Long>, JpaSpecificationExecutor<Conductor> {

    boolean existsByDni(String dni);

    Optional<Conductor> findByPublicIdAndDeletedAtIsNull(UUID publicId);

}
