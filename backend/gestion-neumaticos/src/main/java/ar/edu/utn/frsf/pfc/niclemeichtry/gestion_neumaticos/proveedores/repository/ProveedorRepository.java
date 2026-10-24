package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;

public interface ProveedorRepository extends JpaRepository<ProveedorNeumaticos, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<ProveedorNeumaticos> findByPublicId(UUID publicId);

    List<ProveedorNeumaticos> findAllByOrderByNombreAsc();

}
