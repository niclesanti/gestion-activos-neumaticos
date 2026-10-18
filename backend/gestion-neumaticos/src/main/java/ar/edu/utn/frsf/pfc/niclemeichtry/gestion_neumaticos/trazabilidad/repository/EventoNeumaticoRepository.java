package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoNeumatico;

public interface EventoNeumaticoRepository extends JpaRepository<EventoNeumatico, Long> {

}
