package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.MarcaNeumatico;

public interface MarcaNeumaticoRepository extends JpaRepository<MarcaNeumatico, Long> {

    List<MarcaNeumatico> findAllByOrderByNombreAsc();

}
