package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Long>, JpaSpecificationExecutor<UnidadTransporte> {

    boolean existsByPatente(String patente);

}
