package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;

public interface ConductorService {

    /**
     * Registra un conductor. Nombre, apellido y DNI se guardan sin espacios en
     * los extremos; un DNI vacío equivale a no tenerlo.
     *
     * @param conductorRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario             identificador público del usuario que hace el alta.
     * @return el conductor registrado.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException
     *         si ya existe un conductor con el mismo DNI.
     */
    ConductorResponseDTO registrarConductor(ConductorRequestDTO conductorRequestDTO, String usuario);

    /**
     * Fichas de auditoría (alta, última modificación y baja) de conductores, paginadas y
     * filtradas. Solo para administradores; el orden se limita a los campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}
