package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import jakarta.persistence.LockModeType;

public interface CentroAlmacenamientoRepository extends JpaRepository<CentroAlmacenamiento, Long>, JpaSpecificationExecutor<CentroAlmacenamiento> {

    Optional<CentroAlmacenamiento> findByPublicId(UUID publicId);

    List<CentroAlmacenamiento> findAllByIdIn(Collection<Long> ids);

    List<CentroAlmacenamiento> findAllByDeletedAtIsNullOrderByNombreAsc();

    /** Bloquea la fila hasta el fin de la transacción: dos montajes concurrentes no pierden un descuento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CentroAlmacenamiento c WHERE c.id = :id")
    Optional<CentroAlmacenamiento> findParaActualizarById(Long id);

}
