package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Provincia;

public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    List<Provincia> findAllByOrderByNombreAsc();

}
