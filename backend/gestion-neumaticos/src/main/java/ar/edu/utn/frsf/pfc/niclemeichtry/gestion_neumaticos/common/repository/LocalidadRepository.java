package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Localidad;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    /**
     * Busca la localidad por provincia y nombre (sin distinguir mayúsculas) o la
     * crea, en una sola sentencia: dos altas concurrentes de la misma localidad
     * nueva no chocan contra el índice único. Si ya existía sin código postal,
     * se completa con el indicado; uno ya cargado no se pisa.
     *
     * @return el id de la localidad.
     */
    @Query(nativeQuery = true, value = """
            INSERT INTO common.localidades (nombre, codigo_postal, provincia_id)
            VALUES (:nombre, CAST(:codigoPostal AS VARCHAR), :provinciaId)
            ON CONFLICT (provincia_id, lower(nombre))
            DO UPDATE SET codigo_postal = COALESCE(localidades.codigo_postal, EXCLUDED.codigo_postal)
            RETURNING id""")
    Long buscarOCrear(@Param("provinciaId") Long provinciaId, @Param("nombre") String nombre,
            @Param("codigoPostal") String codigoPostal);

}
