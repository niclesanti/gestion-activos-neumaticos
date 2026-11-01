package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;

public interface UsuarioService {

    /**
     * Fichas de auditoría (alta, última modificación y baja) de los usuarios,
     * paginadas y filtradas. Solo para administradores; el orden se limita a los
     * campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException
     *         si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}
