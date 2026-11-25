package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;

public interface SlotUnidadTransporteRepository extends JpaRepository<SlotUnidadTransporte, Long> {

    /** Slot con su unidad en la misma consulta: la ubicación del neumático los necesita a ambos. */
    @EntityGraph(attributePaths = "unidad")
    Optional<SlotUnidadTransporte> findConUnidadById(Long id);

}
