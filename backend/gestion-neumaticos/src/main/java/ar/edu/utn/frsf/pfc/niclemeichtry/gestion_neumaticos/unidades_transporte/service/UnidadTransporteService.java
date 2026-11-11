package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;

public interface UnidadTransporteService {

    /**
     * Registra una unidad de transporte y genera todos los slots de su
     * configuración, sin neumático asignado. La patente se guarda normalizada
     * (mayúsculas, sin espacios ni guiones).
     *
     * @param unidadTransporteRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario                    identificador público del usuario que hace el alta.
     * @return la unidad registrada, con sus slots.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException
     *         si ya existe una unidad con la misma patente.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la configuración no existe.
     */
    UnidadTransporteResponseDTO registrarUnidadTransporte(UnidadTransporteRequestDTO unidadTransporteRequestDTO,
            String usuario);

    /**
     * Busca las unidades activas cuya patente contiene el término (sin distinguir mayúsculas, espacios ni
     * guiones). Sin término devuelve todas las activas. Paginada; solo se ordena por patente.
     *
     * @param patente  término de búsqueda, opcional.
     * @param pageable página pedida.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<UnidadTransporteListadoDTO> buscar(String patente, Pageable pageable);

    /** Las configuraciones precargadas, con sus ejes en orden. */
    List<ConfiguracionUnidadDTO> listarConfiguraciones();

    /**
     * Fichas de auditoría (alta, última modificación y baja) de unidades de transporte, paginadas y
     * filtradas. Solo para administradores; el orden se limita a los campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}
