package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;

public interface CentroAlmacenamientoRepository extends JpaRepository<CentroAlmacenamiento, Long>, JpaSpecificationExecutor<CentroAlmacenamiento> {

}
