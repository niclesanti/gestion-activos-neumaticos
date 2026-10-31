package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;

public interface ServicioReparacionRepository extends JpaRepository<ServicioReparacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

}
