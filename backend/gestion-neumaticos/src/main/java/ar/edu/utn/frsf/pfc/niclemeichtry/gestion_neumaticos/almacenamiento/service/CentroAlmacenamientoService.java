package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;

public interface CentroAlmacenamientoService {

    /** Da de alta un centro de almacenamiento sin stock; {@code usuario} es el publicId de quien lo registra. */
    CentroAlmacenamientoResponseDTO registrarCentroAlmacenamiento(CentroAlmacenamientoRequestDTO request,
            String usuario);

    /** Centros activos con su stock, ordenados por nombre (son pocos: sin paginar). */
    List<CentroAlmacenamientoListadoDTO> listarCentros();

    /**
     * Fichas de auditoría (alta, última modificación y baja) de centros de almacenamiento, paginadas y
     * filtradas. Solo para administradores; el orden se limita a los campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}
