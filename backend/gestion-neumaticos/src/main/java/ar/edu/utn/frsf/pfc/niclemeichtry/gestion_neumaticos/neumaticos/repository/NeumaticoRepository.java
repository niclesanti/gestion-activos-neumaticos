package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

public interface NeumaticoRepository extends JpaRepository<Neumatico, Long> {

    boolean existsByNumeroSerie(String numeroSerie);

}
