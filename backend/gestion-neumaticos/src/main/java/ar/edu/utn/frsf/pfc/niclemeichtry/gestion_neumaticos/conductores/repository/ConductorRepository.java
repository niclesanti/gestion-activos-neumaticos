package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    boolean existsByDni(String dni);

}
