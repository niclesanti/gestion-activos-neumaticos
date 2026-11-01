package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

public interface NeumaticoRepository extends JpaRepository<Neumatico, Long>, JpaSpecificationExecutor<Neumatico> {

    boolean existsByNumeroSerie(String numeroSerie);

}
